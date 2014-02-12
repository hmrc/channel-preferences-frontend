package controllers.bt

import controllers.common.{GovernmentGateway, BaseController}
import controllers.common.actions.{HeaderCarrier, Actions}
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import controllers.common.service.Connectors
import uk.gov.hmrc.common.microservice.preferences.PreferencesConnector
import play.api.mvc.Request
import scala.concurrent.Future
import uk.gov.hmrc.common.microservice.email.EmailConnector
import uk.gov.hmrc.common.microservice.sa.domain.SaRegime
import uk.gov.hmrc.common.microservice.preferences.SaPreference
import play.api.mvc.SimpleResult
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.domain.Email._
import uk.gov.hmrc.domain.{SaUtr, Email}
import controllers.common.domain.EmailPreferenceData
import controllers.common.preferences.EmailControllerHelper

class AccountDetailsController(override val auditConnector: AuditConnector, val preferencesConnector: PreferencesConnector,
                               val emailConnector: EmailConnector)(implicit override val authConnector: AuthConnector) extends BaseController
with Actions
with BusinessTaxRegimeRoots
with EmailControllerHelper {

  def this() = this(Connectors.auditConnector, Connectors.preferencesConnector, Connectors.emailConnector)(Connectors.authConnector)


  def accountDetails() = AuthenticatedBy(GovernmentGateway).async {
    user => request => accountDetailsPage(user, request)
  }

  def changeEmailAddress(emailAddress: Option[Email]) = AuthorisedFor(regime = SaRegime).async {
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

  private[bt] def confirmOptOutOfEmailRemindersPage(implicit user: User, request: Request[AnyRef]): Future[SimpleResult] = {
    lookupCurrentEmail {
      email =>
        preferencesConnector.savePreferences(user.getSaUtr, false, None).map(_ =>
          Redirect(routes.AccountDetailsController.optedBackIntoPaperThankYou())
        )
    }
  }

  private[bt] def resendValidationEmailAction(implicit user: User, request: Request[AnyRef]): Future[SimpleResult] = {
    lookupCurrentEmail {
      email =>
        preferencesConnector.savePreferences(user.getSaUtr, true, Some(email)).map(_ =>
          Ok(views.html.account_details_verification_email_resent_confirmation(user))
        )
    }
  }

  private[bt] def optOutOfEmailRemindersPage(implicit user: User, request: Request[AnyRef]) = {
    lookupCurrentEmail(email => Future.successful(Ok(views.html.confirm_opt_back_into_paper(email.obfuscated))))
  }

  private[bt] def accountDetailsPage(implicit user: User, request: Request[AnyRef]) = {
    val saPreferenceF = user.regimes.sa.map(regime => preferencesConnector.getPreferences(regime.utr)(HeaderCarrier(request))).getOrElse(Future.successful(None))

    for {
      preference <- saPreferenceF
    } yield Ok(views.html.account_details(preference))

  }

  private[bt] def changeEmailAddressPage(emailAddress: Option[Email])(implicit user: User, request: Request[AnyRef]): Future[SimpleResult] =
    lookupCurrentEmail(email => Future.successful(Ok(views.html.account_details_update_email_address(email, emailForm.fill(EmailPreferenceData(emailAddress))))))



  private def lookupCurrentEmail(func: (String) => Future[SimpleResult])(implicit user: User, request: Request[AnyRef]): Future[SimpleResult] = {
    preferencesConnector.getPreferences(user.getSaUtr)(HeaderCarrier(request)).flatMap {
        case Some(SaPreference(true, Some(email))) => func(email.email)
        case _ => Future.successful(BadRequest("Could not find existing preferences."))
    }
  }

  private[bt] def submitEmailAddressPage(implicit user: User, request: Request[AnyRef]): Future[SimpleResult] =
    lookupCurrentEmail(
      email =>
        submitPreferencesForm(
          (errors) => views.html.account_details_update_email_address(email, errors),
          (enteredEmail) => views.html.account_details_update_email_address_verify_email(enteredEmail),
          () => routes.AccountDetailsController.emailAddressChangeThankYou(),
          emailConnector,
          user.getSaUtr,
          savePreferences
        )
    )

  private def savePreferences(utr: SaUtr, digital: Boolean, email: Option[String] = None, hc: HeaderCarrier) = {
    preferencesConnector.savePreferences(utr, digital, email)(hc)
  }

  private[bt] def emailAddressChangeThankYouPage(implicit user: User, request: Request[AnyRef]): Future[SimpleResult] = {
    lookupCurrentEmail(email => Future.successful(Ok(views.html.account_details_update_email_address_thank_you(email.obfuscated)(user))))
  }
}