package partial.paperless

import controllers.Authentication
import config.Global
import connectors.EntityResolverConnector
import model.HostContext
import partial.paperless.manage.ManagePaperlessPartial
import partial.paperless.warnings.PaperlessWarningPartial
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.frontend.controller.FrontendController

object PaperlessPartialController extends PaperlessPartialController {
  lazy val auditConnector = Global.auditConnector
  lazy val authConnector = Global.authConnector
  lazy val entityResolverConnector = EntityResolverConnector
}

trait PaperlessPartialController
  extends FrontendController
  with Authentication {

  def entityResolverConnector: EntityResolverConnector

  def displayManagePaperlessPartial(implicit returnUrl: HostContext) = authenticated.async { authContext => implicit request =>
    entityResolverConnector.getPreferences(utr = authContext.principal.accounts.sa.get.utr) map { pref =>
      Ok(ManagePaperlessPartial(prefs = pref))
    }
  }

  def displayPaperlessWarningsPartial(implicit hostContext: HostContext) = authenticated.async { implicit authContext => implicit request =>
    entityResolverConnector.getPreferences(utr = authContext.principal.accounts.sa.get.utr).map {
      case None => NotFound
      case Some(prefs) => Ok(PaperlessWarningPartial.apply(prefs, hostContext.returnUrl, hostContext.returnLinkText)).withHeaders("X-Opted-In-Email" -> prefs.digital.toString)
    }
  }
}