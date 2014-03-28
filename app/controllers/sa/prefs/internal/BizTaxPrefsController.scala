package controllers.sa.prefs.internal

import play.api.mvc._
import uk.gov.hmrc.common.microservice.preferences.PreferencesConnector
import controllers.common.{NoRegimeRoots, FrontEndRedirect, BaseController}
import controllers.common.actions.{HeaderCarrier, Actions}
import controllers.common.service.Connectors
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.sa.domain.SaRegime
import uk.gov.hmrc.common.microservice.email.EmailConnector
import scala.concurrent.Future
import uk.gov.hmrc.domain.Email
import controllers.sa.prefs._
import uk.gov.hmrc.common.crypto.Encrypted
import scala.Some
import uk.gov.hmrc.common.microservice.domain.User
import ExternalUrls.businessTaxHome

class BizTaxPrefsController(override val auditConnector: AuditConnector, preferencesConnector: PreferencesConnector, emailConnector: EmailConnector)
                           (implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with NoRegimeRoots
  with PreferencesControllerHelper {

  def this() = this(Connectors.auditConnector, Connectors.preferencesConnector, Connectors.emailConnector)(Connectors.authConnector)

  def redirectToBizTaxOrEmailPrefEntryIfNotSet = AuthorisedFor(SaRegime).async {
    user =>
      request =>
        redirectToBizTaxOrEmailPrefEntryIfNotSetAction(user, request)
  }

  def displayPrefsForm(emailAddress: Option[Encrypted[Email]])(): Action[AnyContent] = AuthorisedFor(SaRegime).async {
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
    preferencesConnector.getPreferences(user.getSaUtr)(HeaderCarrier(request)).map {
      case Some(saPreference) => FrontEndRedirect.toBusinessTax
      case _ => displayPreferencesFormAction(None, getSavePrefsFromInterstitialCall)
    }

  private[prefs] def displayPrefsFormAction(emailAddress: Option[Encrypted[Email]])(implicit user: User, request: Request[AnyRef], withBanner: Boolean = false) =
    Future.successful(displayPreferencesFormAction(emailAddress.map(_.decryptedValue), getSavePrefsFromNonInterstitialPageCall))

  private[prefs] def submitPrefsFormAction(implicit user: User, request: Request[AnyRef]) = {
    submitPreferencesForm(
      errorsView = getSubmitPreferencesView(getSavePrefFormAction),
      emailWarningView = views.html.sa_printing_preference_verify_email(_),
      emailConnector = emailConnector,
      saUtr = user.getSaUtr,
      savePreferences = (utr, digital, email, hc) =>
        preferencesConnector.savePreferences(utr, digital, email)(hc).map(_ =>
          digital match {
            case true => Redirect(routes.BizTaxPrefsController.thankYou())
            case false => Redirect(FrontEndRedirect.businessTaxHome)
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
