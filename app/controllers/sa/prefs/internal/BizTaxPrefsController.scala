package controllers.sa.prefs.internal

import connectors.{EmailConnector, PreferencesConnector}
import controllers.common.BaseController
import controllers.sa.Encrypted
import controllers.sa.prefs.ExternalUrls.businessTaxHome
import controllers.sa.prefs._
import controllers.sa.prefs.internal.EmailOptInJourney._
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{EventTypes, ExtendedDataEvent}
import uk.gov.hmrc.play.auth.frontend.connectors.AuthConnector
import uk.gov.hmrc.play.config.{AppName, AuditConnector}
import uk.gov.hmrc.play.frontend.auth.{Actions, User}

import scala.concurrent.Future

object BizTaxPrefsController extends BizTaxPrefsController with AppName {

  override val auditConnector = AuditConnector
  override val preferencesConnector = PreferencesConnector
  override val emailConnector = EmailConnector

  override protected implicit def authConnector: AuthConnector = AuthConnector
}

trait BizTaxPrefsController
  extends BaseController
  with Actions
  with PreferencesControllerHelper
  with CohortCalculator[OptInCohort]
  with AppName {

  override val values = OptInCohort.values

  def preferencesConnector: PreferencesConnector
  def emailConnector: EmailConnector
  def auditConnector: AuditConnector

  def redirectToBTAOrInterstitialPage = AuthorisedFor(SaRegime).async {
    implicit user => implicit request => redirectToBTAOrInterstitialPageAction(user, request)
  }

  def displayPrefsForm(emailAddress: Option[Encrypted[EmailAddress]]) = AuthorisedFor(SaRegime) {
    implicit user => implicit request => redirectToPrefsFormWithCohort(emailAddress, user)
  }

  def displayPrefsFormForCohort(cohort: Option[Cohort], emailAddress: Option[Encrypted[EmailAddress]]) = AuthorisedFor(SaRegime).async {
    implicit user => implicit request => displayPrefsFormAction(emailAddress, cohort)
  }

  def displayInterstitialPrefsFormForCohort(cohort: Option[Cohort]) = AuthorisedFor(SaRegime).async {
    implicit user => implicit request => displayInterstitialPrefsFormAction(user, request, cohort)
  }

  def submitPrefsFormForInterstitial() = AuthorisedFor(SaRegime).async {
    implicit user => implicit request => submitPrefsFormAction(Interstitial)
  }

  def submitPrefsFormForNonInterstitial() = AuthorisedFor(SaRegime).async {
    implicit user => implicit request => submitPrefsFormAction(AccountDetails)(user, request, withBanner = true)
  }

  def thankYou(emailAddress: Option[controllers.sa.prefs.EncryptedEmail]) = AuthorisedFor(SaRegime) {
    implicit user => implicit request =>
      Ok(views.html.account_details_printing_preference_confirm(Some(user), businessTaxHome, calculateCohort(user), emailAddress.map(_.decryptedValue)))
  }

  def termsAndConditions() = AuthorisedFor(SaRegime).async {
    implicit user => implicit request => termsAndConditionsPage()
  }

  def termsAndConditionsPage()(implicit request: Request[AnyRef], user: User) : Future[Result] =
    Future.successful(Ok(views.html.sa.prefs.sa_terms_and_conditions()))

  val getSavePrefsFromInterstitialCall = controllers.sa.prefs.internal.routes.BizTaxPrefsController.submitPrefsFormForInterstitial()
  val getSavePrefsFromNonInterstitialPageCall = controllers.sa.prefs.internal.routes.BizTaxPrefsController.submitPrefsFormForNonInterstitial()


  private[prefs] def redirectToBTAOrInterstitialPageAction(implicit user: User, request: Request[AnyRef]) =
    preferencesConnector.getPreferences(user.userAuthority.accounts.sa.get.utr)(HeaderCarrier.fromSessionAndHeaders(request.session, request.headers)).map {
        case Some(saPreference) => Redirect(businessTaxHome)
      case None => redirectToInterstitialPageWithCohort(user)
    }


  private[prefs] def displayInterstitialPrefsFormAction(implicit user: User, request: Request[AnyRef], possibleCohort: Option[Cohort]) = {
    implicit val hc = HeaderCarrier.fromSessionAndHeaders(request.session, request.headers)
    val saUtr = user.userAuthority.accounts.sa.get.utr
    preferencesConnector.getPreferences(saUtr).flatMap {
      case Some(saPreference) => Future.successful(Redirect(businessTaxHome))
      case None =>
        possibleCohort.fold(ifEmpty = Future.successful(redirectToInterstitialPageWithCohort(user))) { cohort =>
          preferencesConnector.saveCohort(saUtr, calculateCohort(user)).map { case _ =>
            auditPageShown(saUtr, Interstitial, cohort)
            displayPreferencesFormAction(None, getSavePrefsFromInterstitialCall, cohort = cohort)
          }
        }
    }
  }

  private[prefs] def displayPrefsFormAction(emailAddress: Option[Encrypted[EmailAddress]], possibleCohort: Option[Cohort])(implicit user: User, request: Request[AnyRef]) = {
    val saUtr = user.userAuthority.accounts.sa.get.utr
    possibleCohort.fold(ifEmpty = Future.successful(redirectToPrefsFormWithCohort(emailAddress, user))) { cohort =>
      preferencesConnector.saveCohort(saUtr, calculateCohort(user)).map { case _ =>
        auditPageShown(saUtr, AccountDetails, cohort)
        displayPreferencesFormAction(emailAddress.map(_.decryptedValue), getSavePrefsFromNonInterstitialPageCall, withBanner = true, cohort)
      }
    }
  }

  private[prefs] def submitPrefsFormAction(journey: Journey)(implicit user: User, request: Request[AnyRef], withBanner: Boolean = false) = {
    val cohort = calculateCohort(user)
    def saveAndAuditPreferences(utr:SaUtr, digital: Boolean, email: Option[String], acceptedTAndCs:Boolean, hc: HeaderCarrier):Future[Result] = {
      implicit val headerCarrier = hc
      for {
        _ <- preferencesConnector.saveCohort(utr, calculate(utr.hashCode))
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
      saUtr = user.userAuthority.accounts.sa.get.utr,
      savePreferences = saveAndAuditPreferences
    )
  }

  def getSavePrefFormAction(implicit withBanner: Boolean) = {
    if (withBanner)
      getSavePrefsFromNonInterstitialPageCall
    else
      getSavePrefsFromInterstitialCall
  }

  private def auditPageShown(utr: SaUtr, journey: Journey, cohort: Cohort)(implicit request: Request[_], hc: HeaderCarrier) =
    auditConnector.sendEvent(ExtendedDataEvent(
      auditSource = appName,
      auditType = EventTypes.Succeeded,
      tags = hc.toAuditTags("Show Print Preference Option", request.path),
      detail = Json.toJson(hc.toAuditDetails(
        "utr" -> utr.toString,
        "journey" -> journey.toString,
        "cohort" -> cohort.toString))))

  private def auditChoice(utr: SaUtr, journey: Journey, cohort: Cohort, digital: Boolean, emailOption: Option[String], acceptedTAndCs:Boolean)(implicit request: Request[_], hc: HeaderCarrier) =
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

  private def redirectToInterstitialPageWithCohort(user: User) =
    Redirect(routes.BizTaxPrefsController.displayInterstitialPrefsFormForCohort(Some(calculateCohort(user))))

  private def redirectToPrefsFormWithCohort(emailAddress: Option[Encrypted[EmailAddress]], user: User) =
    Redirect(routes.BizTaxPrefsController.displayPrefsFormForCohort(Some(calculateCohort(user)), emailAddress))
}

