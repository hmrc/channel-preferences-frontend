package controllers.sa.prefs.internal

import connectors._
import controllers.sa.prefs.ExternalUrls.businessTaxHome
import controllers.sa.prefs.config.Global
import controllers.sa.prefs.internal.EmailOptInJourney._
import controllers.sa.prefs.{Encrypted, _}
import hostcontext.HostContext
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{EventTypes, ExtendedDataEvent}
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.{Actions, AuthContext}
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

object DeprecatedYTAManageAccountChoosePaperlessController extends ChoosePaperlessController with ChoosePaperlessControllerDependencies {
  implicit val hostContext = HostContext.defaultsForYtaManageAccount

  def redirectToDisplayFormWithCohort(emailAddress: Option[Encrypted[EmailAddress]]) = AuthorisedFor(SaRegime) { implicit authContext => implicit request =>
    _redirectToDisplayFormWithCohort(emailAddress)
  }

  def displayForm(cohort: Option[OptInCohort], emailAddress: Option[Encrypted[EmailAddress]]) = AuthorisedFor(SaRegime).async { implicit user => implicit request =>
    _displayForm(AccountDetails, emailAddress, cohort)
  }

  def submitForm() = AuthorisedFor(SaRegime).async { implicit user => implicit request =>
    _submitForm(AccountDetails)
  }

  def displayNearlyDone(emailAddress: Option[controllers.sa.prefs.EncryptedEmail]) = AuthorisedFor(SaRegime) { implicit authContext => implicit request =>
    _displayNearlyDone(emailAddress)
  }
}

object DeprecatedYTALoginChoosePaperlessController extends ChoosePaperlessController with ChoosePaperlessControllerDependencies {
  implicit val hostContext = HostContext.defaultsForYtaInterstitials

  def redirectToDisplayFormWithCohortIfNotOptedIn = AuthorisedFor(SaRegime).async { implicit user => implicit request =>
    _redirectToDisplayFormWithCohortIfNotOptedIn
  }

  def displayFormIfNotOptedIn(implicit cohort: Option[OptInCohort]) = AuthorisedFor(SaRegime).async { implicit user => implicit request =>
    _displayFormIfNotOptedIn
  }

  def submitForm() = AuthorisedFor(SaRegime).async { implicit user => implicit request =>
    _submitForm(Interstitial)
  }
}

object ChoosePaperlessController extends ChoosePaperlessController with ChoosePaperlessControllerDependencies {

  def redirectToDisplayFormWithCohort(implicit emailAddress: Option[Encrypted[EmailAddress]], hostContext: HostContext) = AuthorisedFor(SaRegime) { implicit authContext => implicit request =>
    _redirectToDisplayFormWithCohort(emailAddress)
  }

  def displayForm(implicit cohort: Option[OptInCohort], emailAddress: Option[Encrypted[EmailAddress]], hostContext: HostContext) = AuthorisedFor(SaRegime).async { implicit user => implicit request =>
    _displayForm(AccountDetails, emailAddress, cohort)
  }

  def submitForm(implicit hostContext: HostContext) = AuthorisedFor(SaRegime).async { implicit user => implicit request =>
    _submitForm(AccountDetails)
  }

  def displayNearlyDone(implicit emailAddress: Option[controllers.sa.prefs.EncryptedEmail], hostContext: HostContext) = AuthorisedFor(SaRegime) { implicit authContext => implicit request =>
    _displayNearlyDone(emailAddress)
  }
}

trait ChoosePaperlessControllerDependencies extends AppName with OptInCohortCalculator {
  this: ChoosePaperlessController =>

  override val auditConnector = Global.auditConnector
  override val preferencesConnector = PreferencesConnector
  override val emailConnector = EmailConnector
  override protected implicit def authConnector: AuthConnector = Global.authConnector
}

