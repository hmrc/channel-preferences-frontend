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
import AuthContextAvailability._

import scala.concurrent.Future

object DeprecatedYTABizTaxPrefsController extends BizTaxPrefsController with AppName with OptInCohortCalculator {
  override val auditConnector = Global.auditConnector
  override val preferencesConnector = PreferencesConnector
  override val emailConnector = EmailConnector
  override protected implicit def authConnector: AuthConnector = Global.authConnector

  implicit val hostContext = HostContext.defaultsForYta

  def redirectToBTAOrInterstitialPage = AuthorisedFor(SaRegime).async {
    implicit user => implicit request => redirectToBTAOrInterstitialPageAction(user, request)
  }

  def displayPrefsForm(emailAddress: Option[Encrypted[EmailAddress]]) = AuthorisedFor(SaRegime) { implicit authContext => implicit request =>
    redirectToPrefsFormWithCohort(emailAddress)
  }

  def displayPrefsFormForCohort(cohort: Option[OptInCohort], emailAddress: Option[Encrypted[EmailAddress]]) = AuthorisedFor(SaRegime).async {
    implicit user => implicit request => displayPrefsFormAction(emailAddress, cohort)
  }

  def displayInterstitialPrefsFormForCohort(implicit cohort: Option[OptInCohort]) = AuthorisedFor(SaRegime).async { implicit user => implicit request =>
    displayInterstitialPrefsFormAction
  }

  def submitPrefsFormForInterstitial() = AuthorisedFor(SaRegime).async {
    implicit user => implicit request => submitPrefsFormAction(Interstitial)
  }

  def submitPrefsFormForNonInterstitial() = AuthorisedFor(SaRegime).async { implicit user => implicit request =>
    submitPrefsFormAction(AccountDetails)(user, request, withBanner = true, hostContext)
  }

  def thankYou(emailAddress: Option[controllers.sa.prefs.EncryptedEmail]) = AuthorisedFor(SaRegime) {
    implicit authContext => implicit request =>
      Ok(views.html.account_details_printing_preference_confirm(calculateCohort(authContext), emailAddress.map(_.decryptedValue)))
  }
}

object BizTaxPrefsController extends BizTaxPrefsController with AppName with OptInCohortCalculator {

  override val auditConnector = Global.auditConnector
  override val preferencesConnector = PreferencesConnector
  override val emailConnector = EmailConnector
  override protected implicit def authConnector: AuthConnector = Global.authConnector

  def displayPrefsForm(implicit emailAddress: Option[Encrypted[EmailAddress]], hostContext: HostContext) = AuthorisedFor(SaRegime) { implicit authContext => implicit request =>
    redirectToPrefsFormWithCohort(emailAddress)
  }

  def displayPrefsFormForCohort(implicit cohort: Option[OptInCohort], emailAddress: Option[Encrypted[EmailAddress]], hostContext: HostContext) = AuthorisedFor(SaRegime).async {
    implicit user => implicit request => displayPrefsFormAction(emailAddress, cohort)
  }

  def submitPrefsFormForNonInterstitial(implicit hostContext: HostContext) = AuthorisedFor(SaRegime).async { implicit user => implicit request =>
    submitPrefsFormAction(AccountDetails)(user, request, withBanner = true, hostContext)
  }

  def thankYou(implicit emailAddress: Option[controllers.sa.prefs.EncryptedEmail], hostContext: HostContext) = AuthorisedFor(SaRegime) { implicit authContext => implicit request =>
    Ok(views.html.account_details_printing_preference_confirm(calculateCohort(authContext), emailAddress.map(_.decryptedValue)))
  }
}

