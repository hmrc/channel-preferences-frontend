package controllers.sa.prefs.internal

import controllers.sa.prefs.internal.InterstitialPageContentCohorts.Cohort
import play.api.mvc._
import controllers.common.{FrontEndRedirect, BaseController}
import controllers.common.actions.Actions
import controllers.common.service.Connectors
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import controllers.sa.prefs.SaRegime
import connectors.EmailConnector
import uk.gov.hmrc.crypto.Encrypted
import uk.gov.hmrc.emailaddress.EmailAddress
import scala.concurrent.Future
import controllers.sa.prefs._
import uk.gov.hmrc.common.microservice.domain.User
import ExternalUrls.businessTaxHome
import uk.gov.hmrc.play.connectors.HeaderCarrier
import connectors.PreferencesConnector

class BizTaxPrefsController(val auditConnector: AuditConnector, preferencesConnector: PreferencesConnector, emailConnector: EmailConnector)
                           (implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with PreferencesControllerHelper
  with InterstitialPageContentCohortCalculator {

  def this() = this(Connectors.auditConnector, PreferencesConnector, EmailConnector)(Connectors.authConnector)

  def redirectToBTAOrInterstitialPage = AuthorisedFor(SaRegime).async {
    user =>
      request =>
        redirectToBTAOrInterstitialPageAction(user, request)
  }

  def displayPrefsForm(emailAddress: Option[Encrypted[EmailAddress]]) = AuthorisedFor(SaRegime).async {
    implicit user =>
      implicit request =>
        displayPrefsFormAction(emailAddress, calculateCohort(user))
  }

  def displayInterstitialPrefsForm(cohort: Cohort) = AuthorisedFor(SaRegime).async {
    implicit user =>
      implicit request =>
        displayInterstitialPrefsFormAction(user, request, cohort)
  }

  def submitPrefsFormForInterstitial() = AuthorisedFor(SaRegime).async {
    implicit user => implicit request => submitPrefsFormAction
  }

  def submitPrefsFormForNonInterstitial() = AuthorisedFor(SaRegime).async {
    user =>
      request =>
        submitPrefsFormAction(user, request, withBanner = true)
  }

  def thankYou() = AuthorisedFor(SaRegime) {
    user =>
      request =>
        Ok(views.html.account_details_printing_preference_confirm(Some(user), businessTaxHome))
  }

  val getSavePrefsFromInterstitialCall = controllers.sa.prefs.internal.routes.BizTaxPrefsController.submitPrefsFormForInterstitial()
  val getSavePrefsFromNonInterstitialPageCall = controllers.sa.prefs.internal.routes.BizTaxPrefsController.submitPrefsFormForNonInterstitial()


  private[prefs] def redirectToBTAOrInterstitialPageAction(implicit user: User, request: Request[AnyRef]) =
    preferencesConnector.getPreferences(user.userAuthority.accounts.sa.get.utr)(HeaderCarrier.fromSessionAndHeaders(request.session, request.headers)).map {
      case Some(saPreference) => FrontEndRedirect.toBusinessTax
      case None => Redirect(routes.BizTaxPrefsController.displayInterstitialPrefsForm(calculateCohort(user)))
    }

  private[prefs] def displayInterstitialPrefsFormAction(implicit user: User, request: Request[AnyRef], cohort: Cohort) = {
    preferencesConnector.getPreferences(user.userAuthority.accounts.sa.get.utr)(HeaderCarrier.fromSessionAndHeaders(request.session, request.headers)).map {
      case Some(saPreference) =>  FrontEndRedirect.toBusinessTax
      case None => displayPreferencesFormAction(None, getSavePrefsFromInterstitialCall, cohort = cohort)
    }
  }

  private[prefs] def displayPrefsFormAction(emailAddress: Option[Encrypted[EmailAddress]], cohort: Cohort)(implicit user: User, request: Request[AnyRef]) =
    Future.successful(displayPreferencesFormAction(emailAddress.map(_.decryptedValue), getSavePrefsFromNonInterstitialPageCall , withBanner = true, cohort))

  private[prefs] def submitPrefsFormAction(implicit user: User, request: Request[AnyRef], withBanner: Boolean = false) = {
    submitPreferencesForm(
      errorsView = getSubmitPreferencesView(getSavePrefFormAction, cohort = calculateCohort(user)),
      emailWarningView = views.html.sa_printing_preference_verify_email(_),
      emailConnector = emailConnector,
      saUtr = user.userAuthority.accounts.sa.get.utr,
      savePreferences = (utr, digital, email, hc) =>
        preferencesConnector.savePreferences(utr, digital, email)(hc).map(_ =>
          digital match {
            case true => Redirect(routes.BizTaxPrefsController.thankYou())
            case false => Redirect(ExternalUrls.businessTaxHome)
          }
        )(mdcExecutionContext(hc))
    )
  }

  def getSavePrefFormAction(implicit withBanner: Boolean) = {
    if (withBanner)
      getSavePrefsFromNonInterstitialPageCall
    else
      getSavePrefsFromInterstitialCall
  }
}

