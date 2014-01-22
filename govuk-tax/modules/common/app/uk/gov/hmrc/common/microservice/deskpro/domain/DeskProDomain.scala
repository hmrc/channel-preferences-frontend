package uk.gov.hmrc.common.microservice.deskpro.domain

import controllers.common.actions.HeaderCarrier
import uk.gov.hmrc.common.microservice.domain.User
import play.api.mvc.Request

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


object Ticket extends FieldTransformer {
  def apply(name: String, email: String, subject: String, message: String, referrer: String, isJavascript: Boolean, hc: HeaderCarrier, request: Request[AnyRef], user: Option[User]): Ticket =
    Ticket(
      name.trim,
      email,
      subject,
      message.trim,
      referrer,
      ynValueOf(isJavascript),
      userAgentOf(request),
      userIdFrom(hc),
      areaOfTaxOf(user),
      sessionIdFrom(hc))

}

case class TicketId(ticket_id: Int)

case class Feedback(name: String,
                    email: String,
                    subject: String,
                    rating: String,
                    message: String,
                    referrer: String,
                    javascriptEnabled: String,
                    userAgent: String,
                    authId: String,
                    areaOfTax: String,
                    sessionId: String)


object Feedback extends FieldTransformer {
  def apply(name: String, email: String, rating: String, subject: String, message: String, referrer: String, isJavascript: Boolean, hc: HeaderCarrier, request: Request[AnyRef], user: Option[User]): Feedback =
    Feedback(
      name.trim,
      email,
      subject,
      rating,
      message.trim,
      referrer,
      ynValueOf(isJavascript),
      userAgentOf(request),
      userIdFrom(hc),
      areaOfTaxOf(user),
      sessionIdFrom(hc))
}


trait FieldTransformer {
  def sessionIdFrom(hc: HeaderCarrier) = hc.sessionId.getOrElse("n/a")

  def areaOfTaxOf(option: Option[User]) = option.map(user => user.regimes.paye.map(_ => "paye").getOrElse("biztax")).getOrElse("n/a")

  def userIdFrom(hc: HeaderCarrier) = hc.userId.getOrElse("n/a")

  def userAgentOf(request: Request[AnyRef]) = request.headers.get("User-Agent").getOrElse("n/a")

  def ynValueOf(javascript: Boolean) = if (javascript) "Y" else "N"
}