trait BizTaxPrefsController
  extends FrontendController
  with Actions
  with PreferencesControllerHelper
  with AppName {

  def preferencesConnector: PreferencesConnector
  def emailConnector: EmailConnector
  def auditConnector: AuditConnector

  def calculateCohort(authContext: AuthContext): OptInCohort


  val getSavePrefsFromInterstitialCall = controllers.sa.prefs.internal.routes.DeprecatedYTABizTaxPrefsController.submitPrefsFormForInterstitial()
  def getSavePrefsFromNonInterstitialPageCall(implicit hostContext: HostContext) = controllers.sa.prefs.internal.routes.BizTaxPrefsController.submitPrefsFormForNonInterstitial(hostContext)

  private[prefs] def redirectToBTAOrInterstitialPageAction(implicit authContext: AuthContext, request: Request[AnyRef]) =
    preferencesConnector.getPreferences(authContext.principal.accounts.sa.get.utr)(HeaderCarrier.fromSessionAndHeaders(request.session, request.headers)).map {
      case Some(saPreference) if saPreference.digital == true => Redirect(businessTaxHome)
      case _ => redirectToInterstitialPageWithCohort(authContext)
    }


  private[prefs] def displayInterstitialPrefsFormAction(implicit authContext: AuthContext, request: Request[AnyRef], possibleCohort: Option[OptInCohort], hostContext: HostContext) = {
    implicit val hc = HeaderCarrier.fromSessionAndHeaders(request.session, request.headers)
    val saUtr = authContext.principal.accounts.sa.get.utr
    preferencesConnector.getPreferences(saUtr).flatMap {
      case Some(saPreference) if saPreference.digital == true => Future.successful(Redirect(businessTaxHome))
      case _ =>
        possibleCohort.fold(ifEmpty = Future.successful(redirectToInterstitialPageWithCohort(authContext))) { cohort =>
          preferencesConnector.saveCohort(saUtr, calculateCohort(authContext)).map { case _ =>
            auditPageShown(saUtr, Interstitial, cohort)
            displayPreferencesFormAction(None, getSavePrefsFromInterstitialCall, cohort = cohort)
          }
        }
    }
  }

  private[prefs] def displayPrefsFormAction(emailAddress: Option[Encrypted[EmailAddress]], possibleCohort: Option[OptInCohort])(implicit authContext: AuthContext, request: Request[AnyRef], hostContext: HostContext) = {
    val saUtr = authContext.principal.accounts.sa.get.utr
    possibleCohort.fold(ifEmpty = Future.successful(redirectToPrefsFormWithCohort(emailAddress))) { cohort =>
      preferencesConnector.saveCohort(saUtr, calculateCohort(authContext)).map { case _ =>
        auditPageShown(saUtr, AccountDetails, cohort)
        displayPreferencesFormAction(emailAddress.map(_.decryptedValue), getSavePrefsFromNonInterstitialPageCall, withBanner = true, cohort)
      }
    }
  }

  private[prefs] def submitPrefsFormAction(journey: Journey)(implicit authContext: AuthContext, request: Request[AnyRef], withBanner: Boolean = false, hostContext: HostContext) = {
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
          case true  => Redirect(routes.BizTaxPrefsController.thankYou(email map (emailAddress => Encrypted(EmailAddress(emailAddress))), hostContext))
          case false => Redirect(hostContext.returnUrl)
        }
      }
    }
    submitPreferencesForm(
      errorsView = getSubmitPreferencesView(getSavePrefFormAction, cohort),
      emailWarningView = views.html.sa_printing_preference_verify_email(_, cohort),
      emailConnector = emailConnector,
      saUtr = authContext.principal.accounts.sa.get.utr,
      savePreferences = saveAndAuditPreferences
    )
  }

  def getSavePrefFormAction(implicit withBanner: Boolean, hostContext: HostContext) = {
    if (withBanner)
      getSavePrefsFromNonInterstitialPageCall
    else
      getSavePrefsFromInterstitialCall
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

  protected def redirectToInterstitialPageWithCohort(authContext: AuthContext) =
    Redirect(routes.DeprecatedYTABizTaxPrefsController.displayInterstitialPrefsFormForCohort(Some(calculateCohort(authContext))))

  protected def redirectToPrefsFormWithCohort(emailAddress: Option[Encrypted[EmailAddress]])(implicit authContext: AuthContext, hostContext: HostContext) =
    Redirect(routes.BizTaxPrefsController.displayPrefsFormForCohort(Some(calculateCohort(authContext)), emailAddress, hostContext))
}

