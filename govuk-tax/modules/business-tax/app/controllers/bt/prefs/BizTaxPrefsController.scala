package controllers.bt.prefs

import play.api.mvc.{SimpleResult, Request}
import uk.gov.hmrc.common.microservice.preferences.PreferencesConnector
import controllers.common.{FrontEndRedirect, BaseController}
import controllers.common.actions.{HeaderCarrier, Actions}
import controllers.common.service.Connectors
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.sa.domain.SaRegime
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.email.EmailConnector
import scala.concurrent.Future
import uk.gov.hmrc.domain.{SaUtr, Email}
import controllers.common.domain.EmailPreferenceData
import controllers.common.preferences.PreferencesControllerHelper
import controllers.bt._

class BizTaxPrefsController(override val auditConnector: AuditConnector, preferencesConnector: PreferencesConnector, emailConnector: EmailConnector)
                       (implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with BusinessTaxRegimeRoots
  with PreferencesControllerHelper {

  def this() = this(Connectors.auditConnector, Connectors.preferencesConnector, Connectors.emailConnector)(Connectors.authConnector)

  def redirectToBizTaxOrEmailPrefEntryIfNotSet = AuthorisedFor(SaRegime).async {
    user =>
      request =>
        redirectToBizTaxOrEmailPrefEntryIfNotSetAction(user, request)
  }

  def displayPrefsForm(emailAddress: Option[Email])() = AuthorisedFor(SaRegime).async {
    user =>
      request =>
        displayPrefsFormAction(emailAddress.map(_.value))(user, request)
  }

  def submitPrefsForm() = AuthorisedFor(SaRegime).async {
    user =>
      request =>
        submitPrefsFormAction(user, request)
  }

  def submitKeepPaperForm() = AuthorisedFor(SaRegime).async {
    user =>
      request =>
        submitKeepPaperFormAction(user, request)
  }

  def thankYou() = AuthorisedFor(SaRegime) {
    user =>
      request =>
        Ok(views.html.sa_printing_preference_thank_you(user))
  }

  private[prefs] def redirectToBizTaxOrEmailPrefEntryIfNotSetAction(implicit user: User, request: Request[AnyRef]) = {
    preferencesConnector.getPreferences(user.getSaUtr)(HeaderCarrier(request)).map {
      case Some(saPreference) => FrontEndRedirect.toBusinessTax
      case _ => Ok(  views.html.preferences.sa_printing_preference(emailForm.fill(EmailPreferenceData(("", None), None)), getSavePrefsCall, getKeepPaperCall))
    }
  }

  private[prefs] def displayPrefsFormAction(emailAddress: Option[Email])(implicit user: User, request: Request[AnyRef]) = {
    Future.successful(Ok(views.html.preferences.sa_printing_preference(emailForm.fill(EmailPreferenceData(emailAddress)), getSavePrefsCall, getKeepPaperCall)))
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
