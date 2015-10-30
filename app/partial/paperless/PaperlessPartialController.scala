package partial.paperless

import authentication.SaRegimeWithoutRedirection
import config.Global
import connectors.PreferencesConnector
import model.HostContext
import partial.paperless.manage.ManagePaperlessPartial
import partial.paperless.warnings.PaperlessWarningPartial
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

object PaperlessPartialController extends PaperlessPartialController {
  lazy val auditConnector = Global.auditConnector
  lazy val authConnector = Global.authConnector
  lazy val preferencesConnector = PreferencesConnector
}

// FIXME remove when YTA no longer use these endpoints
object PaperlessPartialsForDeprecatedYTAEndpointsController extends PaperlessPartialController {
  lazy val auditConnector = Global.auditConnector
  lazy val authConnector = Global.authConnector
  lazy val preferencesConnector = PreferencesConnector

  val displayManagePaperlessPartial: Action[AnyContent] = displayManagePaperlessPartial(HostContext.defaultsForYtaManageAccountPages)

  val displayPaperlessWarningsPartial: Action[AnyContent] = displayPaperlessWarningsPartial(HostContext.defaultsForYtaWarningsPartial)
}

trait PaperlessPartialController
  extends FrontendController
  with Actions {

  def preferencesConnector: PreferencesConnector

  def displayManagePaperlessPartial(implicit returnUrl: HostContext) = AuthorisedFor(taxRegime = SaRegimeWithoutRedirection, redirectToOrigin = false).async { authContext => implicit request =>
    preferencesConnector.getPreferences(utr = authContext.principal.accounts.sa.get.utr) map { pref =>
      Ok(ManagePaperlessPartial(prefs = pref))
    }
  }

  def displayPaperlessWarningsPartial(implicit hostContext: HostContext) = AuthorisedFor(taxRegime = SaRegimeWithoutRedirection, redirectToOrigin = false).async { implicit authContext => implicit request =>
    def pendingEmailVerification(utr: SaUtr, nino: Option[Nino])(implicit hc: HeaderCarrier) = preferencesConnector.getPreferences(utr, nino).map {
      case None => NotFound
      case Some(prefs) => Ok(PaperlessWarningPartial.apply(prefs, hostContext.returnUrl, hostContext.returnLinkText)).withHeaders("X-Opted-In-Email" -> prefs.digital.toString)
    }
    pendingEmailVerification(utr = authContext.principal.accounts.sa.get.utr, nino = authContext.principal.accounts.paye.map(_.nino))
  }
}