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


  val subject: String = "Contact form submission"
  val actionId: String = "confirmTicket"
  val source: String = "tickets"
  val formId: String = "FeedbackForm"
  val ticketKey: String = "ticketId"

  def this() = this(Connectors.auditConnector, Connectors.hmrcDeskproConnector, Connectors.keyStoreConnector)(Connectors.authConnector)

  val form = Form[ContactForm](
    mapping(
      "contact-name" -> text
        .verifying("error.common.problem_report.name_mandatory", name => !name.isEmpty)
        .verifying("error.common.problem_report.name_too_long", name => name.size < 70),
      "contact-email" -> email,
      "contact-comments" -> text
        .verifying("error.common.comments_mandatory", comment => !comment.isEmpty)
        .verifying("error.common.comments_too_long", comment => comment.size < 2000),
      "isJavascript" -> boolean,
      "referer" -> text
    )(ContactForm.apply)(ContactForm.unapply)
  )

  def index = WithNewSessionTimeout(AuthenticatedBy(GovernmentGateway)( {
    user => request => renderIndex(user, request)
  }))

  private[common] def renderIndex(implicit user: User, request: Request[AnyRef]) = Ok(views.html.contact(form.fill(ContactForm(request.headers.get("Referer").getOrElse("n/a")))))

  def submit = WithNewSessionTimeout(AuthenticatedBy(GovernmentGateway).async( {
    user => request => doSubmit(user, request)
  }))



  private[common] def doSubmit(implicit user: User, request: Request[AnyRef]) = {
    form.bindFromRequest()(request).fold(
      error => {
        Future.successful(BadRequest(views.html.contact(error)))
      },
      data => {
        import data._
        hmrcDeskproConnector.createTicket(contactName, contactEmail, subject, contactComments, referer, data.isJavascript, request, Some(user)).flatMap {
          ticket =>
            val ticketId = ticket.map(_.ticket_id.toString).getOrElse("Unknown")
            keyStoreConnector.addKeyStoreEntry[Map[String, String]](actionId, source, formId, Map(ticketKey -> ticketId)).map(
              _ => Redirect(routes.ContactController.thanks())
            )
        }

      })
  }


  def thanks = WithNewSessionTimeout(AuthenticatedBy(GovernmentGateway).async({
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
