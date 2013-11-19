package uk.gov.hmrc.common.microservice.vat

import uk.gov.hmrc.microservice.{Connector, MicroServiceConfig}
import uk.gov.hmrc.common.microservice.vat.domain.{VatAccountSummary, VatJsonRoot}
import uk.gov.hmrc.domain.CalendarEvent

class VatConnector extends Connector {

  val serviceUrl = MicroServiceConfig.vatServiceUrl

  def root(uri: String) = httpGet[VatJsonRoot](uri).getOrElse(VatJsonRoot(Map.empty))

  def accountSummary(uri: String) = httpGet[VatAccountSummary](uri)

  def calendar(uri: String) = httpGetF[List[CalendarEvent]](uri)
}
