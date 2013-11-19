package uk.gov.hmrc.common.microservice.ct

import uk.gov.hmrc.microservice.{Connector, MicroServiceConfig}
import uk.gov.hmrc.common.microservice.ct.domain.{CtAccountSummary, CtJsonRoot}
import uk.gov.hmrc.domain.CalendarEvent
import scala.concurrent.Future

class CtConnector extends Connector {

  val serviceUrl = MicroServiceConfig.ctServiceUrl

  def root(uri: String) = httpGet[CtJsonRoot](uri).getOrElse(CtJsonRoot(Map.empty))

  def accountSummary(uri: String) = httpGet[CtAccountSummary](uri)

  def calendar(uri: String) :Future[Option[List[CalendarEvent]]] = httpGetF[List[CalendarEvent]](uri)

}



