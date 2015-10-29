package controllers.sa.prefs.internal

import authentication.ValidSessionCredentialsProvider
import connectors.{EmailConnector, PreferencesConnector, SaPreference}
import controllers.sa.prefs.AuthContextAvailability._
import controllers.sa.prefs.config.Global
import controllers.sa.prefs.{EmailFormData, Encrypted, SaRegime}
import hostcontext.HostContext
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.{Actions, AuthContext}
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

object ManagePaperlessController extends ManagePaperlessController with ServicesConfig {
  lazy val auditConnector = Global.auditConnector

  val authConnector = Global.authConnector

  lazy val emailConnector = EmailConnector
  lazy val preferencesConnector = PreferencesConnector

  def displayChangeEmailAddress(implicit emailAddress: Option[Encrypted[EmailAddress]], hostContext: HostContext) = AuthorisedFor(SaRegime).async { implicit authContext => implicit request =>
    _displayChangeEmailAddress(emailAddress)
  }

  def submitChangeEmailAddress(implicit hostContext: HostContext) = AuthorisedFor(SaRegime).async { implicit authContext => implicit request =>
    _submitChangeEmailAddress
  }

  def displayChangeEmailAddressConfirmed(implicit hostContext: HostContext) = AuthorisedFor(SaRegime).async { implicit authContext => implicit request =>
    _displayChangeEmailAddressConfirmed
  }

  def displayStopPaperless(implicit hostContext: HostContext) = AuthorisedFor(SaRegime).async { implicit authContext => implicit request =>
    _displayStopPaperless
  }

  def submitStopPaperless(implicit hostContext: HostContext) = AuthorisedFor(SaRegime).async { implicit authContext => implicit request =>
    _submitStopPaperless
  }

  def displayStopPaperlessConfirmed(implicit hostContext: HostContext) = AuthenticatedBy(ValidSessionCredentialsProvider, redirectToOrigin = true) { implicit authContext => implicit request =>
    _displayStopPaperlessConfirmed
  }

  def resendVerificationEmail(implicit hostContext: HostContext) = AuthorisedFor(SaRegime).async { implicit authContext => implicit request =>
    _resendVerificationEmail
  }
}

object DeprecatedYTAManagePaperlessController extends ManagePaperlessController with ServicesConfig {
  lazy val auditConnector = Global.auditConnector

  val authConnector = Global.authConnector

  lazy val emailConnector = EmailConnector
  lazy val preferencesConnector = PreferencesConnector

  implicit val hostContext = HostContext.defaultsForYtaManageAccount

  def displayChangeEmailAddress(emailAddress: Option[Encrypted[EmailAddress]]) = AuthorisedFor(SaRegime).async { implicit authContext => implicit request =>
    _displayChangeEmailAddress(emailAddress)
  }

  def submitChangeEmailAddress = AuthorisedFor(SaRegime).async { implicit authContext => implicit request =>
    _submitChangeEmailAddress
  }

  def displayChangeEmailAddressConfirmed() = AuthorisedFor(SaRegime).async { implicit authContext => implicit request =>
    _displayChangeEmailAddressConfirmed
  }

  def displayStopPaperless = AuthorisedFor(SaRegime).async { implicit authContext => implicit request =>
    _displayStopPaperless
  }

  def submitStopPaperless = AuthorisedFor(SaRegime).async { implicit authContext => implicit request =>
    _submitStopPaperless
  }

  def displayStopPaperlessConfirmed() = AuthenticatedBy(ValidSessionCredentialsProvider, redirectToOrigin = true) { implicit authContext => implicit request =>
    _displayStopPaperlessConfirmed
  }

  def resendVerificationEmail = AuthorisedFor(SaRegime).async { implicit authContext => implicit request =>
    _resendVerificationEmail
  }
}

trait ManagePaperlessController
extends FrontendController
with Actions
with PreferencesControllerHelper {

  val auditConnector: AuditConnector
  val authConnector: AuthConnector
  val emailConnector: EmailConnector
  val preferencesConnector: PreferencesConnector

  private[prefs] def _displayStopPaperlessConfirmed(implicit authContext: AuthContext, request: Request[AnyRef], hostContext: HostContext): Result = {
    Ok(views.html.opted_back_into_paper_thank_you())
  }

  private[prefs] def _submitStopPaperless(implicit authContext: AuthContext, request: Request[AnyRef], hostContext: HostContext): Future[Result] = {
    preferencesConnector.savePreferences(authContext.principal.accounts.sa.get.utr, false, None).map(_ =>
      Redirect(routes.ManagePaperlessController.displayStopPaperlessConfirmed(hostContext))
    )
  }

  private[prefs] def _resendVerificationEmail(implicit authContext: AuthContext, request: Request[AnyRef], hostContext: HostContext): Future[Result] = {
    lookupCurrentEmail { email =>
      preferencesConnector.savePreferences(authContext.principal.accounts.sa.get.utr, true, Some(email)).map(_ =>
        Ok(views.html.account_details_verification_email_resent_confirmation(email))
      )
    }
  }

  private[prefs] def _displayStopPaperless(implicit authContext: AuthContext, request: Request[AnyRef], hostContext: hostcontext.HostContext) =
    lookupCurrentEmail(email => Future.successful(Ok(views.html.confirm_opt_back_into_paper(email.obfuscated))))

  private[prefs] def _displayChangeEmailAddress(emailAddress: Option[Encrypted[EmailAddress]])(implicit authContext: AuthContext, request: Request[AnyRef], hostContext: HostContext): Future[Result] =
    lookupCurrentEmail(email => Future.successful(Ok(views.html.account_details_update_email_address(email, emailForm.fill(EmailFormData(emailAddress.map(_.decryptedValue)))))))

  private def lookupCurrentEmail(func: (EmailAddress) => Future[Result])(implicit authContext: AuthContext, request: Request[AnyRef]): Future[Result] = {
    preferencesConnector.getPreferences(authContext.principal.accounts.sa.get.utr, None)(HeaderCarrier.fromSessionAndHeaders(request.session, request.headers)).flatMap {
        case Some(SaPreference(true, Some(email))) => func(EmailAddress(email.email))
        case _ => Future.successful(BadRequest("Could not find existing preferences."))
    }
  }

  private[prefs] def _submitChangeEmailAddress(implicit authContext: AuthContext, request: Request[AnyRef], hostContext: HostContext): Future[Result] =
    lookupCurrentEmail(
      email =>
        submitEmailForm(
          errorsView = views.html.account_details_update_email_address(email, _),
          emailWarningView = (enteredEmail) => views.html.account_details_update_email_address_verify_email(enteredEmail),
          successRedirect = routes.ManagePaperlessController.displayChangeEmailAddressConfirmed(hostContext),
          emailConnector = emailConnector,
          saUtr = authContext.principal.accounts.sa.get.utr,
          savePreferences = savePreferences
        )
    )

  private def savePreferences(utr: SaUtr, digital: Boolean, email: Option[String] = None, hc: HeaderCarrier) =
    preferencesConnector.savePreferences(utr, digital, email)(hc)

  private[prefs] def _displayChangeEmailAddressConfirmed(implicit authContext: AuthContext, request: Request[AnyRef], hostContext: HostContext): Future[Result] = {
    lookupCurrentEmail(email => Future.successful(Ok(views.html.account_details_update_email_address_thank_you(email.obfuscated))))
  }
}

