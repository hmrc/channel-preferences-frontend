/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.external

import connectors._
import controllers.ExternalUrlPrefixes
import controllers.auth.AuthenticatedRequest
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

class EmailValidationController @Inject()(
  entityResolverConnector: EntityResolverConnector,
  val authConnector: AuthConnector,
  externalUrlPrefixes: ExternalUrlPrefixes,
  saPrintingPreferenceExpiredEmail: views.html.sa.prefs.sa_printing_preference_expired_email,
  saPrintingPreferenceVerifyEmailFailed: views.html.sa.prefs.sa_printing_preference_verify_email_failed,
  saPrintingPreferenceVerifyEmail: views.html.sa.prefs.sa_printing_preference_verify_email,
  saPrintingPreferenceWrongToken: views.html.sa.prefs.sa_printing_preference_wrong_token,
  mcc: MessagesControllerComponents)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) {

  val regex = "([0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[0-9a-f]{4}-[0-9a-f]{12})".r

  def verify(token: String): Action[AnyContent] = Action.async { implicit request =>
    {
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers)
      implicit val nonAuthenticatedRequest = AuthenticatedRequest(request, None, None, None, None)
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
}
