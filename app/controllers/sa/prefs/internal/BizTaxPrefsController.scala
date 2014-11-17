package controllers.sa.prefs.internal

import connectors.{EmailConnector, PreferencesConnector}
import controllers.common.actions.Actions
import controllers.common.service.Connectors
import controllers.common.{BaseController, FrontEndRedirect}
import controllers.sa.Encrypted
import controllers.sa.prefs.ExternalUrls.businessTaxHome
import controllers.sa.prefs._
import controllers.sa.prefs.internal.EmailOptInJourney._
import play.api.mvc._
import uk.gov.hmrc.common.microservice.audit.{AuditConnector, AuditEvent}
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.connectors.HeaderCarrier
import uk.gov.hmrc.play.monitoring.EventTypes

import scala.concurrent.Future

class BizTaxPrefsController(val auditConnector: AuditConnector,
                            preferencesConnector: PreferencesConnector,
                            emailConnector: EmailConnector)
                           (implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with PreferencesControllerHelper
  with CohortCalculator[OptInCohort]
  with AppName {

  override val values = OptInCohort.values

  def this() = this(Connectors.auditConnector, PreferencesConnector, EmailConnector)(Connectors.authConnector)

  def redirectToBTAOrInterstitialPage = AuthorisedFor(SaRegime).async {
    implicit user => implicit request => redirectToBTAOrInterstitialPageAction(user, request)
  }

  def displayPrefsForm(emailAddress: Option[Encrypted[EmailAddress]]) = AuthorisedFor(SaRegime) {
    implicit user => implicit request => Redirect(routes.BizTaxPrefsController.displayPrefsFormForCohort(calculateCohort(user), emailAddress))
  }

  def displayPrefsFormForCohort(cohort: Cohort, emailAddress: Option[Encrypted[EmailAddress]]) = AuthorisedFor(SaRegime).async {
    implicit user => implicit request => displayPrefsFormAction(emailAddress, cohort)
  }

  def displayInterstitialPrefsFormForCohort(cohort: Cohort) = AuthorisedFor(SaRegime).async {
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

  val getSavePrefsFromInterstitialCall = controllers.sa.prefs.internal.routes.BizTaxPrefsController.submitPrefsFormForInterstitial()
  val getSavePrefsFromNonInterstitialPageCall = controllers.sa.prefs.internal.routes.BizTaxPrefsController.submitPrefsFormForNonInterstitial()


  private[prefs] def redirectToBTAOrInterstitialPageAction(implicit user: User, request: Request[AnyRef]) =
    preferencesConnector.getPreferences(user.userAuthority.accounts.sa.get.utr)(HeaderCarrier.fromSessionAndHeaders(request.session, request.headers)).map {
      case Some(saPreference) => FrontEndRedirect.toBusinessTax
      case None => Redirect(routes.BizTaxPrefsController.displayInterstitialPrefsFormForCohort(calculateCohort(user)))
    }

  private[prefs] def displayInterstitialPrefsFormAction(implicit user: User, request: Request[AnyRef], cohort: Cohort) = {
    implicit val hc = HeaderCarrier.fromSessionAndHeaders(request.session, request.headers)
    val saUtr = user.userAuthority.accounts.sa.get.utr
    preferencesConnector.getPreferences(saUtr).flatMap {
      case Some(saPreference) =>  Future.successful(FrontEndRedirect.toBusinessTax)
      case None =>
        preferencesConnector.saveCohort(saUtr, calculateCohort(user)).map { case _ =>
          auditPageShown(saUtr, Interstitial, cohort)
          displayPreferencesFormAction(None, getSavePrefsFromInterstitialCall, cohort = cohort)
        }
    }
  }

  private[prefs] def displayPrefsFormAction(emailAddress: Option[Encrypted[EmailAddress]], cohort: Cohort)(implicit user: User, request: Request[AnyRef]) = {
    val saUtr = user.userAuthority.accounts.sa.get.utr
    preferencesConnector.saveCohort(saUtr, calculateCohort(user)).map { case _ =>
      auditPageShown(saUtr, AccountDetails, cohort)
      displayPreferencesFormAction(emailAddress.map(_.decryptedValue), getSavePrefsFromNonInterstitialPageCall, withBanner = true, cohort)
    }
  }

  private[prefs] def submitPrefsFormAction(journey: Journey)(implicit user: User, request: Request[AnyRef], withBanner: Boolean = false) = {
    val cohort = calculateCohort(user)
    submitPreferencesForm(
      errorsView = getSubmitPreferencesView(getSavePrefFormAction, cohort),
      emailWarningView = views.html.sa_printing_preference_verify_email(_, cohort),
      emailConnector = emailConnector,
      saUtr = user.userAuthority.accounts.sa.get.utr,
      savePreferences = (utr, digital, email, hc) => {
        implicit val headerCarrier = hc
        for {
          _ <- preferencesConnector.saveCohort(utr, calculate(utr.hashCode))(hc)
          _ <- preferencesConnector.savePreferences(utr, digital, email)(hc)
        } yield {
          auditChoice(utr, journey, cohort, digital, email)(request, hc)
          digital match {
            case true =>
              Redirect(routes.BizTaxPrefsController.thankYou(email map (emailAddress => Encrypted(EmailAddress(emailAddress)))))
            case false => Redirect(ExternalUrls.businessTaxHome)
          }
        }
      }
    )
  }

  def getSavePrefFormAction(implicit withBanner: Boolean) = {
    if (withBanner)
      getSavePrefsFromNonInterstitialPageCall
    else
      getSavePrefsFromInterstitialCall
  }

  private def auditPageShown(utr: SaUtr, journey: Journey, cohort: Cohort)(implicit request: Request[_], hc: HeaderCarrier) = {
    auditConnector.audit(AuditEvent(
      auditSource = appName,
      auditType = EventTypes.Succeeded,
      tags = hc.toAuditTags("Show Print Preference Option", request.path),
      detail = hc.toAuditDetails(
        "utr" -> utr.toString,
        "journey" -> journey.toString,
        "cohort" -> cohort.toString)))(hc)
  }

  private def auditChoice(utr: SaUtr, journey: Journey, cohort: Cohort, digital: Boolean, emailOption: Option[String])(implicit request: Request[_], hc: HeaderCarrier) = {
    auditConnector.audit(AuditEvent(
      auditSource = appName,
      auditType = EventTypes.Succeeded,
      tags = hc.toAuditTags("Set Print Preference", request.path),
      detail = hc.toAuditDetails(
        "utr" -> utr.toString,
        "journey" -> journey.toString,
        "digital" -> digital.toString,
        "cohort" -> cohort.toString,
        "email" -> emailOption.getOrElse(""))))(hc)

  }

}

