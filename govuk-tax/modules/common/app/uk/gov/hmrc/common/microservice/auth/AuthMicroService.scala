package uk.gov.hmrc.microservice.auth

import uk.gov.hmrc.microservice.{ MicroService, MicroServiceConfig }
import uk.gov.hmrc.microservice.auth.domain.UserAuthority

class AuthMicroService(override val serviceUrl: String = MicroServiceConfig.authServiceUrl) extends MicroService {
  def authority(path: String) = httpGet[UserAuthority](path)
}
