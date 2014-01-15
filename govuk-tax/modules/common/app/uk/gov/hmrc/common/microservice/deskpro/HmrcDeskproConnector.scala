package uk.gov.hmrc.common.microservice.deskpro

import uk.gov.hmrc.common.microservice.{MicroServiceConfig, Connector}
import controllers.common.actions.HeaderCarrier
import scala.concurrent.Future
import uk.gov.hmrc.common.microservice.domain.User
import play.api.mvc.Request

class HmrcDeskproConnector extends Connector {

  override val serviceUrl = MicroServiceConfig.hmrcDeskproServiceUrl

  def createTicket(name: String, email: String, subject: String, message: String, referrer: String, isJavascript: Boolean, request: Request[AnyRef], user: Option[User])(implicit hc: HeaderCarrier): Future[Option[TicketId]] =
    httpPostF[TicketId, Ticket]("/deskpro/ticket", Some(Ticket(name, email, subject, message, referrer, isJavascript, hc, request, user)))
}

case class Ticket private(name: String,
                  email: String,
                  subject: String,
                  message: String,
                  referrer: String,
                  javascriptEnabled: String,
                  userAgent: String,
                  authId: String,
                  areaOfTax: String,
                  sessionId: String)

object Ticket {
  def apply(name: String, email: String, subject: String, message: String, referrer: String, isJavascript: Boolean, hc: HeaderCarrier, request: Request[AnyRef], userOption: Option[User]): Ticket =
    Ticket(
      name.trim,
      email,
      subject,
      message.trim,
      referrer,
      if (isJavascript) "Y" else "N",
      request.headers.get("User-Agent").getOrElse("n/a"),
      hc.userId.getOrElse("n/a"),
      userOption.map(user => user.regimes.paye.map(_ => "paye").getOrElse("biztax")).getOrElse("n/a"),
      hc.sessionId.getOrElse("n/a"))

}


case class TicketId(ticket_id: Int)
