/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers.internal

import config.YtaConfig
import controllers.auth.{ AuthenticatedRequest, WithAuthRetrievals }
import model.{ HostContext, Language }
import play.api.Configuration
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.EventKeys
import uk.gov.hmrc.play.audit.http.connector.{ AuditConnector, AuditResult }
import uk.gov.hmrc.play.audit.model.{ EventTypes, ExtendedDataEvent }
import uk.gov.hmrc.play.bootstrap.config.AppName
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

class OptInSurveyController @Inject() (
  ytaConfig: YtaConfig,
  auditConnector: AuditConnector,
  val authConnector: AuthConnector,
  configuration: Configuration,
  optinDeclinedSurvey: views.html.sa.prefs.surveys.optin_declined_survey,
  mcc: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport with WithAuthRetrievals with LanguageHelper {

  def displayOptinDeclinedSurveyForm(implicit hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[AnyContent] => implicit hc =>
        if (ytaConfig.surveyOptinPageEnabled)
          Future.successful(
            Ok(
              optinDeclinedSurvey(
                surveyForm = SurveyOptinDeclinedDetailsForm().fill(
                  SurveyOptinDeclinedDetailsForm.Data(
                    `choice-d210eccd-9ea1-48fd-a28e-25abbb7508fe` = None,
                    `choice-717c2da0-4411-41ad-9a78-b335786e7107` = None,
                    `choice-a6f84da8-9fd7-440d-915e-2a2f8a543c9b` = None,
                    `choice-bf74f47f-e9ce-4c15-a9aa-1af80a594861` = None,
                    `choice-ca31965c-dd40-4a2c-a606-fe961da485c0` = None,
                    reason = None,
                    submissionType = None
                  )
                ),
                submitSurveyFormAction =
                  controllers.internal.routes.OptInSurveyController.submitOptinDeclinedSurveyForm(hostContext)
              )
            )
          )
        else Future.successful(Redirect(routes.ManagePaperlessController.checkSettings(hostContext)))
      }
    }

  def returnToFormWithErrors(submitSurveyFormAction: Call)(
    form: Form[SurveyOptinDeclinedDetailsForm.Data]
  )(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_], hostContext: HostContext): Future[Result] =
    Future.successful(BadRequest(optinDeclinedSurvey(form, submitSurveyFormAction)))

  def auditSurvey(languagePreference: Some[Language], userData: SurveyOptinDeclinedDetailsForm.Data)(implicit
    request: AuthenticatedRequest[_]
  ): Future[AuditResult] =
    auditConnector.sendExtendedEvent(
      ExtendedDataEvent(
        auditSource = AppName.fromConfiguration(configuration),
        auditType = EventTypes.Succeeded,
        tags = Map(EventKeys.TransactionName -> "OptOut Declined Survey Answered"),
        detail = Json.toJson(
          EventDetail(
            submissionType = userData.submissionType.getOrElse("N/A"),
            utr = request.saUtr.getOrElse("N/A"),
            nino = request.nino.getOrElse("N/A"),
            choices = Map(
              "choice-d210eccd-9ea1-48fd-a28e-25abbb7508fe" -> QuestionAnswer(
                question =
                  messagesApi("paperless.survey.optin_declined.choice.d210eccd-9ea1-48fd-a28e-25abbb7508fe", "N/A")(
                    request.lang
                  ),
                answer = userData.`choice-d210eccd-9ea1-48fd-a28e-25abbb7508fe`.getOrElse(false).toString
              ),
              "choice-717c2da0-4411-41ad-9a78-b335786e7107" -> QuestionAnswer(
                question =
                  messagesApi("paperless.survey.optin_declined.choice.717c2da0-4411-41ad-9a78-b335786e7107", "N/A")(
                    request.lang
                  ),
                answer = userData.`choice-717c2da0-4411-41ad-9a78-b335786e7107`.getOrElse(false).toString
              ),
              "choice-a6f84da8-9fd7-440d-915e-2a2f8a543c9b" -> QuestionAnswer(
                question =
                  messagesApi("paperless.survey.optin_declined.choice.a6f84da8-9fd7-440d-915e-2a2f8a543c9b", "N/A")(
                    request.lang
                  ),
                answer = userData.`choice-a6f84da8-9fd7-440d-915e-2a2f8a543c9b`.getOrElse(false).toString
              ),
              "choice-bf74f47f-e9ce-4c15-a9aa-1af80a594861" -> QuestionAnswer(
                question =
                  messagesApi("paperless.survey.optin_declined.choice.bf74f47f-e9ce-4c15-a9aa-1af80a594861", "N/A")(
                    request.lang
                  ),
                answer = userData.`choice-bf74f47f-e9ce-4c15-a9aa-1af80a594861`.getOrElse(false).toString
              ),
              "choice-ca31965c-dd40-4a2c-a606-fe961da485c0" -> QuestionAnswer(
                question =
                  messagesApi("paperless.survey.optin_declined.choice.ca31965c-dd40-4a2c-a606-fe961da485c0", "N/A")(
                    request.lang
                  ),
                answer = userData.`choice-ca31965c-dd40-4a2c-a606-fe961da485c0`.getOrElse(false).toString
              )
            ),
            reason = userData.reason.getOrElse("N/A"),
            language = languagePreference.getOrElse("N/A").toString
          )
        )
      )
    )

  def submitOptinDeclinedSurveyForm(implicit hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[AnyContent] => implicit hc =>
        if (ytaConfig.surveyOptinPageEnabled) {
          val lang = languageType(request.lang.code)
          val call = routes.OptInSurveyController.submitOptinDeclinedSurveyForm(hostContext)
          val formwithErrors = returnToFormWithErrors(call) _
          SurveyOptinDeclinedDetailsForm()
            .bindFromRequest()(request)
            .fold[Future[Result]](
              hasErrors = (formWithErrors: Form[SurveyOptinDeclinedDetailsForm.Data]) =>
                if (formWithErrors.data.getOrElse("submissionType", "submitted") == "submitted")
                  Future.successful(
                    BadRequest(
                      optinDeclinedSurvey(
                        formWithErrors,
                        controllers.internal.routes.OptInSurveyController.submitOptinDeclinedSurveyForm(hostContext)
                      )
                    )
                  )
                else {
                  val userData = SurveyOptinDeclinedDetailsForm.Data(
                    `choice-d210eccd-9ea1-48fd-a28e-25abbb7508fe` =
                      formWithErrors.data.get("choice-d210eccd-9ea1-48fd-a28e-25abbb7508fe").map(_.toBoolean),
                    `choice-717c2da0-4411-41ad-9a78-b335786e7107` =
                      formWithErrors.data.get("choice-717c2da0-4411-41ad-9a78-b335786e7107").map(_.toBoolean),
                    `choice-a6f84da8-9fd7-440d-915e-2a2f8a543c9b` =
                      formWithErrors.data.get("choice-a6f84da8-9fd7-440d-915e-2a2f8a543c9b").map(_.toBoolean),
                    `choice-bf74f47f-e9ce-4c15-a9aa-1af80a594861` =
                      formWithErrors.data.get("choice-bf74f47f-e9ce-4c15-a9aa-1af80a594861").map(_.toBoolean),
                    `choice-ca31965c-dd40-4a2c-a606-fe961da485c0` =
                      formWithErrors.data.get("choice-ca31965c-dd40-4a2c-a606-fe961da485c0").map(_.toBoolean),
                    reason = formWithErrors.data
                      .get("reason")
                      .map(_.substring(0, SurveyOptinDeclinedDetailsForm.reasonMaxLength)),
                    submissionType = formWithErrors.data.get("submissionType")
                  )
                  auditSurvey(languagePreference = Some(lang), userData)
                  Future.successful(Redirect(routes.ManagePaperlessController.checkSettings(hostContext)))
                },
              success = (userData: SurveyOptinDeclinedDetailsForm.Data) => {
                auditSurvey(languagePreference = Some(lang), userData)
                Future.successful(Redirect(routes.ManagePaperlessController.checkSettings(hostContext)))
              }
            )
        } else Future.successful(Redirect(routes.ManagePaperlessController.checkSettings(hostContext)))
      }
    }
}
