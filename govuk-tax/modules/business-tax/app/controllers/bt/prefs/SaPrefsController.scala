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
import controllers.bt.{EmailControllerHelper, EmailPreferenceData, BusinessTaxRegimeRoots}
import scala.concurrent.Future

class SaPrefsController(override val auditConnector: AuditConnector, preferencesConnector: PreferencesConnector, emailConnector: EmailConnector)
                       (implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with BusinessTaxRegimeRoots
  with EmailControllerHelper {

  def this() = this(Connectors.auditConnector, Connectors.preferencesConnector, Connectors.emailConnector)(Connectors.authConnector)

  def displayPrefsOnLoginForm = AuthorisedFor(SaRegime).async {
    user =>
      request =>
        displayPrefsOnLoginFormAction(user, request)
  }

  def displayPrefsForm(emailAddress: Option[String])() = AuthorisedFor(SaRegime).async {
    user =>
      request =>
        displayPrefsFormAction(emailAddress)(user, request)
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

  private[prefs] def displayPrefsOnLoginFormAction(implicit user: User, request: Request[AnyRef]) = {
    preferencesConnector.getPreferences(user.getSa.utr)(HeaderCarrier(request)).map {
      case Some(saPreference) => FrontEndRedirect.toBusinessTax
      case _ => Ok(views.html.sa_printing_preference(emailForm.fill(EmailPreferenceData(("", None), None))))
    }
  }

  private[prefs] def displayPrefsFormAction(emailAddress: Option[String])(implicit user: User, request: Request[AnyRef]) = {
    Future.successful(Ok(views.html.sa_printing_preference(emailForm.fill(EmailPreferenceData((emailAddress.getOrElse(""), emailAddress), None)))))
  }

  private[prefs] def submitPrefsFormAction(implicit user: User, request: Request[AnyRef]) =
    submitPreferencesForm(
      errorsView = views.html.sa_printing_preference(_),
      emailWarningView = views.html.sa_printing_preference_verify_email(_),
      successRedirect = routes.SaPrefsController.thankYou,
      emailConnector,
      preferencesConnector
    )

  private[prefs] def submitKeepPaperFormAction(implicit user: User, request: Request[AnyRef]): Future[SimpleResult] = {
    preferencesConnector.savePreferences(user.getSa.utr, false, None)(HeaderCarrier(request)).map(
      _ => Redirect(FrontEndRedirect.businessTaxHome)
    )
  }
}
