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
        Ok(views.html.sa.prefs.sa_printing_preference_confirm(Some(user), businessTaxHome))
  }

  val getSavePrefsCall = controllers.sa.prefs.internal.routes.BizTaxPrefsController.submitPrefsForm()

  private[prefs] def redirectToBizTaxOrEmailPrefEntryIfNotSetAction(implicit user: User, request: Request[AnyRef]) = {
    preferencesConnector.getPreferences(user.getSaUtr)(HeaderCarrier(request)).map {
      case Some(saPreference) => FrontEndRedirect.toBusinessTax
      case _ => displayPreferencesForm(None, getSavePrefsCall)
    }
  }

  private[prefs] def displayPrefsFormAction(emailAddress: Option[Encrypted[Email]])(implicit user: User, request: Request[AnyRef]) = {
    Future.successful(Ok(views.html.sa.prefs.sa_printing_preference(emailForm.fill(EmailFormData(emailAddress.map(_.decryptedValue))), getSavePrefsCall)))
  }

  private[prefs] def submitPrefsFormAction(implicit user: User, request: Request[AnyRef]) = {
    submitPreferencesForm(
      errorsView = getSubmitPreferencesView(getSavePrefsCall),
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
}
