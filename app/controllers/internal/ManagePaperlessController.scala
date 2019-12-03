/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.internal

import connectors._
import controllers.ExternalUrlPrefixes
import controllers.auth.{AuthenticatedRequest, PreferenceFrontendAuthAction}
import javax.inject.Inject
import model.{Encrypted, HostContext}
import play.api.Configuration
import play.api.i18n.I18nSupport
import play.api.mvc.{MessagesControllerComponents, Result}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}


class ManagePaperlessController @Inject()(
                                             authorise: PreferenceFrontendAuthAction,
                                             entityResolverConnector: EntityResolverConnector,
                                             emailConnector: EmailConnector,
                                             auditConnector: AuditConnector,
                                             val authConnector: AuthConnector,
                                             externalUrlPrefixes: ExternalUrlPrefixes,
                                             configuration: Configuration,
                                          optedBackIntoPaperThankYou: views.html.opted_back_into_paper_thank_you,
  accountDetailsVerificationEmailResentConfirmation: views.html.account_details_verification_email_resent_confirmation,
                                             confirmOptBackIntoPaper : views.html.confirm_opt_back_into_paper,
  accountDetailsUpdateEmailAddress: views.html.account_details_update_email_address,
  accountDetailsUpdateEmailAddressVerifyEmail: views.html.account_details_update_email_address_verify_email,
  accountDetailsUpdateEmailAddressThankYou: views.html.account_details_update_email_address_thank_you,
mcc: MessagesControllerComponents)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with OptInCohortCalculator with I18nSupport {

  private[controllers] def _displayStopPaperlessConfirmed(implicit request: AuthenticatedRequest[_], hostContext: HostContext): Result = {
    Ok(optedBackIntoPaperThankYou())
  }

  private[controllers] def _submitStopPaperless(implicit request: AuthenticatedRequest[_], hostContext: HostContext, hc:HeaderCarrier): Future[Result] =
    entityResolverConnector.updateTermsAndConditions((GenericTerms, TermsAccepted(false)), email = None).map(_ =>
      Redirect(routes.ManagePaperlessController.displayStopPaperlessConfirmed(hostContext))
    )

  private[controllers] def _resendVerificationEmail(implicit request: AuthenticatedRequest[_], hostContext: HostContext, hc:HeaderCarrier): Future[Result] = {
    lookupCurrentEmail { email =>
      entityResolverConnector.changeEmailAddress(email).map(_ =>
        Ok(accountDetailsVerificationEmailResentConfirmation(email))
      )
    }
  }

  private[controllers] def _displayStopPaperless(implicit request: AuthenticatedRequest[_], hostContext: HostContext, hc:HeaderCarrier) =
    lookupCurrentEmail(email => Future.successful(Ok(confirmOptBackIntoPaper(email.obfuscated))))

  private[controllers] def _displayChangeEmailAddress(emailAddress: Option[Encrypted[EmailAddress]])(implicit request: AuthenticatedRequest[_], hostContext: HostContext): Future[Result] =
    lookupCurrentEmail(email => Future.successful(Ok(accountDetailsUpdateEmailAddress(email, EmailForm().fill(EmailForm.Data(emailAddress.map(_.decryptedValue)))))))

  private def lookupCurrentEmail(func: (EmailAddress) => Future[Result])(implicit request: AuthenticatedRequest[_], hc:HeaderCarrier): Future[Result] = {
    entityResolverConnector.getPreferences().flatMap {
      case p@Some(PreferenceResponse(_, Some(email))) if (p.exists(_.genericTermsAccepted)) => func(EmailAddress(email.email))
      case _ => {
          Future.successful(BadRequest("Could not find existing preferences."))
      }
    }
  }

  private[controllers] def _submitChangeEmailAddress(implicit request: AuthenticatedRequest[_], hostContext: HostContext, hc:HeaderCarrier): Future[Result] = {
    lookupCurrentEmail(
      email => {
        EmailForm().bindFromRequest()(request.request).fold(
          errors => Future.successful(BadRequest(accountDetailsUpdateEmailAddress(email, errors))),
          emailForm => {
            val emailVerificationStatus =
              if (emailForm.isEmailVerified) Future.successful(true)
              else emailConnector.isValid(emailForm.mainEmail)

            emailVerificationStatus.flatMap {
              case true => entityResolverConnector.changeEmailAddress(emailForm.mainEmail).map(_ =>
                Redirect(routes.ManagePaperlessController.displayChangeEmailAddressConfirmed(hostContext))
              )
              case false => Future.successful(Ok(accountDetailsUpdateEmailAddressVerifyEmail(emailForm.mainEmail)))
            }
          }
        )
      }
    )
  }

  private[controllers] def _displayChangeEmailAddressConfirmed(implicit request: AuthenticatedRequest[_], hostContext: HostContext): Future[Result] = {
    lookupCurrentEmail(email => Future.successful(Ok(accountDetailsUpdateEmailAddressThankYou(email.obfuscated))))
  }

    def displayChangeEmailAddress(implicit emailAddress: Option[Encrypted[EmailAddress]], hostContext: HostContext) = authorise.async { implicit authenticatedRequest =>

    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromHeadersAndSession(authenticatedRequest.headers)
        _displayChangeEmailAddress(emailAddress)
    }

    def submitChangeEmailAddress(implicit hostContext: HostContext) = authorise.async { implicit authenticatedRequest =>

    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromHeadersAndSession(authenticatedRequest.headers)
        _submitChangeEmailAddress
    }

    def displayChangeEmailAddressConfirmed(implicit hostContext: HostContext) = authorise.async { implicit authenticatedRequest =>

    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromHeadersAndSession(authenticatedRequest.headers)
        _displayChangeEmailAddressConfirmed
    }

    def displayStopPaperless(implicit hostContext: HostContext) = authorise.async { implicit authenticatedRequest =>

    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromHeadersAndSession(authenticatedRequest.headers)
        _displayStopPaperless
    }

    def submitStopPaperless(implicit hostContext: HostContext) = authorise.async { implicit authenticatedRequest =>
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromHeadersAndSession(authenticatedRequest.headers)

        _submitStopPaperless
    }

    def displayStopPaperlessConfirmed(implicit hostContext: HostContext) = authorise { implicit authenticatedRequest =>

    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromHeadersAndSession(authenticatedRequest.headers)
        _displayStopPaperlessConfirmed
    }

    def resendVerificationEmail(implicit hostContext: HostContext) = authorise.async { implicit authenticatedRequest =>
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromHeadersAndSession(authenticatedRequest.headers)
        _resendVerificationEmail
    }

}

