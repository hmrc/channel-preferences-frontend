package microservice.ggw

import microservice.{MicroService, MicroServiceConfig}
import microservice.auth.domain.UserAuthority
import play.api.libs.json.Json

class GgwMicroService extends MicroService {

  override val serviceUrl = MicroServiceConfig.ggwServiceUrl

  def login(credentials:Credentials) = httpPost[UserAuthority]("/government-gateway/login", Json.toJson(credentials)).getOrElse(throw new IllegalStateException("Expected UserAuthority response but none returned"))


}
case class Credentials(userId:String, password:String)
