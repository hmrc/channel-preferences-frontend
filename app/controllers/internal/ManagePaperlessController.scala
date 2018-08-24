package controllers.internal

import config.Global
import connectors._
import controllers.auth.{AuthAction, AuthController, AuthenticatedRequest}
import model.{Encrypted, HostContext}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.Result
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

object ManagePaperlessController extends ManagePaperlessController with ServicesConfig {
  lazy val auditConnector = Global.auditConnector

  val authorise: AuthAction = AuthController

  lazy val emailConnector = EmailConnector
  lazy val entityResolverConnector = EntityResolverConnector

  def displayChangeEmailAddress(implicit emailAddress: Option[Encrypted[EmailAddress]], hostContext: HostContext) = authorise.async { implicit authenticatedRequest =>
    _displayChangeEmailAddress(emailAddress)
  }

  def submitChangeEmailAddress(implicit hostContext: HostContext) = authorise.async { implicit authenticatedRequest =>
    _submitChangeEmailAddress
  }

  def displayChangeEmailAddressConfirmed(implicit hostContext: HostContext) = authorise.async { implicit authenticatedRequest =>
    _displayChangeEmailAddressConfirmed
  }

  def displayStopPaperless(implicit hostContext: HostContext) = authorise.async { implicit authenticatedRequest =>
    _displayStopPaperless
  }

  def submitStopPaperless(implicit hostContext: HostContext) = authorise.async { implicit authenticatedRequest =>
    _submitStopPaperless
  }

  def displayStopPaperlessConfirmed(implicit hostContext: HostContext) = authorise { implicit authenticatedRequest =>
    _displayStopPaperlessConfirmed
  }

  def resendVerificationEmail(implicit hostContext: HostContext) = authorise.async { implicit authenticatedRequest =>
    _resendVerificationEmail
  }
}

trait ManagePaperlessController
  extends FrontendController {

  val auditConnector: AuditConnector
  val emailConnector: EmailConnector
  val entityResolverConnector: EntityResolverConnector

  private[controllers] def _displayStopPaperlessConfirmed(implicit request: AuthenticatedRequest[_], hostContext: HostContext): Result = {
    Ok(views.html.opted_back_into_paper_thank_you())
  }

  private[controllers] def _submitStopPaperless(implicit request: AuthenticatedRequest[_], hostContext: HostContext): Future[Result] =
    entityResolverConnector.updateTermsAndConditions((GenericTerms, TermsAccepted(false)), email = None).map(_ =>
      Redirect(routes.ManagePaperlessController.displayStopPaperlessConfirmed(hostContext))
    )

  private[controllers] def _resendVerificationEmail(implicit request: AuthenticatedRequest[_], hostContext: HostContext): Future[Result] = {
    lookupCurrentEmail { email =>
      entityResolverConnector.changeEmailAddress(email).map(_ =>
        Ok(views.html.account_details_verification_email_resent_confirmation(email))
      )
    }
  }

  private[controllers] def _displayStopPaperless(implicit request: AuthenticatedRequest[_], hostContext: HostContext) =
    lookupCurrentEmail(email => Future.successful(Ok(views.html.confirm_opt_back_into_paper(email.obfuscated))))

  private[controllers] def _displayChangeEmailAddress(emailAddress: Option[Encrypted[EmailAddress]])(implicit request: AuthenticatedRequest[_], hostContext: HostContext): Future[Result] =
    lookupCurrentEmail(email => Future.successful(Ok(views.html.account_details_update_email_address(email, EmailForm().fill(EmailForm.Data(emailAddress.map(_.decryptedValue)))))))

  private def lookupCurrentEmail(func: (EmailAddress) => Future[Result])(implicit request: AuthenticatedRequest[_]): Future[Result] = {
    entityResolverConnector.getPreferences().flatMap {
      case p@Some(PreferenceResponse(_, Some(email))) if (p.exists(_.genericTermsAccepted)) => func(EmailAddress(email.email))
      case _ => Future.successful(BadRequest("Could not find existing preferences."))
    }
  }

  private[controllers] def _submitChangeEmailAddress(implicit request: AuthenticatedRequest[_], hostContext: HostContext): Future[Result] = {
    lookupCurrentEmail(
      email => {
        EmailForm().bindFromRequest()(request.request).fold(
          errors => Future.successful(BadRequest(views.html.account_details_update_email_address(email, errors))),
          emailForm => {
            val emailVerificationStatus =
              if (emailForm.isEmailVerified) Future.successful(true)
              else emailConnector.isValid(emailForm.mainEmail)

            emailVerificationStatus.flatMap {
              case true => entityResolverConnector.changeEmailAddress(emailForm.mainEmail).map(_ =>
                Redirect(routes.ManagePaperlessController.displayChangeEmailAddressConfirmed(hostContext))
              )
              case false => Future.successful(Ok(views.html.account_details_update_email_address_verify_email(emailForm.mainEmail)))
            }
          }
        )
      }
    )
  }

  private[controllers] def _displayChangeEmailAddressConfirmed(implicit request: AuthenticatedRequest[_], hostContext: HostContext): Future[Result] = {
    lookupCurrentEmail(email => Future.successful(Ok(views.html.account_details_update_email_address_thank_you(email.obfuscated))))
  }
}

