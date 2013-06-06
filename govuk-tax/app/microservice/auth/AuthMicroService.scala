package microservice.auth

import microservice.{ MicroServiceConfig, MicroService }
import scala.concurrent.Await
import microservice.auth.domain.UserAuthority

class AuthMicroService(override val serviceUrl: String = MicroServiceConfig.authServiceUrl) extends MicroService {

  def authority(uri: String) = get[UserAuthority](uri)
}
