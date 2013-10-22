package uk.gov.hmrc.common.microservice.auth

import uk.gov.hmrc.microservice.{ MicroService, MicroServiceConfig }
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import play.api.libs.json.JsNull

class AuthMicroService(override val serviceUrl: String = MicroServiceConfig.authServiceUrl) extends MicroService {

  def authority(path: String) = httpGet[UserAuthority](path)

  def authorityByPidAndUpdateLoginTime(pid: String) = httpPost[UserAuthority](s"/auth/pid/${pid}", JsNull)
}
