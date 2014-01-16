package uk.gov.hmrc.common.microservice.deskpro

import uk.gov.hmrc.common.microservice.{MicroServiceConfig, Connector}
import controllers.common.actions.HeaderCarrier
import scala.concurrent.Future
import uk.gov.hmrc.common.microservice.domain.User
import play.api.mvc.Request
import uk.gov.hmrc.common.microservice.deskpro.domain.{TicketId, Ticket, Feedback}

class HmrcDeskproConnector extends Connector {

  override val serviceUrl = MicroServiceConfig.hmrcDeskproServiceUrl

  def createTicket(name: String, email: String, subject: String, message: String, referrer: String, isJavascript: Boolean, request: Request[AnyRef], user: Option[User])(implicit hc: HeaderCarrier): Future[Option[TicketId]] =
    httpPostF[TicketId, Ticket]("/deskpro/ticket", Some(Ticket(name, email, subject, message, referrer, isJavascript, hc, request, user)))

  def createFeedback(name: String, email: String, rating: String, subject: String, message: String, referrer: String, isJavascript: Boolean, request: Request[AnyRef], user: Option[User])(implicit hc: HeaderCarrier): Future[Option[TicketId]] =
    httpPostF[TicketId, Feedback]("/deskpro/feedback", Some(Feedback(name, email, rating, subject, message, referrer, isJavascript, hc, request, user)))

}