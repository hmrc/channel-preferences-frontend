/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers.external

import connectors._
import controllers.ExternalUrlPrefixes
import controllers.auth.AuthenticatedRequest
import model.HostContext
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

class EmailValidationController @Inject() (
  entityResolverConnector: EntityResolverConnector,
  val authConnector: AuthConnector,
  externalUrlPrefixes: ExternalUrlPrefixes,
  saPrintingPreferenceExpiredEmail: views.html.sa.prefs.sa_printing_preference_expired_email,
  saPrintingPreferenceVerifyEmailFailed: views.html.sa.prefs.sa_printing_preference_verify_email_failed,
  saPrintingPreferenceVerifyEmail: views.html.sa.prefs.sa_printing_preference_verify_email,
  saPrintingPreferenceWrongToken: views.html.sa.prefs.sa_printing_preference_wrong_token,
  mcc: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) {

  val regex = "([0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[0-9a-f]{4}-[0-9a-f]{12})".r

  def verify(token: String): Action[AnyContent] =
    Action.async { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers)
      implicit val nonAuthenticatedRequest = AuthenticatedRequest(request, None, None, None, None)
      implicit val hostContext: HostContext = new HostContext(returnUrl = "", returnLinkText = "")
      token match {
        case regex(_) =>
          entityResolverConnector.updateEmailValidationStatusUnsecured(token) map {
            case Validated => Ok(saPrintingPreferenceVerifyEmail(None, None))
            case ValidatedWithReturn(returnText, returnUrl) =>
              Ok(saPrintingPreferenceVerifyEmail(Some(returnUrl), Some(returnText)))
            case ValidationExpired => Ok(saPrintingPreferenceExpiredEmail())
            case WrongToken        => Ok(saPrintingPreferenceWrongToken())
            case ValidationErrorWithReturn(returnLinkText, returnUrl) =>
              BadRequest(saPrintingPreferenceVerifyEmailFailed(Some(returnUrl), Some(returnLinkText)))
            case ValidationError =>
              BadRequest(saPrintingPreferenceVerifyEmailFailed(None, None))
          }
        case _ =>
          Future.successful(BadRequest(saPrintingPreferenceVerifyEmailFailed(None, None)))
      }
    }
}
