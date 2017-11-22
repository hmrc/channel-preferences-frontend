package controllers.internal

import config.Global
import connectors._
import controllers.internal.EmailOptInJourney._
import controllers.internal.PaperlessChoice.OptedIn
import controllers.{Authentication, FindTaxIdentifier, internal}
import model.{Encrypted, HostContext}
import play.api.Play
import play.api.Play.current
import play.api.data.Form
import play.api.i18n.Messages.Implicits._
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.http.connector.{AuditResult, AuditConnector}
import uk.gov.hmrc.play.audit.model.{EventTypes, ExtendedDataEvent}
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.{Actions, AuthContext}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

object ChoosePaperlessController extends ChoosePaperlessController {

  val auditConnector = Global.auditConnector
  val entityResolverConnector = EntityResolverConnector
  val emailConnector = EmailConnector

  override protected implicit def authConnector: AuthConnector = Global.authConnector
}

trait ChoosePaperlessController extends FrontendController with OptInCohortCalculator with Authentication with Actions with AppName with FindTaxIdentifier {

  def entityResolverConnector: EntityResolverConnector
  def emailConnector: EmailConnector
  def auditConnector: AuditConnector

  def redirectToDisplayFormWithCohort(emailAddress: Option[Encrypted[EmailAddress]], hostContext: HostContext): Action[AnyContent] = authenticated { implicit authContext => implicit request =>
    createRedirectToDisplayFormWithCohort(emailAddress, hostContext)
  }

  def redirectToDisplayFormWithCohortBySvc(svc: String, token: String, emailAddress: Option[Encrypted[EmailAddress]], hostContext: HostContext): Action[AnyContent] = authenticated { implicit authContext => implicit request =>
    Redirect(routes.ChoosePaperlessController.displayFormBySvc(svc, token, emailAddress, hostContext))
  }


  def displayFormBySvc(svc: String, token: String, emailAddress: Option[Encrypted[EmailAddress]], hostContext: HostContext) = authenticated.async { implicit authContext => implicit request => {
    auditPageShown(authContext, AccountDetails, IPage)
    val email = emailAddress.map(_.decryptedValue)
    hasStoredEmail(hostContext, Some(svc), Some(token)).map { (emailAlreadyStored: Boolean) => {
      Ok(views.html.sa.prefs.sa_printing_preference(
        emailForm = OptInDetailsForm().fill(OptInDetailsForm.Data(emailAddress = email, preference = None, acceptedTcs = None, emailAlreadyStored = Some(emailAlreadyStored))),
        submitPrefsFormAction = internal.routes.ChoosePaperlessController.submitFormBySvc(svc, token, hostContext),
        cohort = IPage
      ))
    }
    }
  }
  }

  private def createRedirectToDisplayFormWithCohort(emailAddress: Option[Encrypted[EmailAddress]], hostContext: HostContext)(implicit authContext: AuthContext) =
    Redirect(routes.ChoosePaperlessController.displayForm(Some(calculateCohort(hostContext)), emailAddress, hostContext))

  def displayForm(implicit cohort: Option[OptInCohort], emailAddress: Option[Encrypted[EmailAddress]], hostContext: HostContext) = authenticated.async { implicit authContext => implicit request =>
    cohort.fold(ifEmpty = Future.successful(createRedirectToDisplayFormWithCohort(emailAddress, hostContext))) { cohort => {
      auditPageShown(authContext, AccountDetails, cohort)
      val email = emailAddress.map(_.decryptedValue)

      def form(emailAlreadyStored: Boolean): Form[_] =
        if (hostContext.termsAndConditions.contains("taxCredits")) {
          OptInTaxCreditsDetailsForm().fill(OptInTaxCreditsDetailsForm.Data(emailAddress = email, termsAndConditions = (None, None), emailAlreadyStored = Some(emailAlreadyStored)))
        } else {
          OptInDetailsForm().fill(OptInDetailsForm.Data(emailAddress = email, preference = None, acceptedTcs = None, emailAlreadyStored = Some(emailAlreadyStored)))
        }


      hasStoredEmail(hostContext, None, None).map(emailAlreadyStored =>
        Ok(views.html.sa.prefs.sa_printing_preference(
          emailForm = form(emailAlreadyStored),
          submitPrefsFormAction = internal.routes.ChoosePaperlessController.submitForm(hostContext),
          cohort = cohort
        )))
    }}}

