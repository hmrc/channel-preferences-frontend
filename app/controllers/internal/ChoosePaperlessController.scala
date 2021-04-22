/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers.internal

import config.YtaConfig
import connectors._
import controllers.auth.{ AuthenticatedRequest, WithAuthRetrievals }
import controllers.internal.EmailOptInJourney._
import controllers.internal.PaperlessChoice.{ OptedIn, OptedOut }
import controllers.{ ExternalUrlPrefixes, internal }
import model.JourneyType.MultiPage1
import model.{ Encrypted, HostContext, Language, PageType }
import org.joda.time.DateTime
import play.api.Configuration
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.http.connector.{ AuditConnector, AuditResult }
import uk.gov.hmrc.play.audit.model.{ DataCall, EventTypes, MergedDataEvent }
import uk.gov.hmrc.play.bootstrap.config.AppName
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }
import model.SurveyType

class ChoosePaperlessController @Inject() (
  ytaConfig: YtaConfig,
  entityResolverConnector: EntityResolverConnector,
  emailConnector: EmailConnector,
  auditConnector: AuditConnector,
  val authConnector: AuthConnector,
  externalUrlPrefixes: ExternalUrlPrefixes,
  configuration: Configuration,
  saPrintingPreference: views.html.sa.prefs.sa_printing_preference,
  saPrintingPreferenceVerifyEmail: views.html.sa_printing_preference_verify_email,
  saPrintingPreferenceOptinEmail: views.html.sa.prefs.sa_printing_preference_optin_email,
  saPrintingPreferenceOptinConfirmation: views.html.sa.prefs.sa_printing_preference_optin_confirmation,
  accountDetailsPrintingPreferenceConfirm: views.html.account_details_printing_preference_confirm,
  changeLanguage: views.html.change_language,
  mcc: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with OptInCohortCalculator with I18nSupport with WithAuthRetrievals
    with LanguageHelper {

  def redirectToDisplayFormWithCohort(
    emailAddress: Option[Encrypted[EmailAddress]],
    hostContext: HostContext
  ): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[AnyContent] => implicit hc =>
        Future.successful(createRedirectToDisplayFormWithCohort(emailAddress, hostContext))
      }
    }

  def redirectToDisplayFormWithCohortBySvc(
    svc: String,
    token: String,
    emailAddress: Option[Encrypted[EmailAddress]],
    hostContext: HostContext
  ): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[AnyContent] => implicit hc =>
        Future.successful(
          Redirect(routes.ChoosePaperlessController.displayFormBySvc(svc, token, emailAddress, hostContext))
        )
      }
    }

  def displayFormBySvc(
    svc: String,
    token: String,
    emailAddress: Option[Encrypted[EmailAddress]],
    hostContext: HostContext
  ) =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[AnyContent] => implicit hc =>
        auditPageShown(AccountDetails, CohortCurrent.ipage)
        val email = emailAddress.map(_.decryptedValue)
        hasStoredEmail(hostContext, Some(svc), Some(token)).map { (emailAlreadyStored: Boolean) =>
          val cohort = CohortCurrent.ipage
          cohort.journeyType match {
            case None =>
              Ok(
                saPrintingPreference(
                  emailForm = OptInDetailsForm().fill(
                    OptInDetailsForm.Data(
                      emailAddress = email,
                      preference = None,
                      acceptedTcs = None,
                      emailAlreadyStored = Some(emailAlreadyStored)
                    )
                  ),
                  submitPrefsFormAction =
                    internal.routes.ChoosePaperlessController.submitFormBySvc(svc, token, hostContext),
                  cohort = CohortCurrent.ipage
                )
              )
            case Some(MultiPage1) =>
              Redirect(
                routes.ChoosePaperlessController.displayOptIn(Some(svc), Some(token), hostContext)
              )
            case _ => BadRequest("Invalid Cohort")
          }
        }
      }
    }

  private def createRedirectToDisplayFormWithCohort(
    emailAddress: Option[Encrypted[EmailAddress]],
    hostContext: HostContext
  )(implicit request: AuthenticatedRequest[_], hc: HeaderCarrier) =
    Redirect(
      routes.ChoosePaperlessController.displayForm(Some(calculateCohort(hostContext)), emailAddress, hostContext)
    )

  def displayForm(
    cohort: Option[OptInCohort],
    emailAddress: Option[Encrypted[EmailAddress]],
    hostContext: HostContext
  ): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[AnyContent] => implicit hc =>
        cohort.fold(ifEmpty = Future.successful(createRedirectToDisplayFormWithCohort(emailAddress, hostContext))) {
          cohort =>
            auditPageShown(AccountDetails, cohort)
            val email: Option[EmailAddress] = emailAddress.map(_.decryptedValue)

            def form(emailAlreadyStored: Boolean): Form[_] =
              if (hostContext.termsAndConditions.contains("taxCredits"))
                OptInTaxCreditsDetailsForm().fill(
                  OptInTaxCreditsDetailsForm.Data(
                    emailAddress = email,
                    termsAndConditions = (None, None),
                    emailAlreadyStored = Some(emailAlreadyStored)
                  )
                )
              else if (cohort.pageType == PageType.ReOptInPage)
                ReOptInDetailsForm().fill(
                  ReOptInDetailsForm.Data(
                    emailAddress = hostContext.email.map(EmailAddress(_)),
                    preference = None,
                    acceptedTcs = None,
                    emailAlreadyStored = Some(emailAlreadyStored)
                  )
                )
              else
                OptInDetailsForm().fill(
                  OptInDetailsForm.Data(
                    emailAddress = email,
                    preference = None,
                    acceptedTcs = None,
                    emailAlreadyStored = Some(emailAlreadyStored)
                  )
                )

            cohort.journeyType match {
              case Some(MultiPage1) =>
                Future.successful(
                  Redirect(
                    routes.ChoosePaperlessController.displayOptIn(None, None, hostContext)
                  )
                )
              case _ =>
                hasStoredEmail(hostContext, None, None).map { emailAlreadyStored =>
                  Ok(
                    saPrintingPreference(
                      emailForm = form(emailAlreadyStored),
                      submitPrefsFormAction = internal.routes.ChoosePaperlessController.submitForm(hostContext),
                      cohort = cohort
                    )
                  )
                }
            }

        }
      }
    }

  def displayCohort(cohort: Option[OptInCohort]): Action[AnyContent] =
    Action.async { implicit request =>
      cohort.fold(Future.successful(BadRequest("Invalid cohort"))) { cohort =>
        val form =
          cohort.pageType match {
            case PageType.IPage       => OptInDetailsForm()
            case PageType.TCPage      => OptInTaxCreditsDetailsForm()
            case PageType.ReOptInPage => ReOptInDetailsForm()
            case _                    => throw (new Exception("Invalid cohort"))
          }
        implicit val authRequest = AuthenticatedRequest[AnyContent](request, None, None, None, None)
        Future.successful(
          Ok(
            saPrintingPreference(
              emailForm = form,
              submitPrefsFormAction = internal.routes.ChoosePaperlessController.cohortNop(),
              cohort = cohort
            )
          )
        )
      }
    }

  def cohortNop() =
    Action.async {
      Future.successful(Ok(""))
    }

  def cohortList(): Action[AnyContent] =
    Action.async { implicit request =>
      Future.successful(Ok(OptInCohort.listCohortWrites.writes((OptInCohortConfigurationValues.availableValues))))
    }

  def submitFormBySvc(implicit svc: String, token: String, hostContext: HostContext) =
    Action.async { implicit request =>
      withAuthenticatedRequest { authRequest: AuthenticatedRequest[_] => implicit hc =>
        val call = routes.ChoosePaperlessController.submitFormBySvc(svc, token, hostContext)
        val lang = languageType(request.lang.code)
        val formwithErrors = returnToFormWithErrors(call, CohortCurrent.ipage, authRequest) _

        OptInOrOutForm().bindFromRequest.fold[Future[Result]](
          hasErrors = formwithErrors,
          happyForm =>
            if (happyForm.optedIn.contains(false))
              saveAndAuditPreferences(
                digital = false,
                email = None,
                CohortCurrent.ipage,
                false,
                Some(svc),
                Some(token),
                Some(lang)
              )(authRequest, hostContext, hc)
            else
              OptInDetailsForm().bindFromRequest.fold[Future[Result]](
                hasErrors = formwithErrors,
                success = {
                  case emailForm @ OptInDetailsForm.Data((Some(emailAddress), _), _, Some(OptedIn), Some(true), _) =>
                    validateEmailAndSavePreference(
                      emailAddress,
                      emailForm.isEmailVerified,
                      emailForm.isEmailAlreadyStored,
                      CohortCurrent.ipage,
                      Some(svc),
                      Some(token),
                      Some(lang)
                    )(authRequest, hostContext, hc)
                  case _ =>
                    formwithErrors(OptInDetailsForm().bindFromRequest)
                }
              )
        )
      }
    }

  def displayOptIn(svc: Option[String], token: Option[String], hostContext: HostContext) =
    Action.async { implicit request: MessagesRequest[AnyContent] =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[_] => implicit hc =>
        val call = (svc, token) match {
          case (Some(s), Some(t)) => internal.routes.ChoosePaperlessController.submitOptInBySvc(s, t, hostContext)
          case _                  => internal.routes.ChoosePaperlessController.submitOptIn(hostContext)
        }
        hasStoredEmail(hostContext, None, None).map { emailAlreadyStored =>
          Ok(
            saPrintingPreference(
              emailForm = OptInStartForm().fill(
                OptInStartForm.Data(
                  choice = None,
                  emailAlreadyStored = Some(emailAlreadyStored)
                )
              ),
              submitPrefsFormAction = call,
              cohort = CohortCurrent.ipage
            )
          )
        }
      }
    }

  def submitOptIn(hostContext: HostContext) =
    Action.async { implicit request: MessagesRequest[AnyContent] =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[_] => implicit hc =>
        handleOptInChoice(None, None, hostContext)
      }
    }

  def submitOptInBySvc(implicit svc: String, token: String, hostContext: HostContext) =
    Action.async { implicit request: MessagesRequest[AnyContent] =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[_] => implicit hc =>
        handleOptInChoice(Some(svc), Some(token), hostContext)
      }
    }

  private def handleOptInChoice(svc: Option[String], token: Option[String], hostContext: HostContext)(implicit
    request: AuthenticatedRequest[_],
    hc: HeaderCarrier
  ) = {
    val call = (svc, token) match {
      case (Some(s), Some(t)) => routes.ChoosePaperlessController.submitOptInBySvc(s, t, hostContext)
      case _                  => routes.ChoosePaperlessController.submitOptIn(hostContext)
    }
    val lang = languageType(request.lang.code)
    val formwithErrors = returnToFormWithErrors(call, CohortCurrent.ipage, request) _

    OptInStartForm()
      .bindFromRequest()
      .fold(
        formwithErrors,
        happyForm =>
          if (happyForm.choice.contains(OptedOut))
            saveAndAuditPreferences(
              digital = false,
              email = None,
              CohortCurrent.ipage,
              false,
              svc,
              token,
              languagePreference = Some(lang),
              if (hostContext.survey && ytaConfig.surveyOptinPageEnabled && svc.isEmpty)
                Some(SurveyType.StandardInterruptOptOut)
              else None
            )(request, hostContext, hc)
          else
            Future.successful(
              Redirect(
                routes.ChoosePaperlessController.displayOptInEmail(svc, token, "", Some(false), hostContext)
              )
            )
      )
  }

  def saveOptinPreference(
    changeEmail: Boolean,
    emailAddress: String,
    svc: Option[String],
    token: Option[String],
    languagePreference: Some[Language],
    hostContext: HostContext
  )(implicit request: AuthenticatedRequest[_], hc: HeaderCarrier): Future[Result] = {
    val terms = CohortCurrent.ipage.terms -> TermsAccepted(true, Some(OptInPage.from(CohortCurrent.ipage)), None)
    val updtTandCRes =
      if (changeEmail) entityResolverConnector.changeEmailAddress(emailAddress)
      else
        entityResolverConnector
          .updateTermsAndConditionsForSvc(
            TermsAndConditionsUpdate
              .from(terms, Some(emailAddress), svc.isDefined && token.isDefined, languagePreference)(hostContext),
            svc,
            token
          )(hc, hostContext = hostContext)
          .map(prefStatus => auditChoice(AccountDetails, CohortCurrent.ipage, terms, Some(emailAddress), prefStatus))

    updtTandCRes.map(_ =>
      Redirect(
        routes.ChoosePaperlessController.displayOptInConfirmation(svc, token, emailAddress, hostContext)
      )
    )
  }

  def submitOptInEmail(
    svc: Option[String],
    token: Option[String],
    changeEmail: Option[Boolean],
    hostContext: HostContext
  ) =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[_] => implicit hc =>
        OptInEmailForm()
          .bindFromRequest()(request)
          .fold(
            formwithErrors =>
              Future.successful(
                BadRequest(
                  saPrintingPreferenceOptinEmail(
                    formwithErrors,
                    internal.routes.ChoosePaperlessController.submitOptInEmail(svc, token, changeEmail, hostContext)
                  )
                )
              ),
            happyForm =>
              saveOptinPreference(
                changeEmail.getOrElse(false),
                happyForm,
                svc,
                token,
                Some(languageType(request.lang.code)),
                hostContext
              )
          )
      }
    }

  def displayOptInEmail(
    svc: Option[String],
    token: Option[String],
    email: String,
    changeEmail: Option[Boolean],
    hostContext: model.HostContext
  ) =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[_] => implicit hc =>
        Future.successful(
          Ok(
            saPrintingPreferenceOptinEmail(
              OptInEmailForm().fill(email),
              internal.routes.ChoosePaperlessController.submitOptInEmail(svc, token, changeEmail, hostContext)
            )
          )
        )
      }
    }

  def displayOptInConfirmation(
    svc: Option[String],
    token: Option[String],
    email: String,
    hostContext: model.HostContext
  ) =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[_] => implicit hc =>
        Future.successful(
          Ok(
            saPrintingPreferenceOptinConfirmation(svc, token, email, hostContext)
          )
        )
      }
    }

  def submitForm(implicit hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[AnyContent] => implicit hc =>
        val cohort = calculateCohort(hostContext)
        val call = routes.ChoosePaperlessController.submitForm(hostContext)
        val lang = languageType(request.lang.code)
        val formwithErrors = returnToFormWithErrors(call, cohort, authRequest) _

        def handleTc()(implicit request: AuthenticatedRequest[_]): Future[Result] =
          OptInOrOutTaxCreditsForm().bindFromRequest.fold[Future[Result]](
            hasErrors = formwithErrors,
            happyForm =>
              if (happyForm.optedIn.contains(false))
                saveAndAuditPreferences(
                  digital = false,
                  email = None,
                  cohort,
                  false,
                  None,
                  None,
                  languagePreference = Some(lang)
                )
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
                        Some(lang)
                      )
                    case _ =>
                      formwithErrors(OptInDetailsForm().bindFromRequest)
                  }
                )
          )

        def handleGeneric()(implicit request: AuthenticatedRequest[_]): Future[Result] =
          OptInOrOutForm().bindFromRequest.fold[Future[Result]](
            formwithErrors,
            happyForm =>
              if (happyForm.optedIn.contains(false))
                saveAndAuditPreferences(
                  digital = false,
                  email = None,
                  cohort,
                  false,
                  None,
                  None,
                  languagePreference = Some(lang),
                  if (hostContext.survey && ytaConfig.surveyOptinPageEnabled) Some(SurveyType.StandardInterruptOptOut)
                  else None
                )
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
                        languagePreference = Some(lang)
                      )
                    case _ =>
                      formwithErrors(OptInDetailsForm().bindFromRequest)
                  }
                )
          )

        def handleReOptInGeneric()(implicit request: AuthenticatedRequest[_]): Future[Result] =
          ReOptInOrOutForm().bindFromRequest.fold[Future[Result]](
            hasErrors = formwithErrors,
            happyForm =>
              if (happyForm.optedIn.contains(false))
                saveAndAuditPreferences(
                  digital = false,
                  email = None,
                  cohort,
                  false,
                  None,
                  None,
                  languagePreference = Some(lang)
                )
              else
                ReOptInDetailsForm().bindFromRequest.fold[Future[Result]](
                  hasErrors = formwithErrors,
                  success = {
                    case emailForm @ ReOptInDetailsForm
                          .Data((Some(emailAddress), _), _, Some(OptedIn), Some(true), _) =>
                      validateEmailAndSavePreference(
                        emailAddress = emailAddress,
                        isEmailVerified = true,
                        isEmailAlreadyStored = true,
                        cohort = cohort,
                        svc = None,
                        token = None,
                        languagePreference = Some(lang)
                      )
                    case _ =>
                      formwithErrors(ReOptInDetailsForm().bindFromRequest)
                  }
                )
          )

        hostContext.cohort match {
          case Some(c) if c.pageType == PageType.ReOptInPage                 => handleReOptInGeneric()
          case Some(c) if c.pageType == PageType.IPage                       => handleGeneric()
          case None if hostContext.termsAndConditions.contains("taxCredits") => handleTc()
          case None                                                          => handleGeneric()
        }
      }
    }

  def returnToFormWithErrors(submitPrefsFormAction: Call, cohort: OptInCohort, request: AuthenticatedRequest[_])(
    form: Form[_]
  )(implicit hc: HeaderCarrier): Future[Result] = {
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
    languagePreference: Some[Language],
    surveyType: Option[SurveyType] = None
  )(implicit request: AuthenticatedRequest[_], hostContext: HostContext, hc: HeaderCarrier): Future[Result] = {
    val terms = cohort.terms -> TermsAccepted(digital, Some(OptInPage.from(cohort)), surveyType)

    entityResolverConnector
      .updateTermsAndConditionsForSvc(
        TermsAndConditionsUpdate.from(terms, email, (svc.isDefined && token.isDefined), languagePreference),
        svc,
        token
      )
      .map { preferencesStatus =>
        auditChoice(AccountDetails, cohort, terms, email, preferencesStatus)
        if (cohort.pageType == PageType.ReOptInPage)
          if (ytaConfig.surveyReOptInPage10Enabled && !digital)
            Redirect(routes.SurveyController.displayReOptInDeclinedSurveyForm(hostContext))
          else
            Redirect(routes.ManagePaperlessController.checkSettings(hostContext))
        else if (surveyType.isDefined)
          Redirect(routes.OptInSurveyController.displayOptinDeclinedSurveyForm(hostContext))
        else if (digital && !emailAlreadyStored) {
          val encryptedEmail = email map (emailAddress => Encrypted(EmailAddress(emailAddress)))
          Redirect(routes.ChoosePaperlessController.displayNearlyDone(encryptedEmail, hostContext))
        } else Redirect(hostContext.returnUrl)
      }
  }

  def validateEmailAndSavePreference(
    emailAddress: String,
    isEmailVerified: Boolean,
    isEmailAlreadyStored: Boolean,
    cohort: OptInCohort,
    svc: Option[String],
    token: Option[String],
    languagePreference: Some[Language]
  )(implicit request: AuthenticatedRequest[_], hostContext: HostContext, hc: HeaderCarrier): Future[Result] = {
    val emailVerificationStatus =
      if (isEmailVerified) Future.successful(true)
      else emailConnector.isValid(emailAddress)

    emailVerificationStatus.flatMap {
      case true =>
        saveAndAuditPreferences(
          digital = true,
          email = Some(emailAddress),
          cohort,
          isEmailAlreadyStored,
          svc,
          token,
          languagePreference
        )
      case false =>
        if (svc.isDefined && token.isDefined)
          Future.successful(
            Ok(
              saPrintingPreferenceVerifyEmail(
                emailAddress,
                cohort,
                controllers.internal.routes.ChoosePaperlessController.submitFormBySvc(svc.get, token.get, hostContext),
                controllers.internal.routes.ChoosePaperlessController
                  .redirectToDisplayFormWithCohortBySvc(
                    svc.get,
                    token.get,
                    Some(Encrypted(EmailAddress(emailAddress))),
                    hostContext
                  )
                  .url
              )
            )
          )
        else
          Future.successful(
            Ok(
              saPrintingPreferenceVerifyEmail(
                emailAddress,
                cohort,
                controllers.internal.routes.ChoosePaperlessController.submitForm(hostContext),
                controllers.internal.routes.ChoosePaperlessController
                  .redirectToDisplayFormWithCohort(Some(Encrypted(EmailAddress(emailAddress))), hostContext)
                  .url
              )
            )
          )
    }
  }

  private def auditPageShown(
    journey: Journey,
    cohort: OptInCohort
  )(implicit request: AuthenticatedRequest[_], hc: HeaderCarrier): Future[AuditResult] =
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
            "cohort"  -> cohort.toString
          ),
          generatedAt = DateTime.now().toDate.toInstant
        ),
        response = DataCall(
          tags = hc.toAuditTags("Show Print Preference Option", request.path),
          detail = hc.toAuditDetails(
            "utr"     -> request.saUtr.getOrElse("N/A"),
            "nino"    -> request.nino.getOrElse("N/A"),
            "journey" -> journey.toString,
            "cohort"  -> cohort.toString
          ),
          generatedAt = DateTime.now().toDate.toInstant
        )
      )
    )

  private def auditChoice(
    journey: Journey,
    cohort: OptInCohort,
    terms: (TermsType, TermsAccepted),
    emailOption: Option[String],
    preferencesStatus: PreferencesStatus
  )(implicit
    request: AuthenticatedRequest[_],
    message: play.api.i18n.Messages,
    hc: HeaderCarrier
  ): Future[AuditResult] =
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
          generatedAt = DateTime.now().toDate.toInstant
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
          generatedAt = DateTime.now().toDate.toInstant
        )
      )
    )

  private def hasStoredEmail(hostContext: HostContext, svc: Option[String], token: Option[String])(implicit
    hc: HeaderCarrier
  ): Future[Boolean] = {
    val terms = hostContext.termsAndConditions.getOrElse("generic")
    val f: Any => Boolean = (v: Any) =>
      v match {
        case Right(PreferenceNotFound(Some(_))) | Right(PreferenceFound(false, Some(_), _, _, _, _)) => true
        case _                                                                                       => false
      }

    if (svc.isDefined && token.isDefined)
      entityResolverConnector.getPreferencesStatusByToken(svc.get, token.get, terms) map f
    else entityResolverConnector.getPreferencesStatus(terms) map f
  }

  def displayNearlyDone(emailAddress: Option[Encrypted[EmailAddress]], hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[AnyContent] => implicit request =>
        implicit val hostContextImplicit = hostContext
        Future.successful(
          Ok(accountDetailsPrintingPreferenceConfirm(calculateCohort(hostContext), emailAddress.map(_.decryptedValue)))
        )
      }
    }

  def displayLanguageForm(implicit
    hostContext: HostContext
  ): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit authRequest: AuthenticatedRequest[AnyContent] => implicit hc =>
        entityResolverConnector.getPreferences() map { pref =>
          Ok(
            changeLanguage(
              languageForm =
                LanguageForm().fill(LanguageForm.Data(language = pref.map(_.lang.exists(_ == Language.Welsh)))),
              submitLanguageFormAction = internal.routes.ChoosePaperlessController.submitLanguageForm(hostContext)
            )
          )
        }
      }
    }

  def submitLanguageForm(implicit hostContext: HostContext) =
    Action.async { implicit request =>
      withAuthenticatedRequest { authRequest: AuthenticatedRequest[_] => implicit hc =>
        LanguageForm().bindFromRequest.fold[Future[Result]](
          formWithErrors => Future.successful(BadRequest("Unable to submit the form.")),
          happyForm => {
            val lang = happyForm.language.fold[Language](Language.English)(isWelsh =>
              if (isWelsh) Language.Welsh else Language.English
            )
            entityResolverConnector
              .updateTermsAndConditions(TermsAndConditionsUpdate.fromLanguage(Some(lang)))
              .map { preferencesStatus =>
                Redirect(routes.ManagePaperlessController.checkSettings(hostContext))
              }
          }
        )
      }
    }
}
