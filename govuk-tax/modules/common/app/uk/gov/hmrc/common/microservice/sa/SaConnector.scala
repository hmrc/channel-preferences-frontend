package uk.gov.hmrc.common.microservice.sa

import uk.gov.hmrc.common.microservice.{Connector, MicroServiceConfig}
import uk.gov.hmrc.common.microservice.sa.domain.write.SaAddressForUpdate
import uk.gov.hmrc.common.microservice.sa.domain.{SaAccountSummary, SaPerson, SaJsonRoot}
import scala.concurrent._
import controllers.common.actions.HeaderCarrier

class SaConnector extends Connector {

  override val serviceUrl = MicroServiceConfig.saServiceUrl

  def root(uri: String)(implicit hc: HeaderCarrier): Future[SaJsonRoot] = httpGetF[SaJsonRoot](uri).map(_.getOrElse(SaJsonRoot(Map.empty)))

  def person(uri: String)(implicit hc: HeaderCarrier): Future[Option[SaPerson]] = httpGetF[SaPerson](uri)

  def accountSummary(uri: String)(implicit headerCarrier: HeaderCarrier): Future[Option[SaAccountSummary]] = httpGetF[SaAccountSummary](uri)
}
