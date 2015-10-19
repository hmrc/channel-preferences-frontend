package controllers.sa.prefs.internal

import authentication.ValidSessionCredentialsProvider
import connectors.{EmailConnector, PreferencesConnector, SaPreference}
import controllers.sa.prefs.AuthContextAvailability._
import controllers.sa.prefs.config.Global
import controllers.sa.prefs.{ExternalUrls, Encrypted, EmailFormData, SaRegime}
import hostcontext.ReturnUrl
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

object AccountDetailsController extends AccountDetailsController with ServicesConfig {
  lazy val auditConnector = Global.auditConnector

  val authConnector = Global.authConnector

  lazy val emailConnector = EmailConnector
  lazy val preferencesConnector = PreferencesConnector
}

trait AccountDetailsController
  extends FrontendController
  with Actions
  with PreferencesControllerHelper {

  val auditConnector: AuditConnector
  val authConnector: AuthConnector
  val emailConnector: EmailConnector
  val preferencesConnector: PreferencesConnector

  def changeEmailAddress(emailAddress: Option[Encrypted[EmailAddress]]) = AuthorisedFor(SaRegime).async {
    authContext => request => changeEmailAddressPage(emailAddress)(authContext, request)
  }

  def submitEmailAddress = AuthorisedFor(SaRegime).async {
    authContext => request => submitEmailAddressPage(authContext, request)
  }

  def emailAddressChangeThankYou() = AuthorisedFor(SaRegime).async {
    authContext => request => emailAddressChangeThankYouPage(authContext, request)
  }

  def optOutOfEmailReminders = AuthorisedFor(SaRegime).async {
    authContext => request => optOutOfEmailRemindersPage(authContext, request)
  }

  def confirmOptOutOfEmailReminders = AuthorisedFor(SaRegime).async {
    authContext => request => confirmOptOutOfEmailRemindersPage(authContext, request)
  }

  def optedBackIntoPaperThankYou() = AuthenticatedBy(ValidSessionCredentialsProvider, redirectToOrigin = true).async {
    implicit authContext => implicit request =>
      Future(Ok(views.html.opted_back_into_paper_thank_you()))
  }

  def resendValidationEmail(implicit returnUrl: ReturnUrl) = AuthorisedFor(SaRegime).async { implicit authContext => implicit request =>
    resendValidationEmailAction
  }

  def resendValidationEmailDeprecated = AuthorisedFor(SaRegime).async { implicit authContext => implicit request =>
    implicit val returnUrl = ReturnUrl(ExternalUrls.businessTaxHome)
    resendValidationEmailAction
  }

  private[prefs] def confirmOptOutOfEmailRemindersPage(implicit authContext: AuthContext, request: Request[AnyRef]): Future[Result] = {
    lookupCurrentEmail {
      email =>
        preferencesConnector.savePreferences(authContext.principal.accounts.sa.get.utr, false, None).map(_ =>
          Redirect(routes.AccountDetailsController.optedBackIntoPaperThankYou())
        )
    }
  }

  private[prefs] def resendValidationEmailAction(implicit authContext: AuthContext, request: Request[AnyRef], returnUrl: ReturnUrl): Future[Result] = {
    lookupCurrentEmail { email =>
      preferencesConnector.savePreferences(authContext.principal.accounts.sa.get.utr, true, Some(email)).map(_ =>
        Ok(views.html.account_details_verification_email_resent_confirmation(email))
      )
    }
  }

  private[prefs] def optOutOfEmailRemindersPage(implicit authContext: AuthContext, request: Request[AnyRef]) =
    lookupCurrentEmail(email => Future.successful(Ok(views.html.confirm_opt_back_into_paper(email.obfuscated))))

  private[prefs] def changeEmailAddressPage(emailAddress: Option[Encrypted[EmailAddress]])(implicit authContext: AuthContext, request: Request[AnyRef]): Future[Result] =
    lookupCurrentEmail(email => Future.successful(Ok(views.html.account_details_update_email_address(email, emailForm.fill(EmailFormData(emailAddress.map(_.decryptedValue)))))))

  private def lookupCurrentEmail(func: (EmailAddress) => Future[Result])(implicit authContext: AuthContext, request: Request[AnyRef]): Future[Result] = {
    preferencesConnector.getPreferences(authContext.principal.accounts.sa.get.utr, None)(HeaderCarrier.fromSessionAndHeaders(request.session, request.headers)).flatMap {
        case Some(SaPreference(true, Some(email))) => func(EmailAddress(email.email))
        case _ => Future.successful(BadRequest("Could not find existing preferences."))
    }
  }

  private[prefs] def submitEmailAddressPage(implicit authContext: AuthContext, request: Request[AnyRef]): Future[Result] =
    lookupCurrentEmail(
      email =>
        submitEmailForm(
          views.html.account_details_update_email_address(email, _),
          (enteredEmail) => views.html.account_details_update_email_address_verify_email(enteredEmail),
          routes.AccountDetailsController.emailAddressChangeThankYou(),
          emailConnector,
          authContext.principal.accounts.sa.get.utr,
          savePreferences
        )
    )

  private def savePreferences(utr: SaUtr, digital: Boolean, email: Option[String] = None, hc: HeaderCarrier) =
    preferencesConnector.savePreferences(utr, digital, email)(hc)

  private[prefs] def emailAddressChangeThankYouPage(implicit authContext: AuthContext, request: Request[AnyRef]): Future[Result] = {
    lookupCurrentEmail(email => Future.successful(Ok(views.html.account_details_update_email_address_thank_you(email.obfuscated))))
  }
}

