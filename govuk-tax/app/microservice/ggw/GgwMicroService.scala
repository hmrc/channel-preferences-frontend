package microservice.ggw

import microservice.{ MicroService, MicroServiceConfig }
import play.api.libs.json._
import microservice.auth.domain.UserAuthority
import scala.collection.Seq

//todo make GGW a Play submodule
class GgwMicroService extends MicroService {

  override val serviceUrl = MicroServiceConfig.ggwServiceUrl

  implicit object CredentialsWrites extends Writes[Credentials] {
    def writes(c: Credentials): JsValue = JsObject(Seq("username" -> JsString(c.userId), "password" -> JsString(c.password)))
  }

  def login(credentials: Credentials) = {
    httpPost[UserAuthority]("/government-gateway/login", Json.toJson(credentials), Map.empty).getOrElse(throw new IllegalStateException("Expected UserAuthority response but none returned"))
  }

}
//TODO add name to this - ? how will we render the name in the page
case class Credentials(userId: String, password: String)

