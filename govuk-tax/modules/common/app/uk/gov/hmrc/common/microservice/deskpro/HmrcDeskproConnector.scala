package uk.gov.hmrc.common.microservice.deskpro

import uk.gov.hmrc.common.microservice.{MicroServiceConfig, Connector}
import controllers.common.actions.HeaderCarrier
import scala.concurrent.Future

class HmrcDeskproConnector extends Connector {

  override val serviceUrl = MicroServiceConfig.hmrcDeskproServiceUrl

  def createTicket(ticket: Ticket)(implicit hc: HeaderCarrier): Future[Option[TicketId]] = {
    httpPostF[TicketId, Ticket]("/deskpro/ticket", Some(ticket))
  }

}

case class Ticket(name: String,
                      email: String,
                      subject: String,
                      message: String,
                      referrer: String,
                      javascriptEnabled: String,
                      userAgent: String,
                      authId: String,
                      areaOfTax: String,
                      sessionId: String)


case class TicketId(ticket_id: Int)
