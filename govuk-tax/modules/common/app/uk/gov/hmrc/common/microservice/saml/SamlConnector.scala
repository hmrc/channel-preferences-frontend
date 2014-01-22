package uk.gov.hmrc.common.microservice.saml

import uk.gov.hmrc.common.microservice.{MicroServiceConfig, Connector}
import uk.gov.hmrc.microservice.saml.domain.{AuthResponseValidationResult, AuthRequestFormData}
import controllers.common.actions.HeaderCarrier
import scala.concurrent.Future
import org.json4s.JObject
import play.api.Logger
import scala.util.Try
import uk.gov.hmrc.crypto.AesCrypto
import org.apache.commons.codec.binary.Base64


class SamlConnector extends Connector with AesCrypto {

  val encryptionKey = "s7fkwn2sc52fggkfslv29g"

  override val serviceUrl = MicroServiceConfig.samlServiceUrl


  def create(implicit hc: HeaderCarrier) = httpGetF[AuthRequestFormData]("/saml/create")
    .map(_.getOrElse(throw new IllegalStateException("Expected SAML auth response but none returned")))

  def validate(authResponse: String)(implicit hc: HeaderCarrier) = {
    httpPostF[AuthResponseValidationResult, Map[String, String]](
    uri = "/saml/validate",
    body = Some(Map("samlResponse" -> authResponse))
    ).map(_.getOrElse(throw new IllegalStateException("Expected SAML validation response but none returned")))
  }

  val idaTokenRequired = MicroServiceConfig.idaTokenRequired
  val idaTokenApiUser = MicroServiceConfig.idaTokenApiUser.map(decrypt)
  val idaTokenApiPass = MicroServiceConfig.idaTokenApiPass.map(decrypt)

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
