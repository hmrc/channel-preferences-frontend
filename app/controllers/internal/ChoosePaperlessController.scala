/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.internal

import connectors._
import controllers.auth.{AuthenticatedRequest, WithAuthRetrievals}
import controllers.internal.EmailOptInJourney._
import controllers.internal.PaperlessChoice.OptedIn
import controllers.{ExternalUrlPrefixes, internal}
import javax.inject.Inject
import model.{Encrypted, HostContext}
import org.joda.time.DateTime
import play.api.Configuration
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.{DataCall, EventTypes, MergedDataEvent}
import uk.gov.hmrc.play.bootstrap.config.AppName
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class ChoosePaperlessController @Inject()(
                                           entityResolverConnector: EntityResolverConnector,
                                           emailConnector: EmailConnector,
                                           auditConnector: AuditConnector,
                                           val authConnector: AuthConnector,
                                           externalUrlPrefixes: ExternalUrlPrefixes,
                                           configuration: Configuration,
                                           saPrintingPreference: views.html.sa.prefs.sa_printing_preference,
                                           saPrintingPreferenceVerifyEmail: views.html.sa_printing_preference_verify_email,
                                           accountDetailsPrintingPreferenceConfirm: views.html.account_details_printing_preference_confirm,
                                           mcc: MessagesControllerComponents)(implicit ec: ExecutionContext)
  extends FrontendController(mcc) with OptInCohortCalculator with I18nSupport with WithAuthRetrievals {

  def redirectToDisplayFormWithCohort(
                                       emailAddress: Option[Encrypted[EmailAddress]],
                                       hostContext: HostContext): Action[AnyContent] = Action.async { implicit request =>
    withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[AnyContent] => implicit hc =>
      Future.successful(createRedirectToDisplayFormWithCohort(emailAddress, hostContext))
    }
  }

  def redirectToDisplayFormWithCohortBySvc(
                                            svc: String,
                                            token: String,
                                            emailAddress: Option[Encrypted[EmailAddress]],
                                            hostContext: HostContext): Action[AnyContent] = Action.async { implicit request =>
    withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[AnyContent] => implicit hc =>
      Future.successful(
        Redirect(routes.ChoosePaperlessController.displayFormBySvc(svc, token, emailAddress, hostContext)))
    }
  }

  def displayFormBySvc(
                        svc: String,
                        token: String,
                        emailAddress: Option[Encrypted[EmailAddress]],
                        hostContext: HostContext) = Action.async { implicit request =>
    withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[AnyContent] => implicit hc =>
    {
      auditPageShown(AccountDetails, IPage)
      val email = emailAddress.map(_.decryptedValue)
      hasStoredEmail(hostContext, Some(svc), Some(token)).map { (emailAlreadyStored: Boolean) =>
      {
        Ok(
          saPrintingPreference(
            emailForm = OptInDetailsForm().fill(
              OptInDetailsForm.Data(
                emailAddress = email,
                preference = None,
                acceptedTcs = None,
                emailAlreadyStored = Some(emailAlreadyStored))),
            submitPrefsFormAction =
              internal.routes.ChoosePaperlessController.submitFormBySvc(svc, token, hostContext),
            cohort = IPage
          ))
      }
      }
    }
    }
  }

  private def createRedirectToDisplayFormWithCohort(
                                                     emailAddress: Option[Encrypted[EmailAddress]],
                                                     hostContext: HostContext)(implicit request: AuthenticatedRequest[_], hc: HeaderCarrier) =
    Redirect(
      routes.ChoosePaperlessController.displayForm(Some(calculateCohort(hostContext)), emailAddress, hostContext))

  def displayForm(
                   cohort: Option[OptInCohort],
                   emailAddress: Option[Encrypted[EmailAddress]],
                   hostContext: HostContext): Action[AnyContent] = Action.async { implicit request =>
    withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[AnyContent] => implicit hc =>
      cohort.fold(ifEmpty = Future.successful(createRedirectToDisplayFormWithCohort(emailAddress, hostContext))) {
        cohort =>
        {
          auditPageShown(AccountDetails, cohort)
          val email = emailAddress.map(_.decryptedValue)

          def form(emailAlreadyStored: Boolean): Form[_] =
            if (hostContext.termsAndConditions.contains("taxCredits")) {
              OptInTaxCreditsDetailsForm().fill(
                OptInTaxCreditsDetailsForm.Data(
                  emailAddress = email,
                  termsAndConditions = (None, None),
                  emailAlreadyStored = Some(emailAlreadyStored)))
            } else {
              OptInDetailsForm().fill(
                OptInDetailsForm.Data(
                  emailAddress = email,
                  preference = None,
                  acceptedTcs = None,
                  emailAlreadyStored = Some(emailAlreadyStored)))
            }

          hasStoredEmail(hostContext, None, None).map(
            emailAlreadyStored =>
              Ok(
                saPrintingPreference(
                  emailForm = form(emailAlreadyStored),
                  submitPrefsFormAction = internal.routes.ChoosePaperlessController.submitForm(hostContext),
                  cohort = cohort
                )))
        }
      }
    }
  }

  def submitFormBySvc(implicit svc: String, token: String, hostContext: HostContext) =
    Action.async { implicit request =>
      withAuthenticatedRequest { authRequest: AuthenticatedRequest[_] => implicit hc =>
        val call = routes.ChoosePaperlessController.submitFormBySvc(svc, token, hostContext)
        val lang = request.lang.code
        val formwithErrors = returnToFormWithErrors(call, IPage, authRequest) _

        OptInOrOutForm().bindFromRequest.fold[Future[Result]](
          hasErrors = formwithErrors,
          happyForm =>
            if (happyForm.optedIn.contains(false))
              saveAndAuditPreferences(digital = false, email = None, IPage, false, Some(svc), Some(token), lang)(
                authRequest,
                hostContext,
                hc)
            else
              OptInDetailsForm().bindFromRequest.fold[Future[Result]](
                hasErrors = formwithErrors,
                success = {
                  case emailForm @ OptInDetailsForm.Data((Some(emailAddress), _), _, Some(OptedIn), Some(true), _) =>
                    validateEmailAndSavePreference(
                      emailAddress,
                      emailForm.isEmailVerified,
                      emailForm.isEmailAlreadyStored,
                      IPage,
                      Some(svc),
                      Some(token),
                      lang
                    )(authRequest, hostContext, hc)
                  case _ =>
                    formwithErrors(OptInDetailsForm().bindFromRequest)
                }
              )
        )
      }
    }

  def submitForm(implicit hostContext: HostContext) = Action.async { implicit request =>
    withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[AnyContent] => implicit hc =>
      val cohort = calculateCohort(hostContext)
      val call = routes.ChoosePaperlessController.submitForm(hostContext)
      val lang = request.lang.code
      val formwithErrors = returnToFormWithErrors(call, cohort, authRequest) _

      def handleTc()(implicit request: AuthenticatedRequest[_]): Future[Result] =
        OptInOrOutTaxCreditsForm().bindFromRequest.fold[Future[Result]](
          hasErrors = formwithErrors,
          happyForm =>
            if (happyForm.optedIn.contains(false))
              saveAndAuditPreferences(digital = false, email = None, cohort, false, None, None, languagePreference = lang)
            else
              OptInTaxCreditsDetailsForm().bindFromRequest.fold[Future[Result]](
                hasErrors = formwithErrors,
                success = {
                  case emailForm @ OptInTaxCreditsDetailsForm
                  .Data((Some(emailAddress), _), _, _, (Some(true), Some(true))) =>
                    validateEmailAndSavePreference(
                      emailAddress,
                      emailForm.isEmailVerified,
                      emailForm.isEmailAlreadyStored,
                      cohort,
                      None,
                      None,
                      lang)
                  case _ =>
                    formwithErrors(OptInDetailsForm().bindFromRequest)
                }
              )
        )

      def handleGeneric()(implicit request: AuthenticatedRequest[_]): Future[Result] =
        OptInOrOutForm().bindFromRequest.fold[Future[Result]](
          hasErrors = formwithErrors,
          happyForm =>
            if (happyForm.optedIn.contains(false))
              saveAndAuditPreferences(digital = false, email = None, cohort, false, None, None, languagePreference = lang)
            else
              OptInDetailsForm().bindFromRequest.fold[Future[Result]](
                hasErrors = formwithErrors,
                success = {
                  case emailForm @ OptInDetailsForm.Data((Some(emailAddress), _), _, Some(OptedIn), Some(true), _) =>
                    validateEmailAndSavePreference(
                      emailAddress,
                      emailForm.isEmailVerified,
                      emailForm.isEmailAlreadyStored,
                      cohort,
                      None,
                      None,
                      languagePreference = lang)
                  case _ =>
                    formwithErrors(OptInDetailsForm().bindFromRequest)
                }
              )
        )

      if (hostContext.termsAndConditions.contains("taxCredits")) handleTc()
      else handleGeneric()
    }
  }

  def returnToFormWithErrors(submitPrefsFormAction: Call, cohort: OptInCohort, request: AuthenticatedRequest[_])(
    form: Form[_])(implicit hc: HeaderCarrier): Future[Result] = {
    implicit val req = request
    Future.successful(BadRequest(saPrintingPreference(form, submitPrefsFormAction, cohort)))
  }

  def saveAndAuditPreferences(
                               digital: Boolean,
                               email: Option[String],
                               cohort: OptInCohort,
                               emailAlreadyStored: Boolean,
                               svc: Option[String],
                               token: Option[String],
                               languagePreference: String)(
                               implicit request: AuthenticatedRequest[_],
                               hostContext: HostContext,
                               hc: HeaderCarrier): Future[Result] = {
    val terms = cohort.terms -> TermsAccepted(digital)

    entityResolverConnector
      .updateTermsAndConditionsForSvc(terms, email, svc, token, (svc.isDefined && token.isDefined), languagePreference)
      .map(preferencesStatus => {
        auditChoice(AccountDetails, cohort, terms, email, preferencesStatus)
        if (digital && !emailAlreadyStored) {
          val encryptedEmail = email map (emailAddress => Encrypted(EmailAddress(emailAddress)))
          Redirect(routes.ChoosePaperlessController.displayNearlyDone(encryptedEmail, hostContext))
        } else Redirect(hostContext.returnUrl)
      })
  }

  def validateEmailAndSavePreference(
                                      emailAddress: String,
                                      isEmailVerified: Boolean,
                                      isEmailAlreadyStored: Boolean,
                                      cohort: OptInCohort,
                                      svc: Option[String],
                                      token: Option[String],
                                      languagePreference: String)(implicit request: AuthenticatedRequest[_], hostContext: HostContext, hc: HeaderCarrier): Future[Result] = {
    val emailVerificationStatus =
      if (isEmailVerified) Future.successful(true)
      else emailConnector.isValid(emailAddress)

    emailVerificationStatus.flatMap {
      case true =>
        saveAndAuditPreferences(digital = true, email = Some(emailAddress), cohort, isEmailAlreadyStored, svc, token, languagePreference)
      case false =>
        if (svc.isDefined && token.isDefined)
          Future.successful(
            Ok(saPrintingPreferenceVerifyEmail(
              emailAddress,
              cohort,
              controllers.internal.routes.ChoosePaperlessController.submitFormBySvc(svc.get, token.get, hostContext),
              controllers.internal.routes.ChoosePaperlessController
                .redirectToDisplayFormWithCohortBySvc(
                  svc.get,
                  token.get,
                  Some(Encrypted(EmailAddress(emailAddress))),
                  hostContext)
                .url
            )))
        else
          Future.successful(
            Ok(saPrintingPreferenceVerifyEmail(
              emailAddress,
              cohort,
              controllers.internal.routes.ChoosePaperlessController.submitForm(hostContext),
              controllers.internal.routes.ChoosePaperlessController
                .redirectToDisplayFormWithCohort(Some(Encrypted(EmailAddress(emailAddress))), hostContext)
                .url
            )))
    }
  }

  private def auditPageShown(journey: Journey, cohort: OptInCohort)(
    implicit request: AuthenticatedRequest[_],
    hc: HeaderCarrier): Future[AuditResult] =
    auditConnector.sendMergedEvent(
      MergedDataEvent(
        auditSource = AppName.fromConfiguration(configuration),
        auditType = EventTypes.Succeeded,
        request = DataCall(
          tags = hc.toAuditTags("Show Print Preference Option", request.path),
          detail = hc.toAuditDetails(
            "utr"     -> request.saUtr.getOrElse("N/A"),
            "nino"    -> request.nino.getOrElse("N/A"),
            "journey" -> journey.toString,
            "cohort"  -> cohort.toString),
          generatedAt = DateTime.now()
        ),
        response = DataCall(
          tags = hc.toAuditTags("Show Print Preference Option", request.path),
          detail = hc.toAuditDetails(
            "utr"     -> request.saUtr.getOrElse("N/A"),
            "nino"    -> request.nino.getOrElse("N/A"),
            "journey" -> journey.toString,
            "cohort"  -> cohort.toString),
          generatedAt = DateTime.now()
        )
      )
    )

  private def auditChoice(
                           journey: Journey,
                           cohort: OptInCohort,
                           terms: (TermsType, TermsAccepted),
                           emailOption: Option[String],
                           preferencesStatus: PreferencesStatus)(
                           implicit request: AuthenticatedRequest[_],
                           message: play.api.i18n.Messages,
                           hc: HeaderCarrier): Future[AuditResult] =
    auditConnector.sendMergedEvent(
      MergedDataEvent(
        auditSource = AppName.fromConfiguration(configuration),
        auditType = if (preferencesStatus == PreferencesFailure) EventTypes.Failed else EventTypes.Succeeded,
        request = DataCall(
          tags = hc.toAuditTags("Set Print Preference", request.path),
          detail = hc.toAuditDetails(
            "client"                    -> "YTA",
            "utr"                       -> request.saUtr.getOrElse("N/A"),
            "nino"                      -> request.nino.getOrElse("N/A"),
            "journey"                   -> journey.toString,
            "digital"                   -> terms._2.accepted.toString,
            "cohort"                    -> cohort.toString,
            "TandCsScope"               -> terms._1.toString.toLowerCase,
            "userConfirmedReadTandCs"   -> terms._2.accepted.toString,
            "email"                     -> emailOption.getOrElse(""),
            "newUserPreferencesCreated" -> (preferencesStatus == PreferencesCreated).toString
          ),
          generatedAt = DateTime.now()
        ),
        response = DataCall(
          tags = hc.toAuditTags("Set Print Preference", request.path),
          detail = hc.toAuditDetails(
            "client"                    -> "YTA",
            "utr"                       -> request.saUtr.getOrElse("N/A"),
            "nino"                      -> request.nino.getOrElse("N/A"),
            "journey"                   -> journey.toString,
            "digital"                   -> terms._2.accepted.toString,
            "cohort"                    -> cohort.toString,
            "TandCsScope"               -> terms._1.toString.toLowerCase,
            "userConfirmedReadTandCs"   -> terms._2.accepted.toString,
            "email"                     -> emailOption.getOrElse(""),
            "newUserPreferencesCreated" -> (preferencesStatus == PreferencesCreated).toString
          ),
          generatedAt = DateTime.now()
        )
      ))

  private def hasStoredEmail(hostContext: HostContext, svc: Option[String], token: Option[String])(
    implicit hc: HeaderCarrier): Future[Boolean] = {
    val terms = hostContext.termsAndConditions.getOrElse("generic")
    val f: Any => Boolean = (v: Any) =>
      v match {
        case Right(PreferenceNotFound(Some(_))) | Right(PreferenceFound(false, Some(_), _)) => true
        case _                                                                           => false
      }

    if (svc.isDefined && token.isDefined)
      entityResolverConnector.getPreferencesStatusByToken(svc.get, token.get, terms) map (f)
    else entityResolverConnector.getPreferencesStatus(terms) map (f)
  }

  def displayNearlyDone(emailAddress: Option[Encrypted[EmailAddress]], hostContext: HostContext): Action[AnyContent] = Action.async {
    implicit request =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[AnyContent] => implicit request =>
        implicit val hostContextImplicit = hostContext
        Future.successful(
          Ok(accountDetailsPrintingPreferenceConfirm(calculateCohort(hostContext), emailAddress.map(_.decryptedValue))))
      }
  }
}
