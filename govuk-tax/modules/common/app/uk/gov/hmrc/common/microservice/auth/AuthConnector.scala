package uk.gov.hmrc.common.microservice.auth

import uk.gov.hmrc.microservice.{ Connector, MicroServiceConfig }
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import play.api.libs.json.JsNull
import controllers.common.actions.HeaderCarrier

class AuthConnector(override val serviceUrl: String = MicroServiceConfig.authServiceUrl) extends Connector {

  def authority(path: String)(implicit hc: HeaderCarrier) = httpGet[UserAuthority](path)

  def authorityByPidAndUpdateLoginTime(pid: String)(implicit hc: HeaderCarrier) = httpPost[UserAuthority](s"/auth/pid/${pid}", JsNull)
}
