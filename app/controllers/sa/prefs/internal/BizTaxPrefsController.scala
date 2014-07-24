package controllers.sa.prefs.internal

import play.api.mvc._
import controllers.common.{FrontEndRedirect, BaseController}
import controllers.common.actions.Actions
import controllers.common.service.Connectors
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import controllers.sa.prefs.SaRegime
import connectors.EmailConnector
import uk.gov.hmrc.emailaddress.EmailAddress
import scala.concurrent.Future
import controllers.sa.prefs._
import uk.gov.hmrc.common.crypto.Encrypted
import uk.gov.hmrc.common.microservice.domain.User
import ExternalUrls.businessTaxHome
import uk.gov.hmrc.play.connectors.HeaderCarrier
import connectors.PreferencesConnector

class BizTaxPrefsController(val auditConnector: AuditConnector, preferencesConnector: PreferencesConnector, emailConnector: EmailConnector)
                           (implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with PreferencesControllerHelper {

  def this() = this(Connectors.auditConnector, PreferencesConnector, EmailConnector)(Connectors.authConnector)

  def redirectToBizTaxOrEmailPrefEntryIfNotSet = AuthorisedFor(SaRegime).async {
    user =>
      request =>
        redirectToBizTaxOrEmailPrefEntryIfNotSetAction(user, request)
  }

  def displayPrefsForm(emailAddress: Option[Encrypted[EmailAddress]])(): Action[AnyContent] = AuthorisedFor(SaRegime).async {
    implicit user =>
      implicit request =>
        displayPrefsFormAction(emailAddress)
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
        Ok(views.html.sa.prefs.sa_printing_preference_confirm(Some(user), businessTaxHome))
  }

  val getSavePrefsFromInterstitialCall = controllers.sa.prefs.internal.routes.BizTaxPrefsController.submitPrefsFormForInterstitial()
  val getSavePrefsFromNonInterstitialPageCall = controllers.sa.prefs.internal.routes.BizTaxPrefsController.submitPrefsFormForNonInterstitial()

  private[prefs] def redirectToBizTaxOrEmailPrefEntryIfNotSetAction(implicit user: User, request: Request[AnyRef]) =
    preferencesConnector.getPreferences(user.userAuthority.accounts.sa.get.utr)(HeaderCarrier.fromSessionAndHeaders(request.session, request.headers)).map {
      case Some(saPreference) => FrontEndRedirect.toBusinessTax
      case _ => displayPreferencesFormAction(None, getSavePrefsFromInterstitialCall)
    }

  private[prefs] def displayPrefsFormAction(emailAddress: Option[Encrypted[EmailAddress]])(implicit user: User, request: Request[AnyRef]) =
    Future.successful(displayPreferencesFormAction(emailAddress.map(_.decryptedValue), getSavePrefsFromNonInterstitialPageCall , withBanner =true))

  private[prefs] def submitPrefsFormAction(implicit user: User, request: Request[AnyRef], withBanner: Boolean = false) = {
    submitPreferencesForm(
      errorsView = getSubmitPreferencesView(getSavePrefFormAction),
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
