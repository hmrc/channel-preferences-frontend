package uk.gov.hmrc.common.microservice.deskpro.domain

import controllers.common.actions.HeaderCarrier
import play.api.mvc.Request
import controllers.common.{GovernmentGateway, Ida, SessionKeys}

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
  def apply(name: String, email: String, subject: String, message: String, referrer: String, isJavascript: Boolean, hc: HeaderCarrier, request: Request[AnyRef]): Ticket =
    Ticket(
      name.trim,
      email,
      subject,
      message.trim,
      referrer,
      ynValueOf(isJavascript),
      userAgentOf(request),
      userIdFrom(hc),
      areaOfTaxOf(request),
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
  def apply(name: String, email: String, rating: String, subject: String, message: String, referrer: String, isJavascript: Boolean, hc: HeaderCarrier, request: Request[AnyRef]): Feedback =
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
      areaOfTaxOf(request),
      sessionIdFrom(hc))
}


trait FieldTransformer {
  def sessionIdFrom(hc: HeaderCarrier) = hc.sessionId.getOrElse("n/a")

  def areaOfTaxOf(request: Request[AnyRef]) = request.session.get(SessionKeys.authProvider) match {
                                                                case Some(Ida.id) =>  "paye"
                                                                case Some(GovernmentGateway.id) => "biztax"
                                                                case _ => "n/a" }

  def userIdFrom(hc: HeaderCarrier) = hc.userId.getOrElse("n/a")

  def userAgentOf(request: Request[AnyRef]) = request.headers.get("User-Agent").getOrElse("n/a")

  def ynValueOf(javascript: Boolean) = if (javascript) "Y" else "N"
}
