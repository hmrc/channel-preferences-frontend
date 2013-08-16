package uk.gov.hmrc.microservice.governmentgateway

import play.api.libs.json._
import scala.collection.Seq
import uk.gov.hmrc.microservice.{ MicroService, MicroServiceConfig }

class GovernmentGatewayMicroService extends MicroService {

  override val serviceUrl = MicroServiceConfig.governmentGatewayServiceUrl

  implicit object CredentialsWrites extends Writes[Credentials] {
    def writes(c: Credentials): JsValue = JsObject(Seq("userId" -> JsString(c.userId), "password" -> JsString(c.password)))
  }

  implicit object ValidateTokenRequestWrites extends Writes[SsoLoginRequest] {
    def writes(g: SsoLoginRequest): JsValue = JsObject(Seq("token" -> JsString(g.token), "timestamp" -> JsString(g.timestamp.toString)))
  }

  def login(credentials: Credentials) = {
    httpPost[GovernmentGatewayResponse]("/government-gateway/login", Json.toJson(credentials), Map.empty).getOrElse(throw new IllegalStateException("Expected UserAuthority response but none returned"))
  }

  def ssoLogin(ssoLoginRequest: SsoLoginRequest) = {
    httpPost[GovernmentGatewayResponse]("/government-gateway/sso-login", Json.toJson(ssoLoginRequest), Map.empty).getOrElse(throw new IllegalStateException("Expected UserAuthority response but none returned"))
  }

}

case class Credentials(userId: String, password: String)
case class SsoLoginRequest(token: String, timestamp: Long)
case class GovernmentGatewayResponse(authId: String, name: String, encodedGovernmentGatewayToken: String)

