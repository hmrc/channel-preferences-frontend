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

object ManagePaperlessController extends AccountDetailsController with ServicesConfig {
  lazy val auditConnector = Global.auditConnector

  val authConnector = Global.authConnector

  lazy val emailConnector = EmailConnector
  lazy val preferencesConnector = PreferencesConnector

  def changeEmailAddress(implicit emailAddress: Option[Encrypted[EmailAddress]], hostContext: HostContext) = AuthorisedFor(SaRegime).async { implicit authContext => implicit request =>
    changeEmailAddressPage(emailAddress)
  }

  def submitEmailAddress(implicit hostContext: HostContext) = AuthorisedFor(SaRegime).async { implicit authContext => implicit request =>
    submitEmailAddressPage
  }

  def emailAddressChangeThankYou(implicit hostContext: HostContext) = AuthorisedFor(SaRegime).async { implicit authContext => implicit request =>
    emailAddressChangeThankYouPage
  }

  def optOutOfEmailReminders(implicit hostContext: HostContext) = AuthorisedFor(SaRegime).async { implicit authContext => implicit request =>
    optOutOfEmailRemindersPage
  }

  def resendValidationEmail(implicit hostContext: HostContext) = AuthorisedFor(SaRegime).async { implicit authContext => implicit request =>
    resendValidationEmailAction
  }

  def optedBackIntoPaperThankYou(implicit hostContext: HostContext) = AuthenticatedBy(ValidSessionCredentialsProvider, redirectToOrigin = true) { implicit authContext => implicit request =>
    optedBackIntoPaperAction
  }

  def confirmOptOutOfEmailReminders(implicit hostContext: HostContext) = AuthorisedFor(SaRegime).async { implicit authContext => implicit request =>
    confirmOptOutOfEmailRemindersPage
  }
}

object DeprecatedYTAAccountDetailsController extends AccountDetailsController with ServicesConfig {
  lazy val auditConnector = Global.auditConnector

  val authConnector = Global.authConnector

  lazy val emailConnector = EmailConnector
  lazy val preferencesConnector = PreferencesConnector

  implicit val hostContext = HostContext.defaultsForYtaManageAccount

  def changeEmailAddress(emailAddress: Option[Encrypted[EmailAddress]]) = AuthorisedFor(SaRegime).async { implicit authContext => implicit request =>
    changeEmailAddressPage(emailAddress)
  }

  def submitEmailAddress = AuthorisedFor(SaRegime).async { implicit authContext => implicit request =>
    submitEmailAddressPage
  }

  def emailAddressChangeThankYou() = AuthorisedFor(SaRegime).async { implicit authContext => implicit request =>
    emailAddressChangeThankYouPage
  }

  def optOutOfEmailRemindersDeprecated = AuthorisedFor(SaRegime).async { implicit authContext => implicit request =>
    optOutOfEmailRemindersPage
  }

  def confirmOptOutOfEmailReminders = AuthorisedFor(SaRegime).async { implicit authContext => implicit request =>
    confirmOptOutOfEmailRemindersPage
  }

  def optedBackIntoPaperThankYou() = AuthenticatedBy(ValidSessionCredentialsProvider, redirectToOrigin = true) { implicit authContext => implicit request =>
    optedBackIntoPaperAction
  }

  def resendValidationEmailDeprecated = AuthorisedFor(SaRegime).async { implicit authContext => implicit request =>
    resendValidationEmailAction
  }
}

trait AccountDetailsController
extends FrontendController
with Actions
with PreferencesControllerHelper {

  val auditConnector: AuditConnector
  val authConnector: AuthConnector
  val emailConnector: EmailConnector
  val preferencesConnector: PreferencesConnector

  private[prefs] def optedBackIntoPaperAction(implicit authContext: AuthContext, request: Request[AnyRef], hostContext: HostContext): Result = {
    Ok(views.html.opted_back_into_paper_thank_you())
  }

  private[prefs] def confirmOptOutOfEmailRemindersPage(implicit authContext: AuthContext, request: Request[AnyRef], hostContext: HostContext): Future[Result] = {
    preferencesConnector.savePreferences(authContext.principal.accounts.sa.get.utr, false, None).map(_ =>
      Redirect(routes.ManagePaperlessController.optedBackIntoPaperThankYou(hostContext))
    )
  }

  private[prefs] def resendValidationEmailAction(implicit authContext: AuthContext, request: Request[AnyRef], hostContext: HostContext): Future[Result] = {
    lookupCurrentEmail { email =>
      preferencesConnector.savePreferences(authContext.principal.accounts.sa.get.utr, true, Some(email)).map(_ =>
        Ok(views.html.account_details_verification_email_resent_confirmation(email))
      )
    }
  }

  private[prefs] def optOutOfEmailRemindersPage(implicit authContext: AuthContext, request: Request[AnyRef], hostContext: hostcontext.HostContext) =
    lookupCurrentEmail(email => Future.successful(Ok(views.html.confirm_opt_back_into_paper(email.obfuscated))))

  private[prefs] def changeEmailAddressPage(emailAddress: Option[Encrypted[EmailAddress]])(implicit authContext: AuthContext, request: Request[AnyRef], hostContext: HostContext): Future[Result] =
    lookupCurrentEmail(email => Future.successful(Ok(views.html.account_details_update_email_address(email, emailForm.fill(EmailFormData(emailAddress.map(_.decryptedValue)))))))

  private def lookupCurrentEmail(func: (EmailAddress) => Future[Result])(implicit authContext: AuthContext, request: Request[AnyRef]): Future[Result] = {
    preferencesConnector.getPreferences(authContext.principal.accounts.sa.get.utr, None)(HeaderCarrier.fromSessionAndHeaders(request.session, request.headers)).flatMap {
        case Some(SaPreference(true, Some(email))) => func(EmailAddress(email.email))
        case _ => Future.successful(BadRequest("Could not find existing preferences."))
    }
  }

  private[prefs] def submitEmailAddressPage(implicit authContext: AuthContext, request: Request[AnyRef], hostContext: HostContext): Future[Result] =
    lookupCurrentEmail(
      email =>
        submitEmailForm(
          errorsView = views.html.account_details_update_email_address(email, _),
          emailWarningView = (enteredEmail) => views.html.account_details_update_email_address_verify_email(enteredEmail),
          successRedirect = routes.ManagePaperlessController.emailAddressChangeThankYou(hostContext),
          emailConnector = emailConnector,
          saUtr = authContext.principal.accounts.sa.get.utr,
          savePreferences = savePreferences
        )
    )

  private def savePreferences(utr: SaUtr, digital: Boolean, email: Option[String] = None, hc: HeaderCarrier) =
    preferencesConnector.savePreferences(utr, digital, email)(hc)

  private[prefs] def emailAddressChangeThankYouPage(implicit authContext: AuthContext, request: Request[AnyRef], hostContext: HostContext): Future[Result] = {
    lookupCurrentEmail(email => Future.successful(Ok(views.html.account_details_update_email_address_thank_you(email.obfuscated))))
  }
}

