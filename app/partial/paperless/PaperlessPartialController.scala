/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package partial.paperless

import connectors.EntityResolverConnector
import controllers.auth.{ AuthenticatedRequest, WithAuthRetrievals }
import javax.inject.Inject
import model.HostContext
import partial.paperless.manage.ManagePaperlessPartial
import partial.paperless.warnings.PaperlessWarningPartial
import play.api.i18n.I18nSupport
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.ExecutionContext

class PaperlessPartialController @Inject()(
  entityResolverConnector: EntityResolverConnector,
  val authConnector: AuthConnector,
  managePaperlessPartial: ManagePaperlessPartial,
  mcc: MessagesControllerComponents)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport with WithAuthRetrievals {

  def displayManagePaperlessPartial(implicit returnUrl: HostContext): Action[AnyContent] = Action.async { request =>
    withAuthenticatedRequest { implicit authenticatedRequest: AuthenticatedRequest[_] => implicit hc: HeaderCarrier =>
      entityResolverConnector.getPreferences().map { pref =>
        Ok(managePaperlessPartial(pref))
      }
    }(request, ec)
  }

  def displayPaperlessWarningsPartial(implicit hostContext: HostContext): Action[AnyContent] = Action.async { request =>
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