  def submitFormBySvc(implicit svc: String, token: String, hostContext: HostContext) = authenticated.async { implicit authContext => implicit request =>
    val call = routes.ChoosePaperlessController.submitFormBySvc(svc, token, hostContext)
    val formwithErrors = returnToFormWithErrors(call, IPage, request)_

    OptInOrOutForm().bindFromRequest.fold[Future[Result]](
      hasErrors = formwithErrors,
      happyForm =>
        if (happyForm.optedIn.contains(false)) saveAndAuditPreferences(digital = false, email = None, IPage, false, Some(svc), Some(token))
        else OptInDetailsForm().bindFromRequest.fold[Future[Result]](
          hasErrors = formwithErrors,
          success = {
            case emailForm@OptInDetailsForm.Data((Some(emailAddress), _), _, Some(OptedIn), Some(true), _) =>
              validateEmailAndSavePreference(emailAddress, emailForm.isEmailVerified, emailForm.isEmailAlreadyStored, IPage, Some(svc), Some(token))
            case _ =>
              formwithErrors(OptInDetailsForm().bindFromRequest)
          }
        )
    )
  }

  def submitForm(implicit hostContext: HostContext) = authenticated.async { implicit authContext => implicit request =>
    val cohort = calculateCohort(hostContext)
    val call = routes.ChoosePaperlessController.submitForm(hostContext)
    val formwithErrors = returnToFormWithErrors(call, cohort, request)_

    def handleTc(): Future[Result] = {
      OptInOrOutTaxCreditsForm().bindFromRequest.fold[Future[Result]](
        hasErrors = formwithErrors,
        happyForm =>
          if (happyForm.optedIn.contains(false)) saveAndAuditPreferences(digital = false, email = None, cohort, false, None, None)
          else OptInTaxCreditsDetailsForm().bindFromRequest.fold[Future[Result]](
            hasErrors = formwithErrors,
            success = {
              case emailForm@OptInTaxCreditsDetailsForm.Data((Some(emailAddress), _), _, _, (Some(true), Some(true))) =>
                validateEmailAndSavePreference(emailAddress, emailForm.isEmailVerified, emailForm.isEmailAlreadyStored, cohort, None, None)
              case _ =>
                formwithErrors(OptInDetailsForm().bindFromRequest)
            }
          )
      )
    }

    def handleGeneric(): Future[Result] = {
      OptInOrOutForm().bindFromRequest.fold[Future[Result]](
        hasErrors = formwithErrors,
        happyForm =>
          if (happyForm.optedIn.contains(false)) saveAndAuditPreferences(digital = false, email = None, cohort, false, None, None)
          else OptInDetailsForm().bindFromRequest.fold[Future[Result]](
            hasErrors = formwithErrors,
            success = {
              case emailForm@OptInDetailsForm.Data((Some(emailAddress), _), _, Some(OptedIn), Some(true), _) =>
                validateEmailAndSavePreference(emailAddress, emailForm.isEmailVerified, emailForm.isEmailAlreadyStored, cohort, None, None)
              case _ =>
                formwithErrors(OptInDetailsForm().bindFromRequest)
            }
          )
      )
    }

    if (hostContext.termsAndConditions.contains("taxCredits")) handleTc()
    else handleGeneric()
  }

  def returnToFormWithErrors(submitPrefsFormAction: Call, cohort: OptInCohort, request : Request[_])(form: Form[_]): Future[Result] = {
    Future.successful(BadRequest(views.html.sa.prefs.sa_printing_preference(form, submitPrefsFormAction, cohort)(request, applicationMessages)))
  }

  def saveAndAuditPreferences(digital: Boolean, email: Option[String], cohort: OptInCohort, emailAlreadyStored: Boolean, svc: Option[String], token: Option[String])
                             (implicit request : Request[_], hostContext: HostContext, authContext: AuthContext, hc: HeaderCarrier): Future[Result] = {
    val terms = cohort.terms -> TermsAccepted(digital)

    entityResolverConnector.updateTermsAndConditionsForSvc(terms, email, svc, token, (svc.isDefined && token.isDefined)).map(preferencesStatus => {
      auditChoice(authContext, AccountDetails, cohort, terms, email, preferencesStatus)
      if (digital && !emailAlreadyStored) {
        val encryptedEmail = email map (emailAddress => Encrypted(EmailAddress(emailAddress)))
        Redirect(routes.ChoosePaperlessController.displayNearlyDone(encryptedEmail, hostContext))
      } else Redirect(hostContext.returnUrl)
    })
  }

