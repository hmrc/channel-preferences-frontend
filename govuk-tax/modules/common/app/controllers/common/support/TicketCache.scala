package controllers.common.support

import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import controllers.common.service.Connectors
import uk.gov.hmrc.common.microservice.deskpro.domain.TicketId
import controllers.common.actions.HeaderCarrier
import scala.concurrent.Future

class TicketCache(keyStoreConnector: KeyStoreConnector = Connectors.keyStoreConnector) {

  import scala.concurrent.ExecutionContext.Implicits.global

  val actionId: String = "confirmTicket"
  val source: String = "tickets"
  val ticketKey: String = "ticketId"

  def stashTicket(ticket: Option[TicketId], formId: String)(implicit hc: HeaderCarrier) =
    ticket.map {
      t =>
        keyStoreConnector.addKeyStoreEntry[Map[String, String]](actionId, source, formId, Map(ticketKey -> t.ticket_id.toString)).map(_ => "stored")
    }.getOrElse(Future.successful("ignored"))

  def popTicket(formId: String)(implicit hc: HeaderCarrier) =
    keyStoreConnector.getEntry[Map[String, String]](actionId, source, formId).map {
      keyStoreData =>
        keyStoreData.flatMap(_.get(ticketKey)).getOrElse("Unknown")
    }
}

object TicketCache {
  def apply() = new TicketCache()
}
