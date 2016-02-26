package controllers.internal

import config.Global
import connectors._
import controllers.AuthContextAvailability._
import controllers.Authentication
import model.{Encrypted, HostContext}
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.{Actions, AuthContext}
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

object ManagePaperlessController extends ManagePaperlessController with ServicesConfig with Authentication {
  lazy val auditConnector = Global.auditConnector

  val authConnector = Global.authConnector

  lazy val emailConnector = EmailConnector
  lazy val entityResolverConnector = EntityResolverConnector

  def displayChangeEmailAddress(implicit emailAddress: Option[Encrypted[EmailAddress]], hostContext: HostContext) = authenticated.async { implicit authContext => implicit request =>
    _displayChangeEmailAddress(emailAddress)
  }

  def submitChangeEmailAddress(implicit hostContext: HostContext) = authenticated.async { implicit authContext => implicit request =>
    _submitChangeEmailAddress
  }

  def displayChangeEmailAddressConfirmed(implicit hostContext: HostContext) = authenticated.async { implicit authContext => implicit request =>
    _displayChangeEmailAddressConfirmed
  }

  def displayStopPaperless(implicit hostContext: HostContext) = authenticated.async { implicit authContext => implicit request =>
    _displayStopPaperless
  }

  def submitStopPaperless(implicit hostContext: HostContext) = authenticated.async { implicit authContext => implicit request =>
    _submitStopPaperless
  }

  def displayStopPaperlessConfirmed(implicit hostContext: HostContext) = authenticated { implicit authContext => implicit request =>
    _displayStopPaperlessConfirmed
  }

  def resendVerificationEmail(implicit hostContext: HostContext) = authenticated.async { implicit authContext => implicit request =>
    _resendVerificationEmail
  }
}

trait ManagePaperlessController
extends FrontendController
with Actions {

  val auditConnector: AuditConnector
  val authConnector: AuthConnector
  val emailConnector: EmailConnector
  val entityResolverConnector: EntityResolverConnector

  private[controllers] def _displayStopPaperlessConfirmed(implicit authContext: AuthContext, request: Request[AnyRef], hostContext: HostContext): Result = {
    Ok(views.html.opted_back_into_paper_thank_you())
  }

  private[controllers] def _submitStopPaperless(implicit authContext: AuthContext, request: Request[AnyRef], hostContext: HostContext): Future[Result] =
    entityResolverConnector.updateTermsAndConditions(authContext.principal.accounts.sa.get.utr, (Generic, TermsAccepted(false)), email = None).map(_ =>
      Redirect(routes.ManagePaperlessController.displayStopPaperlessConfirmed(hostContext))
    )

  private[controllers] def _resendVerificationEmail(implicit authContext: AuthContext, request: Request[AnyRef], hostContext: HostContext): Future[Result] = {
    lookupCurrentEmail { email =>
      entityResolverConnector.savePreferences(authContext.principal.accounts.sa.get.utr, true, Some(email)).map(_ =>
        Ok(views.html.account_details_verification_email_resent_confirmation(email))
      )
    }
  }

  private[controllers] def _displayStopPaperless(implicit authContext: AuthContext, request: Request[AnyRef], hostContext: HostContext) =
    lookupCurrentEmail(email => Future.successful(Ok(views.html.confirm_opt_back_into_paper(email.obfuscated))))

  private[controllers] def _displayChangeEmailAddress(emailAddress: Option[Encrypted[EmailAddress]])(implicit authContext: AuthContext, request: Request[AnyRef], hostContext: HostContext): Future[Result] =
    lookupCurrentEmail(email => Future.successful(Ok(views.html.account_details_update_email_address(email, EmailForm().fill(EmailForm.Data(emailAddress.map(_.decryptedValue)))))))

  private def lookupCurrentEmail(func: (EmailAddress) => Future[Result])(implicit authContext: AuthContext, request: Request[AnyRef]): Future[Result] = {
    entityResolverConnector.getPreferences(authContext.principal.accounts.sa.get.utr).flatMap {
        case Some(SaPreference(true, Some(email))) => func(EmailAddress(email.email))
        case _ => Future.successful(BadRequest("Could not find existing preferences."))
    }
  }

  private[controllers] def _submitChangeEmailAddress(implicit authContext: AuthContext, request: Request[AnyRef], hostContext: HostContext): Future[Result] = {
    lookupCurrentEmail(
      email => {
        EmailForm().bindFromRequest()(request).fold(
          errors => Future.successful(BadRequest(views.html.account_details_update_email_address(email, errors))),
          emailForm => {
            val emailVerificationStatus =
              if (emailForm.isEmailVerified) Future.successful(true)
              else emailConnector.isValid(emailForm.mainEmail)

            emailVerificationStatus.flatMap {
              case true => entityResolverConnector.savePreferences(
                utr = authContext.principal.accounts.sa.get.utr,
                digital = true,
                email = Some(emailForm.mainEmail)
              ).map(_ => Redirect(routes.ManagePaperlessController.displayChangeEmailAddressConfirmed(hostContext)))
              case false => Future.successful(Ok(views.html.account_details_update_email_address_verify_email(emailForm.mainEmail)))
            }
          }
        )
      }
    )
  }

  private[controllers] def _displayChangeEmailAddressConfirmed(implicit authContext: AuthContext, request: Request[AnyRef], hostContext: HostContext): Future[Result] = {
    lookupCurrentEmail(email => Future.successful(Ok(views.html.account_details_update_email_address_thank_you(email.obfuscated))))
  }
}

