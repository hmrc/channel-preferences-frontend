package microservice.ggw

import microservice.{ MicroService, MicroServiceConfig }
import play.api.libs.json._
import scala.collection.Seq

//todo make GGW a Play submodule
//todo ? encode password - if not then https...
class GgwMicroService extends MicroService {

  override val serviceUrl = MicroServiceConfig.ggwServiceUrl

  implicit object CredentialsWrites extends Writes[Credentials] {
    def writes(c: Credentials): JsValue = JsObject(Seq("username" -> JsString(c.userId), "password" -> JsString(c.password)))
  }

  def login(credentials: Credentials) = {
    httpPost[GovernmentGatewayResponse]("/government-gateway/login", Json.toJson(credentials)).getOrElse(throw new IllegalStateException(s"Expected a ${GovernmentGatewayResponse.getClass.getName} but none returned"))
  }

}

case class Credentials(userId: String, password: String)
case class GovernmentGatewayResponse(authId: String, name: String)

