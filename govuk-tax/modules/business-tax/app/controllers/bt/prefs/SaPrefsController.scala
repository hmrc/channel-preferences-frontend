package controllers.bt.prefs

import play.api.mvc.{SimpleResult, Request}
import uk.gov.hmrc.common.microservice.preferences.PreferencesConnector
import controllers.common.{SessionKeys, FrontEndRedirect, BaseController}
import controllers.common.actions.{HeaderCarrier, Actions}
import controllers.common.service.Connectors
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.sa.domain.SaRegime
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.email.EmailConnector
import controllers.bt.{EmailControllerHelper, EmailPreferenceData, BusinessTaxRegimeRoots}
import scala.concurrent.Future

class SaPrefsController(override val auditConnector: AuditConnector, preferencesConnector: PreferencesConnector, emailConnector: EmailConnector)
                       (implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with BusinessTaxRegimeRoots
  with EmailControllerHelper {

  def this() = this(Connectors.auditConnector, Connectors.preferencesConnector, Connectors.emailConnector)(Connectors.authConnector)

  def displayPrefsOnLoginForm(emailAddress: Option[String]) = AuthorisedFor(account = SaRegime).async {
    user => request =>
      displayPrefsOnLoginFormAction(emailAddress)(user, request)
  }

  // FIXME - Need to capture emailAddress from session
  def displayPrefsForm() = AuthorisedFor(account = SaRegime).async {
    user => request =>
      displayPrefsFormAction(user, request)
  }

  def submitPrefsForm() = AuthorisedFor(account = SaRegime).async {
    user => request =>
      submitPrefsFormAction(user, request)
  }

  def submitKeepPaperForm() = AuthorisedFor(account = SaRegime).async {
    user => request =>
      submitKeepPaperFormAction(user, request)
  }

  def thankYou() = AuthorisedFor(account = SaRegime) {
    user => request =>
      Ok(views.html.sa_printing_preference_thank_you(user))
  }

  private[prefs] def displayPrefsOnLoginFormAction(emailAddress: Option[String])(implicit user: User, request: Request[AnyRef]) = {
    preferencesConnector.getPreferences(user.getSa.utr)(HeaderCarrier(request)).map {
      case Some(saPreference) => FrontEndRedirect.toBusinessTax
      case _ => Ok(views.html.sa_printing_preference(emailForm.fill(EmailPreferenceData((emailAddress.getOrElse(""), emailAddress), None))))
    }
  }

  private[prefs] def displayPrefsFormAction(implicit user: User, request: Request[AnyRef]) = {
    val emailAddress = request.session.get(SessionKeys.unconfirmedEmailAddress)
    Future.successful(Ok(views.html.sa_printing_preference(emailForm.fill(EmailPreferenceData((emailAddress.getOrElse(""), emailAddress), None)))))
  }

  private[prefs] def submitPrefsFormAction(implicit user: User, request: Request[AnyRef]) = {
    submitPreferencesForm((errors) => views.html.sa_printing_preference(errors), (email) => views.html.sa_printing_preference_verify_email(email), () => routes.SaPrefsController.thankYou(),
    emailConnector, preferencesConnector)
  }

  private[prefs] def submitKeepPaperFormAction(implicit user: User, request: Request[AnyRef]): Future[SimpleResult] = {
    preferencesConnector.savePreferences(user.getSa.utr, false, None)(HeaderCarrier(request)).map(
      _ => Redirect(FrontEndRedirect.businessTaxHome)
    )
  }
}
