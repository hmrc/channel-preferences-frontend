package microservice.auth

import microservice.{ MicroServiceConfig, MicroService }
import microservice.auth.domain.UserAuthority

class AuthMicroService(override val serviceUrl: String = MicroServiceConfig.authServiceUrl) extends MicroService {
  def authorityFromOid(oid: String) = httpGet[UserAuthority](s"/auth/oid/$oid")
  def authority(path: String) = httpGet[UserAuthority](path)
}
