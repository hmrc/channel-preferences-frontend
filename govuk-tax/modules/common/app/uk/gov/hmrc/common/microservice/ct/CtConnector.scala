package uk.gov.hmrc.common.microservice.ct

import uk.gov.hmrc.microservice.{Connector, MicroServiceConfig}
import uk.gov.hmrc.common.microservice.ct.domain.{CtAccountSummary, CtJsonRoot}
import uk.gov.hmrc.domain.CalendarEvent
import scala.concurrent.Future
import controllers.common.actions.HeaderCarrier

class CtConnector extends Connector {

  val serviceUrl = MicroServiceConfig.ctServiceUrl

  def root(uri: String) = httpGet[CtJsonRoot](uri).getOrElse(CtJsonRoot(Map.empty))

  def accountSummary(uri: String)(implicit headerCarrier:HeaderCarrier) = httpGetF[CtAccountSummary](uri)

  def calendar(uri: String)(implicit headerCarrier:HeaderCarrier) :Future[Option[List[CalendarEvent]]] = httpGetF[List[CalendarEvent]](uri)

}



