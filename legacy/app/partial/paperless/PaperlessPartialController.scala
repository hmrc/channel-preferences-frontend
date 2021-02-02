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

package partial.paperless

import connectors.EntityResolverConnector
import controllers.auth.{ AuthenticatedRequest, WithAuthRetrievals }
import model.HostContext
import partial.paperless.manage.ManagePaperlessPartial
import partial.paperless.warnings.PaperlessWarningPartial
import play.api.i18n.I18nSupport
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PaperlessPartialController @Inject() (
  entityResolverConnector: EntityResolverConnector,
  val authConnector: AuthConnector,
  managePaperlessPartial: ManagePaperlessPartial,
  mcc: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport with WithAuthRetrievals {

  def displayManagePaperlessPartial(implicit returnUrl: HostContext): Action[AnyContent] =
    Action.async { request =>
      withAuthenticatedRequest { implicit authenticatedRequest: AuthenticatedRequest[_] => implicit hc: HeaderCarrier =>
        entityResolverConnector.getPreferences().map { pref =>
          Ok(managePaperlessPartial(pref))
        }
      }(request, ec)
    }

  def displayPaperlessWarningsPartial(implicit hostContext: HostContext): Action[AnyContent] =
    Action.async { request =>
      withAuthenticatedRequest { implicit authenticatedRequest: AuthenticatedRequest[_] => implicit hc: HeaderCarrier =>
        entityResolverConnector.getPreferences().map {
          case None => NotFound
          case Some(prefs) =>
            Ok(PaperlessWarningPartial.apply(prefs, hostContext))
              .withHeaders("X-Opted-In-Email" -> prefs.genericTermsAccepted.toString)
        }
      }(request, ec)
    }
}
