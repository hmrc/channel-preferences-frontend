package controllers.common


import controllers.common.actions.{HeaderCarrier, Actions}
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import controllers.common.service.Connectors
import play.api.data._
import play.api.data.Forms._
import uk.gov.hmrc.common.microservice.deskpro.{Ticket, HmrcDeskproConnector}


class ContactController(override val auditConnector: AuditConnector, hmrcDeskproConnector: HmrcDeskproConnector)(implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with AllRegimeRoots {

  def this() = this(Connectors.auditConnector, Connectors.hmrcDeskproConnector)(Connectors.authConnector)

  val form = Form[ContactForm](
    mapping(
      "contact-name" -> text.verifying("error.common.problem_report.name_mandatory", action => !action.isEmpty),
      "contact-email" -> email,
      "contact-comments" -> text.verifying("error.common.comments_mandatory", action => !action.isEmpty),
      "isJavascript" -> boolean,
      "referer" -> text
    )(ContactForm.apply)(ContactForm.unapply)
  )
  def index = WithNewSessionTimeout(UnauthorisedAction{
    implicit request =>
    Ok(views.html.contact(form.fill(ContactForm(request.headers.get("Referer").getOrElse("n/a")))))
  })

  def submit = WithNewSessionTimeout(UnauthorisedAction {
    implicit request => {
      form.bindFromRequest()(request).fold(
        error => {
            BadRequest(views.html.contact(error))
        },
        data => {
          implicit val hc = HeaderCarrier(request)
          hmrcDeskproConnector.createTicket(Ticket(
            data.contactName,
            data.contactEmail,
            "Contact form submission",
            data.contactComments,
            data.referer,
            if(data.isJavascript) "Y" else "N",
            request.headers.get("User-Agent").getOrElse("n/a"),
            hc.userId.getOrElse("n/a"),
            "paye|biztax",
            hc.sessionId.getOrElse("n/a")))

          Redirect(routes.ContactController.thanks())

        })
    }
  })

  def thanks = WithNewSessionTimeout(UnauthorisedAction {
    implicit request => Ok(views.html.contact_confirmation())
  })

}

case class ContactForm(contactName: String, contactEmail: String, contactComments: String, isJavascript: Boolean, referer: String)

object ContactForm {
  def apply(referer: String): ContactForm = ContactForm("", "", "", false, referer)
}