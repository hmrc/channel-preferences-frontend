package controllers.common

import controllers.common.actions.Actions
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import controllers.common.service.Connectors
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._


class ErrorReportsController (override val auditConnector: AuditConnector)     (implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with AllRegimeRoots
{

  def this() = this(Connectors.auditConnector)(Connectors.authConnector)
  val form = Form(
    tuple(
      "action" -> text,
      "error" -> text,
      "isJavascript" -> boolean
    )
  )
  def report = WithNewSessionTimeout(UnauthorisedAction {
    implicit request =>  {
      form.bindFromRequest.fold(
      error => {BadRequest("Ouch")},
      success => {
        if (!success._3) {
        Ok(views.html.error_reports_confirmation(success._2, success._2))
      } else {
        Ok(Json.toJson(
          Map("status" -> "OK",
            "message" -> "<h2>Thank you for your help.</h2> <p>If you have more extensive feedback, please visit the <a href='/contact'>contact page</a>.</p>"
          )
        ))
      }
  } )}})

}