  def validateEmailAndSavePreference(emailAddress: String, isEmailVerified: Boolean, isEmailAlreadyStored: Boolean, cohort: OptInCohort, svc: Option[String], token: Option[String])
                                    (implicit request : Request[_], hostContext: HostContext, authContext: AuthContext, hc: HeaderCarrier) = {
    val emailVerificationStatus =
      if (isEmailVerified) Future.successful(true)
      else emailConnector.isValid(emailAddress)
    
    emailVerificationStatus.flatMap {
      case true => saveAndAuditPreferences(digital = true, email = Some(emailAddress), cohort, isEmailAlreadyStored, svc, token)
      case false =>
        if (svc.isDefined && token.isDefined) Future.successful(Ok(views.html.sa_printing_preference_verify_email(emailAddress, cohort,
          controllers.internal.routes.ChoosePaperlessController.submitFormBySvc(svc.get, token.get, hostContext),
          controllers.internal.routes.ChoosePaperlessController.redirectToDisplayFormWithCohortBySvc(svc.get, token.get, Some(Encrypted(EmailAddress(emailAddress))), hostContext).url)))
        else Future.successful(Ok(views.html.sa_printing_preference_verify_email(emailAddress, cohort, controllers.internal.routes.ChoosePaperlessController.submitForm(hostContext),
          controllers.internal.routes.ChoosePaperlessController.redirectToDisplayFormWithCohort(Some(Encrypted(EmailAddress(emailAddress))), hostContext).url)))
    }
  }

  private def auditPageShown(authContext: AuthContext, journey: Journey, cohort: OptInCohort)(implicit request: Request[_], hc: HeaderCarrier) =
    auditConnector.sendEvent(ExtendedDataEvent(
      auditSource = appName,
      auditType = EventTypes.Succeeded,
      tags = hc.toAuditTags("Show Print Preference Option", request.path),
      detail = Json.toJson(hc.toAuditDetails(
        "utr" -> findUtr(authContext).map(_.utr).getOrElse("N/A"),
        "nino" -> findNino(authContext).map(_.nino).getOrElse("N/A"),
        "journey" -> journey.toString,
        "cohort" -> cohort.toString
      ))
    ))

  private def auditChoice(authContext: AuthContext, journey: Journey, cohort: OptInCohort, terms: (TermsType, TermsAccepted), emailOption: Option[String], preferencesStatus: PreferencesStatus)(implicit request: Request[_], message: play.api.i18n.Messages, hc: HeaderCarrier) =
    auditConnector.sendEvent(ExtendedDataEvent(
      auditSource = appName,
      auditType = if (preferencesStatus == PreferencesFailure) EventTypes.Failed else EventTypes.Succeeded,
      tags = hc.toAuditTags("Set Print Preference", request.path),
      detail = Json.toJson(hc.toAuditDetails(
        "client" -> "YTA",
        "utr" -> findUtr(authContext).map(_.utr).getOrElse("N/A"),
        "nino" -> findNino(authContext).map(_.nino).getOrElse("N/A"),
        "journey" -> journey.toString,
        "digital" -> terms._2.accepted.toString,
        "cohort" -> cohort.toString,
        "TandCsScope" -> terms._1.toString.toLowerCase,
        "userConfirmedReadTandCs" -> terms._2.accepted.toString,
        "email" -> emailOption.getOrElse(""),
        "newUserPreferencesCreated" -> (preferencesStatus == PreferencesCreated).toString
      ))
    ))

  private def hasStoredEmail(hostContext: HostContext, svc: Option[String], token: Option[String])(implicit hc: HeaderCarrier): Future[Boolean] = {
    val terms = hostContext.termsAndConditions.getOrElse("generic")
    val f: Any => Boolean = (v: Any) =>  v match {
      case Right(PreferenceNotFound(Some(_))) | Right(PreferenceFound(false, Some(_))) => true
      case _ => false
    }

    if (svc.isDefined && token.isDefined)
      entityResolverConnector.getPreferencesStatusByToken(svc.get, token.get, terms) map(f)
    else entityResolverConnector.getPreferencesStatus(terms) map(f)
  }

  def displayNearlyDone(emailAddress: Option[Encrypted[EmailAddress]], hostContext: HostContext) = authenticated { implicit authContext => implicit request =>
    implicit val hostContextImplicit = hostContext
    Ok(views.html.account_details_printing_preference_confirm(calculateCohort(hostContext), emailAddress.map(_.decryptedValue)))
  }
}
