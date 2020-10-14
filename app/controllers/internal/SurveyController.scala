/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers.internal

import config.YtaConfig
import controllers.auth.{ AuthenticatedRequest, WithAuthRetrievals }
import javax.inject.Inject
import model.{ HostContext, Language }
import play.api.Configuration
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.EventKeys
import uk.gov.hmrc.play.audit.http.connector.{ AuditConnector, AuditResult }
import uk.gov.hmrc.play.audit.model.{ DataEvent, EventTypes }
import uk.gov.hmrc.play.bootstrap.config.AppName
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ ExecutionContext, Future }

class SurveyController @Inject()(
  ytaConfig: YtaConfig,
  auditConnector: AuditConnector,
  val authConnector: AuthConnector,
  configuration: Configuration,
  reOptinDeclinedSurvey: views.html.sa.prefs.surveys.reoptin_declined_survey,
  mcc: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport with WithAuthRetrievals with LanguageHelper {

  def displayReOptInDeclinedSurveyForm(implicit hostContext: HostContext): Action[AnyContent] = Action.async {
    implicit request =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[AnyContent] => implicit hc =>
        if (ytaConfig.surveyReOptInPage10Enabled) {
          Future.successful(
            Ok(reOptinDeclinedSurvey(
              surveyForm = SurveyReOptInDeclinedDetailsForm().fill(
                SurveyReOptInDeclinedDetailsForm.Data(
                  choice1 = false,
                  choice2 = false,
                  choice3 = false,
                  choice4 = false,
                  choice5 = false,
                  reason = None
                )),
              submitSurveyFormAction =
                controllers.internal.routes.SurveyController.submitReOptInDeclinedSurveyForm(hostContext)
            )))
        } else Future.successful(Redirect(routes.ManagePaperlessController.checkSettings(hostContext)))
      }
  }

  def returnToFormWithErrors(submitSurveyFormAction: Call)(form: Form[SurveyReOptInDeclinedDetailsForm.Data])(
    implicit hc: HeaderCarrier,
    request: AuthenticatedRequest[_],
    hostContext: HostContext): Future[Result] =
    Future.successful(BadRequest(reOptinDeclinedSurvey(form, submitSurveyFormAction)))

  def auditSurvey(languagePreference: Some[Language], form: SurveyReOptInDeclinedDetailsForm.Data)(
    implicit request: AuthenticatedRequest[_]): Future[AuditResult] =
    auditConnector.sendEvent(
      DataEvent(
        auditSource = AppName.fromConfiguration(configuration),
        auditType = EventTypes.Succeeded,
        tags = Map(EventKeys.TransactionName -> "Re-OptIn Declined Survey Answered"),
        detail = Map(
          "utr"     -> request.saUtr.getOrElse("N/A"),
          "nino"    -> request.nino.getOrElse("N/A"),
          "choice1" -> form.choice1.toString,
          "choice2" -> form.choice2.toString,
          "choice3" -> form.choice3.toString,
          "choice4" -> form.choice4.toString,
          "choice5" -> form.choice5.toString,
          "reason"  -> form.reason.getOrElse("N/A")
        )
      )
    )

  def submitReOptInDeclinedSurveyForm(implicit hostContext: HostContext): Action[AnyContent] = Action.async {
    implicit request =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[AnyContent] => implicit hc =>
        if (ytaConfig.surveyReOptInPage10Enabled) {
          val lang = languageType(request.lang.code)
          val call = routes.SurveyController.submitReOptInDeclinedSurveyForm(hostContext)
          val formwithErrors = returnToFormWithErrors(call) _
          SurveyReOptInDeclinedDetailsForm()
            .bindFromRequest()(request)
            .fold[Future[Result]](
              hasErrors = formwithErrors,
              form => {
                auditSurvey(languagePreference = Some(lang), form)
                Future.successful(Redirect(routes.ManagePaperlessController.checkSettings(hostContext)))
              }
            )
        } else Future.successful(Redirect(routes.ManagePaperlessController.checkSettings(hostContext)))
      }
  }
}
