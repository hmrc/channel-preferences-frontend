package uk.gov.hmrc.common.microservice.idatokenapi

import scala.util.Try
import scala.concurrent.Future

import org.apache.commons.codec.binary.Base64

import uk.gov.hmrc.common.microservice.{MicroServiceConfig, Connector}
import uk.gov.hmrc.crypto.AesCrypto

import play.api.libs.ws.WS
import play.api.libs.json._
import play.api.Logger
import play.api.data.validation.ValidationError
import controllers.common.actions.LoggingDetails


object IdaTokenAuth extends AesCrypto {
  val encryptionKey = "s7fkwn2sc52fggkfslv29g"

  def main(args:Array[String]):Unit = {
    args match {
      case Array(username, password) => {
        println(s"Encrypted username: ${encrypt(username)}")
        println(s"Encrypted password: ${encrypt(password)}")
      }
      case _ => {
        println("Usage: IdaTokenAuth <username> <password>")
        println("will output the encrypted, base64 encoded strings.")
      }
    }
  }
}

class IdaTokenApiConnector extends Connector with AesCrypto {
  val encryptionKey = IdaTokenAuth.encryptionKey

  override val serviceUrl = ""

  lazy val pathBase = MicroServiceConfig.idaTokenApiPathBase
  lazy val idaTokenRequired = MicroServiceConfig.idaTokenRequired
  lazy val idaTokenApiUser = MicroServiceConfig.idaTokenApiUser.flatMap { encryptedUser => Try {decrypt(encryptedUser)}.toOption}
  lazy val idaTokenApiPass = MicroServiceConfig.idaTokenApiPass.flatMap { encryptedPass => Try {decrypt(encryptedPass)}.toOption}

  val idaApiBasicAuthHeaders = for {
    idaUser <- idaTokenApiUser
    idaPass <- idaTokenApiPass
  } yield {
    val userpass = new String(Base64.encodeBase64(s"$idaUser:$idaPass".getBytes))
    Map("Authorization" -> s"Basic $userpass")
  }

  private def verifyUrl(token: String) = {
    s"$pathBase/tokens/$token/validate"
  }

  case class ValidateResponse(valid: Boolean, reason: Option[String])

  implicit val vrReads = Json.reads[ValidateResponse]

  def validateToken(token: String)(implicit lg: LoggingDetails): Future[Boolean] = {
    val url: String = verifyUrl(token)
    val headers: Seq[(String, String)] = ("Content-Type", "application/json") +: (idaApiBasicAuthHeaders.getOrElse(Map()).toSeq)

    withLogging("Post", url) {
      WS.url(url).withHeaders(headers: _*).post("").map { body =>
        Try {
          body.json.validate[ValidateResponse] match {
            case JsSuccess(validateResponse, path) => validateResponse.valid
            case JsError(errs) => logJsValidationErrors("Errors validating json from IDA token validation service", errs); false
          }
        }.recover {
          case t: Throwable => Logger.error("Error parsing Json from IDA token validation service", t); false
        }.getOrElse(false)
      }
    }
  }


  private def logJsValidationErrors(message: String, errs: Seq[(JsPath, Seq[ValidationError])]) {
    Logger.error(message)
    for {
      pathErrs <- errs
      path = pathErrs._1
      err <- pathErrs._2
    } yield {
      Logger.error(s"$path - $err")
    }
  }
}

