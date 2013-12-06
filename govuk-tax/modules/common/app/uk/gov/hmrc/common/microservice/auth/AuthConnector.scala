package uk.gov.hmrc.common.microservice.auth

import uk.gov.hmrc.microservice.{ Connector, MicroServiceConfig }
import uk.gov.hmrc.common.microservice.auth.domain.Authority
import play.api.libs.json.JsNull
import controllers.common.actions.HeaderCarrier

class AuthConnector(override val serviceUrl: String = MicroServiceConfig.authServiceUrl) extends Connector {

  def authority(path: String)(implicit hc: HeaderCarrier) = httpGetF[Authority](path)

  def authorityByPidAndUpdateLoginTime(pid: String)(implicit hc: HeaderCarrier) = httpPostF[Authority](s"/auth/pid/$pid", JsNull)
}
