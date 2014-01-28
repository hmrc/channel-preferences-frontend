package controllers.common.support

import controllers.common.actions.{HeaderCarrier, Actions}
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import controllers.common.service.Connectors
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._
import play.api.mvc.Request
import uk.gov.hmrc.common.microservice.deskpro.HmrcDeskproConnector
import play.api.i18n.Messages
import scala.concurrent.Future
import uk.gov.hmrc.common.microservice.deskpro.domain.TicketId
import controllers.common.{AnyAuthenticationProvider, AllRegimeRoots, BaseController}
import uk.gov.hmrc.common.microservice.domain.User
import controllers.common.validators.Validators._



class ProblemReportsController(override val auditConnector: AuditConnector, hmrcDeskproConnector: HmrcDeskproConnector)(implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with AllRegimeRoots {

  def this() = this(Connectors.auditConnector, Connectors.hmrcDeskproConnector)(Connectors.authConnector)

  val form = Form[ProblemReport](
    mapping(
      "report-name" -> text
        .verifying("error.common.problem_report.action_mandatory", action => !action.isEmpty)
        .verifying("error.common.problem_report.name_too_long", name => name.size <= 70),
      "report-email" -> emailWithDomain.verifying("deskpro.email_too_long", email => email.size <= 255),
      "report-action" -> text
        .verifying("error.common.problem_report.action_mandatory", action => !action.isEmpty)
        .verifying("error.common.comments_too_long", action => action.size <= 1000),
      "report-error" -> text
        .verifying("error.common.problem_report.action_mandatory", error => !error.isEmpty)
        .verifying("error.common.comments_too_long", error => error.size <= 1000),
      "isJavascript" -> boolean
    )(ProblemReport.apply)(ProblemReport.unapply)
  )

  def report = WithNewSessionTimeout(AuthenticatedBy(AnyAuthenticationProvider).async{
    implicit user => implicit request => doReport(request, Some(user))
  })

  def reportUnauthenticated = UnauthorisedAction.async(doReport(_))

  def doReport(implicit request: Request[AnyRef], user: Option[User] = None) = {
    form.bindFromRequest.fold(
      error => {
        if (!error.data.getOrElse("isJavascript", "true").toBoolean) {
          Future.successful(Ok(views.html.support.problem_reports_error_nonjavascript(referrerFrom(request))))
        } else {
          Future.successful(BadRequest(Json.toJson(Map("status" -> "ERROR"))))
        }
      },
      problemReport => {
        val ticket: Future[Option[TicketId]] = createTicket(problemReport, request, user)
        ticket.map {
          ticketOption =>
            val ticketId: String = ticketOption.map(_.ticket_id.toString).getOrElse("Unknown")
            if (!problemReport.isJavascript) Ok(views.html.support.problem_reports_confirmation_nonjavascript(ticketId))
            else Ok(Json.toJson(Map("status" -> "OK", "message" -> views.html.support.ticket_created_body(ticketId).toString())))
        }
      })
  }

  private def createTicket(problemReport: ProblemReport, request: Request[AnyRef], user: Option[User]) = {
    implicit val hc = HeaderCarrier(request)
    hmrcDeskproConnector.createTicket(
      problemReport.reportName,
      problemReport.reportEmail,
      "Support Request",
      problemMessage(problemReport.reportAction, problemReport.reportError),
      referrerFrom(request),
      problemReport.isJavascript,
      request,
      user
    )
  }

  private[support] def problemMessage(action: String, error: String): String = {
    s"""
    ${Messages("problem_report.action")}:
    $action

    ${Messages("problem_report.error")}:
    $error
    """
  }

  private def referrerFrom(request: Request[AnyRef]): String = {
    request.headers.get("referer").getOrElse("/home")
  }
}

case class ProblemReport(reportName: String, reportEmail: String, reportAction: String, reportError: String, isJavascript: Boolean)