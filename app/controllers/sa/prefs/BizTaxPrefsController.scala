package controllers.sa.prefs

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
import scala.Some
import play.api.mvc.SimpleResult
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.common.crypto.Encrypted

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
    user =>
      request =>
        displayPrefsFormAction(emailAddress)(user, request)
  }

  def submitPrefsForm() = AuthorisedFor(SaRegime).async {
    user =>
      request =>
        submitPrefsFormAction(user, request)
  }

  def thankYou() = AuthorisedFor(SaRegime) {
    user =>
      request =>
        // FIXME remove the hard-coded URL - maybe take this as a return-url when entering the pref-setting?
        Ok(views.html.sa.prefs.sa_printing_preference_confirm(Some(user), businessTaxHome))
  }

  private[prefs] def redirectToBizTaxOrEmailPrefEntryIfNotSetAction(implicit user: User, request: Request[AnyRef]) = {
    preferencesConnector.getPreferences(user.getSaUtr)(HeaderCarrier(request)).map {
      case Some(saPreference) => FrontEndRedirect.toBusinessTax
      case _ => displayPreferencesForm(None, getSavePrefsCall, getKeepPaperCall)
    }
  }

  private[prefs] def displayPrefsFormAction(emailAddress: Option[Encrypted[Email]])(implicit user: User, request: Request[AnyRef]) = {
    Future.successful(Ok(views.html.sa.prefs.sa_printing_preference(emailForm.fill(EmailPreferenceData(emailAddress.map(_.decryptedValue))), getSavePrefsCall, getKeepPaperCall)))
  }

  private[prefs] def submitPrefsFormAction(implicit user: User, request: Request[AnyRef]) = {

    def savePreferences(utr: SaUtr, digital: Boolean, email: Option[String] = None, hc: HeaderCarrier) = {
      preferencesConnector.savePreferences(utr, digital, email)(hc)
    }

    submitPreferencesForm(
      errorsView = getSubmitPreferencesView(getSavePrefsCall, getKeepPaperCall),
      emailWarningView = views.html.sa_printing_preference_verify_email(_),
      successRedirect = routes.BizTaxPrefsController.thankYou,
      emailConnector = emailConnector,
      saUtr = user.getSaUtr,
      savePreferences = savePreferences
    )
  }

  private[prefs] def submitKeepPaperFormAction(implicit user: User, request: Request[AnyRef]): Future[SimpleResult] = {
    preferencesConnector.savePreferences(user.getSaUtr, false, None)(HeaderCarrier(request)).map(
      _ => Redirect(FrontEndRedirect.businessTaxHome)
    )
  }
}
