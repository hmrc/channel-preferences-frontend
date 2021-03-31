/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers.internal

import connectors._
import controllers.ExternalUrlPrefixes
import controllers.auth.{ AuthenticatedRequest, WithAuthRetrievals }
import model.{ Encrypted, HostContext, Language }
import play.api.Configuration
import play.api.i18n.I18nSupport
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents, Result }
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.manage._

import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

class ManagePaperlessController @Inject() (
  entityResolverConnector: EntityResolverConnector,
  emailConnector: EmailConnector,
  auditConnector: AuditConnector,
  val authConnector: AuthConnector,
  externalUrlPrefixes: ExternalUrlPrefixes,
  configuration: Configuration,
  optedBackIntoPaperThankYou: views.html.opted_back_into_paper_thank_you,
  accountDetailsVerificationEmailResentConfirmation: views.html.account_details_verification_email_resent_confirmation,
  confirmOptBackIntoPaper: views.html.confirm_opt_back_into_paper,
  accountDetailsUpdateEmailAddress: views.html.account_details_update_email_address,
  accountDetailsUpdateEmailAddressVerifyEmail: views.html.account_details_update_email_address_verify_email,
  accountDetailsUpdateEmailAddressThankYou: views.html.account_details_update_email_address_thank_you,
  digitalFalseFull: digital_false_full,
  digitalTrueBouncedFull: digital_true_bounced_full,
  digitalTrueVerifiedFull: digital_true_verified_full,
  digitalTruePendingFull: digital_true_pending_full,
  howToVerifyEmail: views.html.how_to_verify_email,
  emailDeliveryFailed: views.html.email_delivery_failed,
  mcc: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with OptInCohortCalculator with I18nSupport with WithAuthRetrievals
    with LanguageHelper {

  private[controllers] def _displayStopPaperlessConfirmed(implicit
    request: AuthenticatedRequest[_],
    hostContext: HostContext,
    hc: HeaderCarrier
  ): Result =
    Ok(optedBackIntoPaperThankYou())

  private[controllers] def _submitStopPaperless(
    lang: Some[Language]
  )(implicit request: AuthenticatedRequest[_], hostContext: HostContext, hc: HeaderCarrier): Future[Result] =
    entityResolverConnector
      .updateTermsAndConditions(
        TermsAndConditionsUpdate.from(
          (GenericTerms, TermsAccepted(false, Some(OptInPage.from(CohortCurrent.cysConfirmPage)))),
          email = None,
          false,
          lang
        )
      )
      .map(_ => Redirect(routes.ManagePaperlessController.checkSettings(hostContext)))

  private[controllers] def _resendVerificationEmail(implicit
    request: AuthenticatedRequest[_],
    hostContext: HostContext,
    hc: HeaderCarrier
  ): Future[Result] =
    lookupCurrentEmail { email =>
      entityResolverConnector
        .changeEmailAddress(email)
        .map(_ => Ok(accountDetailsVerificationEmailResentConfirmation(email)))
    }

  private[controllers] def _displayStopPaperless(implicit
    request: AuthenticatedRequest[_],
    hostContext: HostContext,
    hc: HeaderCarrier
  ): Future[Result] =
    lookupCurrentEmail(email => Future.successful(Ok(confirmOptBackIntoPaper(email.obfuscated))))

  private[controllers] def _displayChangeEmailAddress(
    emailAddress: Option[Encrypted[EmailAddress]]
  )(implicit request: AuthenticatedRequest[_], hostContext: HostContext, hc: HeaderCarrier): Future[Result] =
    lookupCurrentEmail(email =>
      Future.successful(
        Ok(
          accountDetailsUpdateEmailAddress(email, EmailForm().fill(EmailForm.Data(emailAddress.map(_.decryptedValue))))
        )
      )
    )

  private def lookupCurrentEmail(
    func: (EmailAddress) => Future[Result]
  )(implicit request: AuthenticatedRequest[_], hc: HeaderCarrier): Future[Result] =
    entityResolverConnector.getPreferences().flatMap {
      case p @ Some(PreferenceResponse(_, Some(email), _)) if p.exists(_.genericTermsAccepted) =>
        func(EmailAddress(email.email))
      case _ =>
        Future.successful(BadRequest("Could not find existing preferences."))
    }

  private[controllers] def _submitChangeEmailAddress(implicit
    request: AuthenticatedRequest[_],
    hostContext: HostContext,
    hc: HeaderCarrier
  ): Future[Result] =
    lookupCurrentEmail { email =>
      EmailForm()
        .bindFromRequest()(request.request)
        .fold(
          errors => Future.successful(BadRequest(accountDetailsUpdateEmailAddress(email, errors))),
          emailForm => {
            val emailVerificationStatus =
              if (emailForm.isEmailVerified) Future.successful(true)
              else emailConnector.isValid(emailForm.mainEmail)

            emailVerificationStatus.flatMap {
              case true =>
                entityResolverConnector
                  .changeEmailAddress(emailForm.mainEmail)
                  .map(_ => Redirect(routes.ManagePaperlessController.displayChangeEmailAddressConfirmed(hostContext)))
              case false => Future.successful(Ok(accountDetailsUpdateEmailAddressVerifyEmail(emailForm.mainEmail)))
            }
          }
        )
    }

  private[controllers] def _displayChangeEmailAddressConfirmed(implicit
    request: AuthenticatedRequest[_],
    hostContext: HostContext,
    hc: HeaderCarrier
  ): Future[Result] =
    lookupCurrentEmail(email => Future.successful(Ok(accountDetailsUpdateEmailAddressThankYou(email.obfuscated))))

  private[controllers] def _displayHowToVerifyEmail(
    emailPref: EmailPreference
  )(implicit request: AuthenticatedRequest[_], hostContext: HostContext): Result =
    Ok(howToVerifyEmail(emailPref))

  private[controllers] def _displayDeliveryFailed(
    emailPref: EmailPreference
  )(implicit request: AuthenticatedRequest[_], hostContext: HostContext): Result =
    Ok(emailDeliveryFailed(emailPref))

  def displayChangeEmailAddress(implicit
    emailAddress: Option[Encrypted[EmailAddress]],
    hostContext: HostContext
  ): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest {
        { implicit authenticatedRequest: AuthenticatedRequest[_] => implicit hc =>
          _displayChangeEmailAddress(emailAddress)
        }
      }
    }

  def submitChangeEmailAddress(implicit hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit withAuthenticatedRequest: AuthenticatedRequest[_] => implicit hc =>
        _submitChangeEmailAddress
      }
    }

  def displayChangeEmailAddressConfirmed(implicit hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit withAuthenticatedRequest: AuthenticatedRequest[_] => implicit hc =>
        _displayChangeEmailAddressConfirmed
      }
    }

  def displayStopPaperless(implicit hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit withAuthenticatedRequest: AuthenticatedRequest[_] => implicit hc =>
        _displayStopPaperless
      }
    }

  def submitStopPaperless(implicit hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest {
        val lang = languageType(request.lang.code)
        implicit withAuthenticatedRequest: AuthenticatedRequest[_] => implicit hc =>
          _submitStopPaperless(Some(lang))
      }
    }

  def displayStopPaperlessConfirmed(implicit hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit withAuthenticatedRequest: AuthenticatedRequest[_] => implicit hc =>
        Future.successful(_displayStopPaperlessConfirmed)
      }
    }

  def resendVerificationEmail(implicit hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit withAuthenticatedRequest: AuthenticatedRequest[_] => implicit hc =>
        _resendVerificationEmail
      }
    }

  def checkSettings(implicit hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit withAuthenticatedRequest: AuthenticatedRequest[_] => implicit hc =>
        entityResolverConnector.getPreferences() map { pref =>
          Ok(pref match {
            case p @ Some(PreferenceResponse(map, Some(email), _)) if p.exists(_.genericTermsAccepted) =>
              (email.hasBounces, email.isVerified) match {
                case (true, _) => digitalTrueBouncedFull(email)
                case (_, true) => digitalTrueVerifiedFull(email)
                case _         => digitalTruePendingFull(email)
              }
            case p @ Some(PreferenceResponse(_, email, _)) =>
              val encryptedEmail = email map (emailPreference => Encrypted(EmailAddress(emailPreference.email)))
              digitalFalseFull()
            case _ => digitalFalseFull()
          })
        }
      }
    }

  def howToVerifyEmail(implicit hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit withAuthenticatedRequest: AuthenticatedRequest[_] => implicit hc =>
        entityResolverConnector.getPreferences().map {
          case None => NotFound
          case Some(pref) =>
            pref.email match {
              case None            => NotFound
              case Some(emailPref) => _displayHowToVerifyEmail(emailPref)
            }
        }
      }
    }

  def deliveryFailed(implicit hostContext: HostContext): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedRequest { implicit withAuthenticatedRequest: AuthenticatedRequest[_] => implicit hc =>
        entityResolverConnector.getPreferences().map {
          case None => NotFound
          case Some(pref) =>
            pref.email match {
              case None            => NotFound
              case Some(emailPref) => _displayDeliveryFailed(emailPref)
            }
        }
      }
    }
}
