package controllers.common


import controllers.common.actions.{HeaderCarrier, Actions}
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import controllers.common.service.Connectors
import play.api.data._
import play.api.data.Forms._
import play.api.mvc.Results._
import uk.gov.hmrc.common.microservice.deskpro.{Ticket, HmrcDeskproConnector}
import play.api.mvc.{AnyContent, Request}
import scala.concurrent.Future
import play.api.i18n.Messages
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector


class ContactController(override val auditConnector: AuditConnector, hmrcDeskproConnector: HmrcDeskproConnector, keyStoreConnector: KeyStoreConnector)(implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with AllRegimeRoots {

  val actionId: String = "confirmTicket"
  val source: String = "tickets"
  val formId: String = "FeedbackForm"
  val ticketKey: String = "ticketId"

  def this() = this(Connectors.auditConnector, Connectors.hmrcDeskproConnector, Connectors.keyStoreConnector)(Connectors.authConnector)

  val form = Form[ContactForm](
    mapping(
      "contact-name" -> text.verifying("error.common.problem_report.name_mandatory", action => !action.isEmpty),
      "contact-email" -> email,
      "contact-comments" -> text.verifying("error.common.comments_mandatory", action => !action.isEmpty),
      "isJavascript" -> boolean,
      "referer" -> text
    )(ContactForm.apply)(ContactForm.unapply)
  )

  def index = WithNewSessionTimeout(AuthenticatedBy(AuthenticatedOr404Provider)({
    user => request => renderIndex(user, request)
  }))

  private[common] def renderIndex(implicit user: User, request: Request[AnyRef]) = Ok(views.html.contact(form.fill(ContactForm(request.headers.get("Referer").getOrElse("n/a")))))

  def submit = WithNewSessionTimeout(AuthenticatedBy(AuthenticatedOr404Provider).async({
    user => request => doSubmit(user, request)
  }))



  private[common] def doSubmit(implicit user: User, request: Request[AnyRef]) = {
    form.bindFromRequest()(request).fold(
      error => {
        Future.successful(BadRequest(views.html.contact(error)))
      },
      data => {
        hmrcDeskproConnector.createTicket(createTicket(data)).flatMap {
          ticket =>
            val ticketId = ticket.map(_.ticket_id.toString).getOrElse("Unknown")
            keyStoreConnector.addKeyStoreEntry[Map[String, String]](actionId, source, formId, Map(ticketKey -> ticketId)).map(
              _ => Redirect(routes.ContactController.thanks())
            )
        }

      })
  }


  def createTicket(data: ContactForm)(implicit user: User, request: Request[AnyRef]): Ticket = {
    Ticket(
      data.contactName,
      data.contactEmail,
      "Contact form submission",
      data.contactComments,
      data.referer,
      if (data.isJavascript) "Y" else "N",
      request.headers.get("User-Agent").getOrElse("n/a"),
      hc.userId.getOrElse("n/a"),
      if (user.regimes.paye.isDefined) "paye" else "biztax",
      hc.sessionId.getOrElse("n/a"))
  }

  def thanks = WithNewSessionTimeout(AuthenticatedBy(AuthenticatedOr404Provider).async({
    implicit user => implicit request => doThanks(user, request)
  }))


  def doThanks(user: User, request: Request[AnyRef]) = {
    implicit val hc = HeaderCarrier(request)
    keyStoreConnector.getEntry[Map[String, String]](actionId, source, formId).map {
      keyStoreData =>
        val ticketId: String = keyStoreData.flatMap(_.get(ticketKey)).getOrElse("Unknown")
        Ok(views.html.contact_confirmation(ticketId)(user, request))
    }
  }
}

case class ContactForm(contactName: String, contactEmail: String, contactComments: String, isJavascript: Boolean, referer: String)

object ContactForm {
  def apply(referer: String): ContactForm = ContactForm("", "", "", false, referer)
}

object AuthenticatedOr404Provider extends AuthenticationProvider{

  def notFoundPage(request: Request[AnyContent]) = Future.successful(Right(NotFound(views.html.global_error(Messages("global.error.pageNotFound404.title"),
    Messages("global.error.pageNotFound404.heading"),
    Messages("global.error.pageNotFound404.message")))))

  def handleNotAuthenticated(request: Request[AnyContent], redirectToOrigin: Boolean) = {
    case UserCredentials(None, _) => notFoundPage(request)
    case UserCredentials(Some(_), None) => notFoundPage(request)
  }

}