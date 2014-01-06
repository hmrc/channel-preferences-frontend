package uk.gov.hmrc.common.microservice.auth

import uk.gov.hmrc.common.microservice.{ Connector, MicroServiceConfig }
import uk.gov.hmrc.common.microservice.auth.domain.Authority
import controllers.common.actions.HeaderCarrier
import scala.concurrent.Future

class AuthConnector(override val serviceUrl: String = MicroServiceConfig.authServiceUrl) extends Connector {

  def authority(path: String)(implicit hc: HeaderCarrier) = httpGetF[Authority](path)

  def authorityByPidAndUpdateLoginTime(pid: String)(implicit hc: HeaderCarrier) : Future[Option[Authority]] = httpPostF(s"/auth/pid/$pid", null)
}
