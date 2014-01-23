package controllers.common.support

import controllers.common.actions.{HeaderCarrier, Actions}
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import controllers.common.service.Connectors
import play.api.data._
import play.api.data.Forms._
import uk.gov.hmrc.common.microservice.deskpro.HmrcDeskproConnector
import play.api.mvc.Request
import scala.concurrent.Future
import uk.gov.hmrc.common.microservice.domain.User
import controllers.common.{GovernmentGateway, AllRegimeRoots, BaseController}
import uk.gov.hmrc.common.microservice.deskpro.domain.TicketId
import scala.concurrent.ExecutionContext.Implicits.global


class ContactController(override val auditConnector: AuditConnector, hmrcDeskproConnector: HmrcDeskproConnector, ticketCache: TicketCache)(implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with AllRegimeRoots {

  val subject: String = "Contact form submission"
  val formId: String = "ContactForm"

  def this() = this(Connectors.auditConnector, Connectors.hmrcDeskproConnector, TicketCache())(Connectors.authConnector)

  val form = Form[ContactForm](
    mapping(
      "contact-name" -> text
        .verifying("error.common.problem_report.name_mandatory", name => !name.trim.isEmpty)
        .verifying("error.common.problem_report.name_too_long", name => name.size <= 70),
      "contact-email" -> email.verifying("error.email_too_long", email => email.size <= 320),
      "contact-comments" -> text
        .verifying("error.common.comments_mandatory", comment => !comment.trim.isEmpty)
        .verifying("error.common.comments_too_long", comment => comment.size <= 2000),
      "isJavascript" -> boolean,
      "referer" -> text
    )(ContactForm.apply)(ContactForm.unapply)
  )

  def index = WithNewSessionTimeout(AuthenticatedBy(GovernmentGateway)({
    user => request => renderIndex(user, request)
  }))

  private[common] def renderIndex(implicit user: User, request: Request[AnyRef]) = Ok(views.html.support.contact(form.fill(ContactForm(request.headers.get("Referer").getOrElse("n/a")))))

  def submit = WithNewSessionTimeout(AuthenticatedBy(GovernmentGateway).async({
    user => request => doSubmit(user, request)
  }))


  private[common] def doSubmit(implicit user: User, request: Request[AnyRef]) = {
    form.bindFromRequest()(request).fold(
      error => {
        Future.successful(BadRequest(views.html.support.contact(error)))
      },
      data => {
        import data._
        redirectToThanks(hmrcDeskproConnector.createTicket(contactName, contactEmail, subject, contactComments, referer, data.isJavascript, request, Some(user)))
      })
  }

  def redirectToThanks(ticketId: Future[Option[TicketId]])(implicit request: Request[AnyRef]) = {
    implicit val hc = HeaderCarrier(request)
    ticketId.flatMap {
      ticket =>
        ticketCache.stashTicket(ticket, formId).map(_ => Redirect(routes.ContactController.thanks()))
    }
  }

  def thanks = WithNewSessionTimeout(AuthenticatedBy(GovernmentGateway).async({
    implicit user => implicit request => doThanks(user, request)
  }))

  def doThanks(user: User, request: Request[AnyRef]) = {
    implicit val hc = HeaderCarrier(request)
    ticketCache.popTicket(formId).map {
      ticketId => Ok(views.html.support.contact_confirmation(ticketId)(user, request))
    }
  }
}

case class ContactForm(contactName: String, contactEmail: String, contactComments: String, isJavascript: Boolean, referer: String)

object ContactForm {
  def apply(referer: String): ContactForm = ContactForm("", "", "", false, referer)
}
