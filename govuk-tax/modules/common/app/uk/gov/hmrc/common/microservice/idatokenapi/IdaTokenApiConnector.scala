package uk.gov.hmrc.common.microservice.idatokenapi

import scala.util.Try
import scala.concurrent.Future

import org.apache.commons.codec.binary.Base64
import org.json4s.JObject

import uk.gov.hmrc.common.microservice.{MicroServiceConfig, Connector}
import uk.gov.hmrc.crypto.AesCrypto

import controllers.common.actions.HeaderCarrier

class IdaTokenApiConnector extends Connector with AesCrypto {
  val encryptionKey = "s7fkwn2sc52fggkfslv29g"

  override val serviceUrl = MicroServiceConfig.idaTokenApiUrl
  lazy val idaTokenRequired = MicroServiceConfig.idaTokenRequired
  lazy val idaTokenApiUser = MicroServiceConfig.idaTokenApiUser.flatMap { encryptedUser => Try {decrypt(encryptedUser)}.toOption}
  lazy val idaTokenApiPass = MicroServiceConfig.idaTokenApiPass.flatMap { encryptedPass => Try {decrypt(encryptedPass)}.toOption}

  val idaApiBasicAuthHeaders = for {
    idaUser <- idaTokenApiUser
    idaPass <- idaTokenApiPass
  } yield {
    val userpass = Base64.encodeBase64(s"$idaUser:$idaPass".getBytes)
    Map("Authorization" -> s"Basic $userpass")
  }

  def validateToken(token: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    httpPostF[JObject, Nothing](s"/ida/tokens/$token/validate", None, idaApiBasicAuthHeaders.getOrElse(Map())).map { json =>
      Try {
        json.map { json =>
          implicit val formats = org.json4s.DefaultFormats
          (json \ "valid").extract[Boolean]
        }.getOrElse(false)
      }.getOrElse(false)
    }
  }
}

