package partial.paperless

import config.Global
import connectors.EntityResolverConnector
import model.HostContext
import partial.paperless.manage.ManagePaperlessPartial
import partial.paperless.warnings.PaperlessWarningPartial
import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import controllers.auth.{AuthAction, AuthController}

object PaperlessPartialController extends PaperlessPartialController {
  lazy val auditConnector = Global.auditConnector
  lazy val  authCOnnector = Global.authConnector
  lazy val entityResolverConnector = EntityResolverConnector
}

trait PaperlessPartialController
  extends FrontendController {

  def authorise:AuthAction = AuthController

  def entityResolverConnector: EntityResolverConnector

  def displayManagePaperlessPartial(implicit returnUrl: HostContext) = authorise.async { implicit request =>
    entityResolverConnector.getPreferences() map { pref =>
      Ok(ManagePaperlessPartial(prefs = pref))
    }
  }

  def displayPaperlessWarningsPartial(implicit hostContext: HostContext) = authorise.async { implicit request =>
    entityResolverConnector.getPreferences().map {
      case None => NotFound
      case Some(prefs) => Ok(PaperlessWarningPartial.apply(prefs, hostContext.returnUrl, hostContext.returnLinkText)).withHeaders("X-Opted-In-Email" -> prefs.genericTermsAccepted.toString)
    }
  }
}
