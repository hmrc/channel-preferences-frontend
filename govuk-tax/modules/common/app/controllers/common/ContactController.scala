package controllers.common


import controllers.common.actions.Actions
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import controllers.common.service.Connectors
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._
import play.api.mvc.Request


class ContactController(override val auditConnector: AuditConnector)(implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with AllRegimeRoots {

  def this() = this(Connectors.auditConnector)(Connectors.authConnector)

  val form = Form[ContactForm](
    mapping(
      "contact-name" -> text.verifying("error.common.problem_report.name_mandatory", action => !action.isEmpty),
      "contact-comments" -> text.verifying("error.common.comments_mandatory", action => !action.isEmpty),
      "contact-email" -> email,
      "isJavascript" -> boolean
    )(ContactForm.apply)(ContactForm.unapply)
  )
  def index = WithNewSessionTimeout(UnauthorisedAction{
    implicit request =>
    Ok(views.html.contact(form))
  })

  def submit = WithNewSessionTimeout(UnauthorisedAction {
    implicit request => {
      form.bindFromRequest.fold(
        error => {
            BadRequest(views.html.contact(error))
        },
        problemReport => {
          // TODO, Store here the feedback

          if (!problemReport.isJavascript) {
            responseForNoJsBrowsers(false)
          } else {
            Ok(Json.toJson(
              Map("status" -> "OK",
                "message" -> "<h2 id=\"feedback-thank-you-header\">Thank you</h2> <p>Your comments will be reviewed by our customer support team.</p>"
              )
            ))
          }
        })
    }
  })

  private def responseForNoJsBrowsers(hasErrors: Boolean)(implicit request: Request[AnyRef]) = Ok(views.html.contact_confirmation(hasErrors, request.headers.get("referer").getOrElse("/home")))

}

case class ContactForm(contactName: String, contactEmail: String, contactComments: String, isJavascript: Boolean)