package controllers.sa.prefs.internal

import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.email.EmailConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import controllers.common.BaseController
import controllers.common.actions.Actions
import controllers.common.service.Connectors
import uk.gov.hmrc.common.crypto.Encrypted
import uk.gov.hmrc.common.microservice.sa.domain.SaRegime
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.emailaddress.EmailAddress
import scala.concurrent.Future
import uk.gov.hmrc.common.microservice.domain.User
import play.api.mvc.{SimpleResult, Request}
import controllers.sa.prefs.{EmailFormData, PreferencesControllerHelper}
import uk.gov.hmrc.play.connectors.HeaderCarrier
import connectors.{FormattedUri, PreferencesConnector, SaPreference}

class AccountDetailsController(val auditConnector: AuditConnector, val preferencesConnector: PreferencesConnector,
                               val emailConnector: EmailConnector)(implicit override val authConnector: AuthConnector) extends BaseController
  with Actions
  with PreferencesControllerHelper {

  def this() = this(Connectors.auditConnector, PreferencesConnector, Connectors.emailConnector)(Connectors.authConnector)

  def changeEmailAddress(emailAddress: Option[Encrypted[EmailAddress]]) = AuthorisedFor(regime = SaRegime).async {
    user => request => changeEmailAddressPage(emailAddress)(user, request)
  }

  def submitEmailAddress = AuthorisedFor(regime = SaRegime).async {
    user => request => submitEmailAddressPage(user, request)
  }


  def emailAddressChangeThankYou() = AuthorisedFor(regime = SaRegime).async {
    user => request => emailAddressChangeThankYouPage(user, request)
  }

  def optOutOfEmailReminders = AuthorisedFor(regime = SaRegime).async {
    user => request => optOutOfEmailRemindersPage(user, request)
  }

  def confirmOptOutOfEmailReminders = AuthorisedFor(regime = SaRegime).async {
    user => request => confirmOptOutOfEmailRemindersPage(user, request)
  }

  def optedBackIntoPaperThankYou() = AuthorisedFor(regime = SaRegime).async {
    user => implicit request => Future(Ok(views.html.opted_back_into_paper_thank_you(user)))
  }

  def resendValidationEmail() = AuthorisedFor(regime = SaRegime).async {
    user => request => resendValidationEmailAction(user, request)
  }

  private[prefs] def confirmOptOutOfEmailRemindersPage(implicit user: User, request: Request[AnyRef]): Future[SimpleResult] = {
    lookupCurrentEmail {
      email =>
        preferencesConnector.savePreferences(user.userAuthority.accounts.sa.get.utr, false, None).map(_ =>
          Redirect(routes.AccountDetailsController.optedBackIntoPaperThankYou())
        )
    }
  }

  private[prefs] def resendValidationEmailAction(implicit user: User, request: Request[AnyRef]): Future[SimpleResult] = {
    lookupCurrentEmail {
      email =>
        preferencesConnector.savePreferences(user.userAuthority.accounts.sa.get.utr, true, Some(email)).map(_ =>
          Ok(views.html.account_details_verification_email_resent_confirmation(user))
        )
    }
  }

  private[prefs] def optOutOfEmailRemindersPage(implicit user: User, request: Request[AnyRef]) = {
    lookupCurrentEmail(email => Future.successful(Ok(views.html.confirm_opt_back_into_paper(email.obfuscated))))
  }

  private[prefs] def changeEmailAddressPage(emailAddress: Option[Encrypted[EmailAddress]])(implicit user: User, request: Request[AnyRef]): Future[SimpleResult] =
    lookupCurrentEmail(email => Future.successful(Ok(views.html.account_details_update_email_address(email, emailForm.fill(EmailFormData(emailAddress.map(_.decryptedValue)))))))



  private def lookupCurrentEmail(func: (EmailAddress) => Future[SimpleResult])(implicit user: User, request: Request[AnyRef]): Future[SimpleResult] = {
    preferencesConnector.getPreferences(user.userAuthority.accounts.sa.get.utr)(HeaderCarrier.fromSessionAndHeaders(request.session, request.headers)).flatMap {
        case Some(SaPreference(true, Some(email))) => func(EmailAddress(email.email))
        case _ => Future.successful(BadRequest("Could not find existing preferences."))
    }
  }

  private[prefs] def submitEmailAddressPage(implicit user: User, request: Request[AnyRef]): Future[SimpleResult] =
    lookupCurrentEmail(
      email =>
        submitEmailForm(
          views.html.account_details_update_email_address(email, _),
          (enteredEmail) => views.html.account_details_update_email_address_verify_email(enteredEmail),
          () => routes.AccountDetailsController.emailAddressChangeThankYou(),
          emailConnector,
          user.userAuthority.accounts.sa.get.utr,
          savePreferences
        )
    )

  private def savePreferences(utr: SaUtr, digital: Boolean, email: Option[String] = None, hc: HeaderCarrier): Future[Option[FormattedUri]] = {
    preferencesConnector.savePreferences(utr, digital, email)(hc)
  }

  private[prefs] def emailAddressChangeThankYouPage(implicit user: User, request: Request[AnyRef]): Future[SimpleResult] = {
    lookupCurrentEmail(email => Future.successful(Ok(views.html.account_details_update_email_address_thank_you(email.obfuscated)(user))))
  }
}
