/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers.internal

import config.YtaConfig
import controllers.auth.{ AuthenticatedRequest, WithAuthRetrievals }
import javax.inject.Inject
import model.{ HostContext, Language }
import play.api.{ Configuration, Logger }
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.EventKeys
import uk.gov.hmrc.play.audit.http.connector.{ AuditConnector, AuditResult }
import uk.gov.hmrc.play.audit.model.{ DataEvent, EventTypes, ExtendedDataEvent }
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
              surveyForm = SurveyReOptInDeclinedDetailsForm().fill(SurveyReOptInDeclinedDetailsForm.Data(
                `choice-0305d33f-2e8d-4cb2-82d2-52132fc325fe` = None,
                `choice-ce34aa17-df2a-44fb-9d5c-4d930396483a` = None,
                `choice-d0edb491-6dcb-48a8-aeca-b16f01c541a5` = None,
                `choice-1e825e7d-6fc8-453f-8c20-1a7ed4d84ea5` = None,
                `choice-15d28c3f-9f33-4c44-aefa-165fc84b5e23` = None,
                reason = None,
                submissionType = None
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

  def auditSurvey(languagePreference: Some[Language], userData: SurveyReOptInDeclinedDetailsForm.Data)(
    implicit request: AuthenticatedRequest[_]): Future[AuditResult] =
    auditConnector.sendExtendedEvent(
      ExtendedDataEvent(
        auditSource = AppName.fromConfiguration(configuration),
        auditType = EventTypes.Succeeded,
        tags = Map(EventKeys.TransactionName -> "Re-OptIn Declined Survey Answered"),
        detail = Json.toJson(
          EventDetail(
            submissionType = userData.submissionType.getOrElse("N/A"),
            utr = request.saUtr.getOrElse("N/A"),
            nino = request.nino.getOrElse("N/A"),
            choices = Map(
              "choice-0305d33f-2e8d-4cb2-82d2-52132fc325fe" -> QuestionAnswer(
                question =
                  messagesApi("paperless.survey.reoptin_declined.choice.0305d33f-2e8d-4cb2-82d2-52132fc325fe", "N/A")(
                    request.lang),
                answer = userData.`choice-0305d33f-2e8d-4cb2-82d2-52132fc325fe`.getOrElse(false).toString
              ),
              "choice-ce34aa17-df2a-44fb-9d5c-4d930396483a" -> QuestionAnswer(
                question =
                  messagesApi("paperless.survey.reoptin_declined.choice.ce34aa17-df2a-44fb-9d5c-4d930396483a", "N/A")(
                    request.lang),
                answer = userData.`choice-ce34aa17-df2a-44fb-9d5c-4d930396483a`.getOrElse(false).toString
              ),
              "choice-d0edb491-6dcb-48a8-aeca-b16f01c541a5" -> QuestionAnswer(
                question =
                  messagesApi("paperless.survey.reoptin_declined.choice.d0edb491-6dcb-48a8-aeca-b16f01c541a5", "N/A")(
                    request.lang),
                answer = userData.`choice-d0edb491-6dcb-48a8-aeca-b16f01c541a5`.getOrElse(false).toString
              ),
              "choice-1e825e7d-6fc8-453f-8c20-1a7ed4d84ea5" -> QuestionAnswer(
                question =
                  messagesApi("paperless.survey.reoptin_declined.choice.1e825e7d-6fc8-453f-8c20-1a7ed4d84ea5", "N/A")(
                    request.lang),
                answer = userData.`choice-1e825e7d-6fc8-453f-8c20-1a7ed4d84ea5`.getOrElse(false).toString
              ),
              "choice-15d28c3f-9f33-4c44-aefa-165fc84b5e23" -> QuestionAnswer(
                question =
                  messagesApi("paperless.survey.reoptin_declined.choice.15d28c3f-9f33-4c44-aefa-165fc84b5e23", "N/A")(
                    request.lang),
                answer = userData.`choice-15d28c3f-9f33-4c44-aefa-165fc84b5e23`.getOrElse(false).toString
              )
            ),
            reason = userData.reason.getOrElse("N/A"),
            language = languagePreference.getOrElse("N/A").toString
          ))
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
              hasErrors = (formWithErrors: Form[SurveyReOptInDeclinedDetailsForm.Data]) =>
                if (formWithErrors.data.getOrElse("submissionType", "submitted") == "submitted") {
                  Future.successful(
                    BadRequest(
                      reOptinDeclinedSurvey(
                        formWithErrors,
                        controllers.internal.routes.SurveyController.submitReOptInDeclinedSurveyForm(hostContext))))
                } else {
                  val userData = SurveyReOptInDeclinedDetailsForm.Data(
                    `choice-0305d33f-2e8d-4cb2-82d2-52132fc325fe` =
                      formWithErrors.data.get("choice-0305d33f-2e8d-4cb2-82d2-52132fc325fe").map(_.toBoolean),
                    `choice-ce34aa17-df2a-44fb-9d5c-4d930396483a` =
                      formWithErrors.data.get("choice-ce34aa17-df2a-44fb-9d5c-4d930396483a").map(_.toBoolean),
                    `choice-d0edb491-6dcb-48a8-aeca-b16f01c541a5` =
                      formWithErrors.data.get("choice-d0edb491-6dcb-48a8-aeca-b16f01c541a5").map(_.toBoolean),
                    `choice-1e825e7d-6fc8-453f-8c20-1a7ed4d84ea5` =
                      formWithErrors.data.get("choice-1e825e7d-6fc8-453f-8c20-1a7ed4d84ea5").map(_.toBoolean),
                    `choice-15d28c3f-9f33-4c44-aefa-165fc84b5e23` =
                      formWithErrors.data.get("choice-15d28c3f-9f33-4c44-aefa-165fc84b5e23").map(_.toBoolean),
                    reason = formWithErrors.data
                      .get("reason")
                      .map(_.substring(0, SurveyReOptInDeclinedDetailsForm.reasonMaxLength)),
                    submissionType = formWithErrors.data.get("submissionType")
                  )
                  auditSurvey(languagePreference = Some(lang), userData)
                  Future.successful(Redirect(routes.ManagePaperlessController.checkSettings(hostContext)))
              },
              success = (userData: SurveyReOptInDeclinedDetailsForm.Data) => {
                auditSurvey(languagePreference = Some(lang), userData)
                Future.successful(Redirect(routes.ManagePaperlessController.checkSettings(hostContext)))
              }
            )
        } else Future.successful(Redirect(routes.ManagePaperlessController.checkSettings(hostContext)))
      }
  }
}