trait ChoosePaperlessController
  extends FrontendController
  with Actions
  with PreferencesControllerHelper
  with AppName {

  def preferencesConnector: PreferencesConnector
  def emailConnector: EmailConnector
  def auditConnector: AuditConnector

  def calculateCohort(authContext: AuthContext): OptInCohort

  val getSavePrefsFromInterstitialCall = controllers.sa.prefs.internal.routes.DeprecatedYTALoginChoosePaperlessController.submitForm()
  def getSavePrefsFromNonInterstitialPageCall(implicit hostContext: HostContext) = controllers.sa.prefs.internal.routes.ChoosePaperlessController.submitForm(hostContext)

  private def returnIf(cond: => Future[Boolean])(implicit hostContext: HostContext, headerCarrier: HeaderCarrier) = new {
    def otherwise(f: => Future[Result]) = {
      cond flatMap {
        case true => Future.successful(Redirect(hostContext.returnUrl))
        case false => f
      }
    }
  }

  private def userAlreadyOptedIn(implicit authContext: AuthContext, headerCarrier: HeaderCarrier) = preferencesConnector.getPreferences(authContext.principal.accounts.sa.get.utr).map {
    case Some(saPreference) => saPreference.digital
    case Some(_) | None     => false
  }

  private[prefs] def _redirectToDisplayFormWithCohortIfNotOptedIn(implicit authContext: AuthContext, request: Request[AnyRef], hostContext: HostContext) =
    returnIf(userAlreadyOptedIn) otherwise {
      Future.successful(Redirect(routes.DeprecatedYTALoginChoosePaperlessController.displayFormIfNotOptedIn(Some(calculateCohort(authContext)))))
    }

  protected def _redirectToDisplayFormWithCohort(emailAddress: Option[Encrypted[EmailAddress]])(implicit authContext: AuthContext, hostContext: HostContext) =
    Redirect(routes.ChoosePaperlessController.displayForm(Some(calculateCohort(authContext)), emailAddress, hostContext))

  private[prefs] def _displayFormIfNotOptedIn(implicit authContext: AuthContext, request: Request[AnyRef], possibleCohort: Option[OptInCohort], hostContext: HostContext) = {
    returnIf(userAlreadyOptedIn) otherwise _displayForm(Interstitial, None, possibleCohort)
  }

  private[prefs] def _displayForm(journey: Journey, emailAddress: Option[Encrypted[EmailAddress]], possibleCohort: Option[OptInCohort])(implicit authContext: AuthContext, request: Request[AnyRef], hostContext: HostContext) = {
    val saUtr = authContext.principal.accounts.sa.get.utr
    possibleCohort.fold(ifEmpty = Future.successful(_redirectToDisplayFormWithCohort(emailAddress))) { cohort =>
      preferencesConnector.saveCohort(saUtr, calculateCohort(authContext)).map { case _ =>
        auditPageShown(saUtr, journey, cohort)
        displayPreferencesFormAction(emailAddress.map(_.decryptedValue), getSavePrefsFromNonInterstitialPageCall, cohort)
      }
    }
  }

  private[prefs] def _submitForm(journey: Journey)(implicit authContext: AuthContext, request: Request[AnyRef], hostContext: HostContext) = {
    val cohort = calculateCohort(authContext)
    def maybeActivateUser(utr: SaUtr, needsActivation: Boolean): Future[Boolean] = needsActivation match {
      case true => preferencesConnector.activateUser(utr, "")
      case false => Future.successful(false)
    }

    def saveAndAuditPreferences(utr:SaUtr, digital: Boolean, email: Option[String], hc: HeaderCarrier): Future[Result] = {
      implicit val headerCarrier = hc
      val terms = Generic -> TermsAccepted(digital)
      for {
        _ <- preferencesConnector.saveCohort(utr, calculateCohort(authContext))
        userCreated <- preferencesConnector.addTermsAndConditions(utr, terms, email)
        userActivated <- maybeActivateUser(utr, userCreated && digital)
      } yield {
        auditChoice(utr, journey, cohort, terms, email, userCreated, userActivated)
        digital match {
          case true  => Redirect(routes.ChoosePaperlessController.displayNearlyDone(email map (emailAddress => Encrypted(EmailAddress(emailAddress))), hostContext))
          case false => Redirect(hostContext.returnUrl)
        }
      }
    }
    submitPreferencesForm(
      errorsView = getSubmitPreferencesView(routes.ChoosePaperlessController.submitForm(hostContext), cohort),
      emailWarningView = views.html.sa_printing_preference_verify_email(_, cohort),
      emailConnector = emailConnector,
      saUtr = authContext.principal.accounts.sa.get.utr,
      savePreferences = saveAndAuditPreferences
    )
  }

  private def auditPageShown(utr: SaUtr, journey: Journey, cohort: OptInCohort)(implicit request: Request[_], hc: HeaderCarrier) =
    auditConnector.sendEvent(ExtendedDataEvent(
      auditSource = appName,
      auditType = EventTypes.Succeeded,
      tags = hc.toAuditTags("Show Print Preference Option", request.path),
      detail = Json.toJson(hc.toAuditDetails(
        "utr" -> utr.toString,
        "journey" -> journey.toString,
        "cohort" -> cohort.toString))))

  private def auditChoice(utr: SaUtr, journey: Journey, cohort: OptInCohort, terms: (TermsType, TermsAccepted), emailOption: Option[String], userCreated: Boolean, userActivated: Boolean)(implicit request: Request[_], hc: HeaderCarrier) =
    auditConnector.sendEvent(ExtendedDataEvent(
      auditSource = appName,
      auditType = if (!userCreated || (terms._2.accepted && !userActivated)) EventTypes.Failed else EventTypes.Succeeded,
      tags = hc.toAuditTags("Set Print Preference", request.path),
      detail = Json.toJson(hc.toAuditDetails(
        "client" -> "YTA",
        "utr" -> utr.toString,
        "journey" -> journey.toString,
        "digital" -> terms._2.accepted.toString,
        "cohort" -> cohort.toString,
        "TandCsScope" -> terms._1.toString.toLowerCase,
        "userConfirmedReadTandCs" -> terms._2.accepted.toString,
        "email" -> emailOption.getOrElse(""),
        "userCreated" -> userCreated.toString,
        "userActivated" -> userActivated.toString))))

  protected def _displayNearlyDone(emailAddress: Option[EncryptedEmail])(implicit authContext: AuthContext, request: Request[AnyRef], hostContext: HostContext): Result = {
    Ok(views.html.account_details_printing_preference_confirm(calculateCohort(authContext), emailAddress.map(_.decryptedValue)))
  }
}

