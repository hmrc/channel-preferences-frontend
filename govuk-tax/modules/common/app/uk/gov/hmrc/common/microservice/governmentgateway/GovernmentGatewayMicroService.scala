package uk.gov.hmrc.microservice.governmentgateway

import play.api.libs.json._
import scala.collection.Seq
import uk.gov.hmrc.microservice.{ MicroService, MicroServiceConfig }

class GovernmentGatewayMicroService extends MicroService {

  override val serviceUrl = MicroServiceConfig.governmentGatewayServiceUrl

  implicit object CredentialsWrites extends Writes[Credentials] {
    def writes(c: Credentials) = JsObject(Seq("userId" -> JsString(c.userId), "password" -> JsString(c.password)))
  }

  implicit object ValidateTokenRequestWrites extends Writes[SsoLoginRequest] {
    def writes(g: SsoLoginRequest) = JsObject(Seq("token" -> JsString(g.token), "timestamp" -> JsString(g.timestamp.toString)))
  }

  def login(credentials: Credentials) = {
    val loginResponse = httpPost[GovernmentGatewayResponse](
      uri = "/government-gateway/login",
      body = Json.toJson(credentials),
      headers = Map.empty)

    loginResponse.getOrElse(throw new IllegalStateException("Expected UserAuthority response but none returned"))
  }

  def ssoLogin(ssoLoginRequest: SsoLoginRequest) = {
    val ssoResponse = httpPost[GovernmentGatewayResponse](
      uri = "/government-gateway/sso-login",
      body = Json.toJson(ssoLoginRequest),
      headers = Map.empty)

    ssoResponse.getOrElse(throw new IllegalStateException("Expected UserAuthority response but none returned"))
  }
}

case class Credentials(userId: String,
  password: String)

case class SsoLoginRequest(token: String,
  timestamp: Long)

case class GovernmentGatewayResponse(authId: String,
  name: String,
  encodedGovernmentGatewayToken: String)

