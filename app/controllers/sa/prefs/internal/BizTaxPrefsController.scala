package controllers.sa.prefs.internal

import connectors.{EmailConnector, PreferencesConnector}
import controllers.sa.Encrypted
import controllers.sa.prefs.ExternalUrls.businessTaxHome
import controllers.sa.prefs._
import controllers.sa.prefs.config.Global
import controllers.sa.prefs.internal.EmailOptInJourney._
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{EventTypes, ExtendedDataEvent}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.frontend.auth.{Actions, AuthContext}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import AuthContextAvailability._

import scala.concurrent.Future

object BizTaxPrefsController extends BizTaxPrefsController with AppName with OptInCohortCalculator {

  override val auditConnector = Global.auditConnector
  override val preferencesConnector = PreferencesConnector
  override val emailConnector = EmailConnector

  override protected implicit def authConnector: AuthConnector = Global.authConnector
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

  def redirectToBTAOrInterstitialPage = AuthorisedFor(SaRegime).async {
    implicit user => implicit request => redirectToBTAOrInterstitialPageAction(user, request)
  }

  def displayPrefsForm(emailAddress: Option[Encrypted[EmailAddress]]) = AuthorisedFor(SaRegime) {
    implicit authContext => implicit request => redirectToPrefsFormWithCohort(emailAddress, authContext)
  }

  def displayPrefsFormForCohort(cohort: Option[OptInCohort], emailAddress: Option[Encrypted[EmailAddress]]) = AuthorisedFor(SaRegime).async {
    implicit user => implicit request => displayPrefsFormAction(emailAddress, cohort)
  }

  def displayInterstitialPrefsFormForCohort(cohort: Option[OptInCohort]) = AuthorisedFor(SaRegime).async {
    implicit user => implicit request => displayInterstitialPrefsFormAction(user, request, cohort)
  }

  def submitPrefsFormForInterstitial() = AuthorisedFor(SaRegime).async {
    implicit user => implicit request => submitPrefsFormAction(Interstitial)
  }

  def submitPrefsFormForNonInterstitial() = AuthorisedFor(SaRegime).async {
    implicit user => implicit request => submitPrefsFormAction(AccountDetails)(user, request, withBanner = true)
  }

  def thankYou(emailAddress: Option[controllers.sa.prefs.EncryptedEmail]) = AuthorisedFor(SaRegime) {
    implicit authContext => implicit request =>
      Ok(views.html.account_details_printing_preference_confirm(businessTaxHome, calculateCohort(authContext), emailAddress.map(_.decryptedValue)))
  }

  def termsAndConditions() = AuthorisedFor(SaRegime).async {
    implicit user => implicit request => termsAndConditionsPage()
  }

  def termsAndConditionsPage()(implicit request: Request[AnyRef], authContext: AuthContext) : Future[Result] =
    Future.successful(Ok(views.html.sa.prefs.sa_terms_and_conditions()))

  val getSavePrefsFromInterstitialCall = controllers.sa.prefs.internal.routes.BizTaxPrefsController.submitPrefsFormForInterstitial()
  val getSavePrefsFromNonInterstitialPageCall = controllers.sa.prefs.internal.routes.BizTaxPrefsController.submitPrefsFormForNonInterstitial()


  private[prefs] def redirectToBTAOrInterstitialPageAction(implicit authContext: AuthContext, request: Request[AnyRef]) =
    preferencesConnector.getPreferences(authContext.principal.accounts.sa.get.utr)(HeaderCarrier.fromSessionAndHeaders(request.session, request.headers)).map {
        case Some(saPreference) => Redirect(businessTaxHome)
      case None => redirectToInterstitialPageWithCohort(authContext)
    }


  private[prefs] def displayInterstitialPrefsFormAction(implicit authContext: AuthContext, request: Request[AnyRef], possibleCohort: Option[OptInCohort]) = {
    implicit val hc = HeaderCarrier.fromSessionAndHeaders(request.session, request.headers)
    val saUtr = authContext.principal.accounts.sa.get.utr
    preferencesConnector.getPreferences(saUtr).flatMap {
      case Some(saPreference) => Future.successful(Redirect(businessTaxHome))
      case None =>
        possibleCohort.fold(ifEmpty = Future.successful(redirectToInterstitialPageWithCohort(authContext))) { cohort =>
          preferencesConnector.saveCohort(saUtr, calculateCohort(authContext)).map { case _ =>
            auditPageShown(saUtr, Interstitial, cohort)
            displayPreferencesFormAction(None, getSavePrefsFromInterstitialCall, cohort = cohort)
          }
        }
    }
  }

  private[prefs] def displayPrefsFormAction(emailAddress: Option[Encrypted[EmailAddress]], possibleCohort: Option[OptInCohort])(implicit authContext: AuthContext, request: Request[AnyRef]) = {
    val saUtr = authContext.principal.accounts.sa.get.utr
    possibleCohort.fold(ifEmpty = Future.successful(redirectToPrefsFormWithCohort(emailAddress, authContext))) { cohort =>
      preferencesConnector.saveCohort(saUtr, calculateCohort(authContext)).map { case _ =>
        auditPageShown(saUtr, AccountDetails, cohort)
        displayPreferencesFormAction(emailAddress.map(_.decryptedValue), getSavePrefsFromNonInterstitialPageCall, withBanner = true, cohort)
      }
    }
  }

  private[prefs] def submitPrefsFormAction(journey: Journey)(implicit authContext: AuthContext, request: Request[AnyRef], withBanner: Boolean = false) = {
    val cohort = calculateCohort(authContext)
    def saveAndAuditPreferences(utr:SaUtr, digital: Boolean, email: Option[String], acceptedTAndCs:Boolean, hc: HeaderCarrier):Future[Result] = {
      implicit val headerCarrier = hc
      for {
        _ <- preferencesConnector.saveCohort(utr, calculateCohort(authContext))
        _ <- preferencesConnector.savePreferences(utr, digital, email)
      } yield {
        auditChoice(utr, journey, cohort, digital, email,acceptedTAndCs)
        digital match {
          case true =>
            Redirect(routes.BizTaxPrefsController.thankYou(email map (emailAddress => Encrypted(EmailAddress(emailAddress)))))
          case false => Redirect(ExternalUrls.businessTaxHome)
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

  def getSavePrefFormAction(implicit withBanner: Boolean) = {
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

  private def auditChoice(utr: SaUtr, journey: Journey, cohort: OptInCohort, digital: Boolean, emailOption: Option[String], acceptedTAndCs:Boolean)(implicit request: Request[_], hc: HeaderCarrier) =
    auditConnector.sendEvent(ExtendedDataEvent(
      auditSource = appName,
      auditType = EventTypes.Succeeded,
      tags = hc.toAuditTags("Set Print Preference", request.path),
      detail = Json.toJson(hc.toAuditDetails(
        "utr" -> utr.toString,
        "journey" -> journey.toString,
        "digital" -> digital.toString,
        "cohort" -> cohort.toString,
        "userConfirmedReadTandCs" -> acceptedTAndCs.toString,
        "email" -> emailOption.getOrElse("")))))

  private def redirectToInterstitialPageWithCohort(authContext: AuthContext) =
    Redirect(routes.BizTaxPrefsController.displayInterstitialPrefsFormForCohort(Some(calculateCohort(authContext))))

  private def redirectToPrefsFormWithCohort(emailAddress: Option[Encrypted[EmailAddress]], authContext: AuthContext) =
    Redirect(routes.BizTaxPrefsController.displayPrefsFormForCohort(Some(calculateCohort(authContext)), emailAddress))
}

