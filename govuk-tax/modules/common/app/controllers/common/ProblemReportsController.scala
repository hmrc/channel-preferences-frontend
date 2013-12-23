package controllers.common

import controllers.common.actions.Actions
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import controllers.common.service.Connectors
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._
import play.api.mvc.Request


class ProblemReportsController(override val auditConnector: AuditConnector)(implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with AllRegimeRoots {

  def this() = this(Connectors.auditConnector)(Connectors.authConnector)

  val form = Form[ProblemReport](
    mapping(
      "report-action" -> text.verifying("error.common.problem_report.action_mandatory", action => !action.isEmpty),
      "report-error" -> text.verifying("error.common.problem_report.action_mandatory", error => !error.isEmpty),
      "isJavascript" -> boolean
    )(ProblemReport.apply)(ProblemReport.unapply)
  )

  def report = WithNewSessionTimeout(UnauthorisedAction {
    implicit request => {
      form.bindFromRequest.fold(
        error => {
          if (!error.data.getOrElse("isJavascript", "true").toBoolean) {
            responseForNoJsBrowsers(true)
          } else {
            BadRequest(Json.toJson(Map("status" -> "ERROR")))
          }
        },
        problemReport => {
          // TODO, Store here the feedback

          if (!problemReport.isJavascript) {
            responseForNoJsBrowsers(false)
          } else {
            Ok(Json.toJson(
              Map("status" -> "OK",
                  "message" -> "<h2 id=\"feedback-thank-you-header\">Thank you for your help.</h2> <p>If you have more extensive feedback, please visit the <a href='/contact'>contact page</a>.</p>"
              )
            ))
          }
        })
    }
  })
  
  private def responseForNoJsBrowsers(hasErrors: Boolean)(implicit request: Request[AnyRef]) = Ok(views.html.problem_reports_confirmation(hasErrors, request.headers.get("referer").getOrElse("/home")))

}

case class ProblemReport(reportAction: String, reportError: String, isJavascript: Boolean)