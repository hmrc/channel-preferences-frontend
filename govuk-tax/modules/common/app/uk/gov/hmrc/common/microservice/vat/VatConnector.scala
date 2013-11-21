package uk.gov.hmrc.common.microservice.vat

import uk.gov.hmrc.microservice.{Connector, MicroServiceConfig}
import uk.gov.hmrc.common.microservice.vat.domain.{VatAccountSummary, VatJsonRoot}
import uk.gov.hmrc.domain.CalendarEvent
import controllers.common.actions.HeaderCarrier

class VatConnector extends Connector {

  val serviceUrl = MicroServiceConfig.vatServiceUrl

  def root(uri: String)(implicit hc: HeaderCarrier) = httpGetHC[VatJsonRoot](uri).getOrElse(VatJsonRoot(Map.empty))

  def accountSummary(uri: String)(implicit hc:HeaderCarrier) = httpGetF[VatAccountSummary](uri)

  def calendar(uri: String)(implicit hc:HeaderCarrier) = httpGetF[List[CalendarEvent]](uri)
}
