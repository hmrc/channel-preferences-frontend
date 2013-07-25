package microservice.ggw

import microservice.{ MicroService, MicroServiceConfig }
import play.api.libs.json._
import scala.collection.Seq
import microservice.auth.domain.UserAuthority

//todo make GGW a Play submodule
//todo ? encode password - if not then https...
class GgwMicroService extends MicroService {

  override val serviceUrl = MicroServiceConfig.ggwServiceUrl

  implicit object CredentialsWrites extends Writes[Credentials] {
    def writes(c: Credentials): JsValue = JsObject(Seq("userId" -> JsString(c.userId), "password" -> JsString(c.password)))
  }

  implicit object ValidateTokenRequestWrites extends Writes[ValidateTokenRequest] {
    def writes(g: ValidateTokenRequest): JsValue = JsObject(Seq("token" -> JsString(g.token), "timestamp" -> JsString(g.timestamp)))
  }

  def login(credentials: Credentials) = {
    httpPost[GovernmentGatewayResponse]("/government-gateway/login", Json.toJson(credentials), Map.empty).getOrElse(throw new IllegalStateException("Expected UserAuthority response but none returned"))
  }

  def validateToken(validateTokenRequest: ValidateTokenRequest) = {
    httpPost[GovernmentGatewayResponse]("/government-gateway/validateToken", Json.toJson(validateTokenRequest), Map.empty).getOrElse(throw new IllegalStateException("Expected UserAuthority response but none returned"))
  }

}

case class Credentials(userId: String, password: String)
case class GovernmentGatewayResponse(authId: String, name: String)
case class ValidateTokenRequest(token: String, timestamp: String)

