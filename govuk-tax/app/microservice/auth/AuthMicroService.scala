package microservice.auth

import microservice.{ MicroServiceConfig, MicroService }
import scala.concurrent.Await
import microservice.auth.domain.MatsUserAuthority

class AuthMicroService(override val serviceUrl: String = MicroServiceConfig.authServiceUrl) extends MicroService {

  def authority(uri: String) = Await.result(response[MatsUserAuthority](httpResource(uri).get()), defaultTimeoutDuration)
}