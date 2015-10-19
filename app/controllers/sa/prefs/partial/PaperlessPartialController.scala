package controllers.sa.prefs.partial

import connectors.PreferencesConnector
import controllers.sa.prefs.{ExternalUrls, SaRegimeWithoutRedirection}
import controllers.sa.prefs.config.Global
import controllers.sa.prefs.internal.PreferencesControllerHelper
import hostcontext.HostContext
import partial.{ManagePaperlessPartial, PaperlessWarningPartial}
import play.api.mvc.{AnyContent, Action}
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

  val displayManagePaperlessPartial: Action[AnyContent] = displayManagePaperlessPartial(HostContext(ExternalUrls.businessTaxHome))
}

trait PaperlessPartialController
  extends FrontendController
  with Actions
  with PreferencesControllerHelper {

  def preferencesConnector: PreferencesConnector

  def displayManagePaperlessPartial(implicit returnUrl: HostContext) = AuthorisedFor(taxRegime = SaRegimeWithoutRedirection, redirectToOrigin = false).async { authContext => implicit request =>
    preferencesConnector.getPreferences(utr = authContext.principal.accounts.sa.get.utr) map { pref =>
      Ok(ManagePaperlessPartial(prefs = pref))
    }
  }

  val displayPaperlessWarningsPartial = AuthorisedFor(taxRegime = SaRegimeWithoutRedirection, redirectToOrigin = false).async { implicit authContext => implicit request =>
    def pendingEmailVerification(utr: SaUtr, nino: Option[Nino])(implicit hc: HeaderCarrier) = preferencesConnector.getPreferences(utr, nino).map {
      case None => NotFound
      case Some(prefs) => Ok(PaperlessWarningPartial.apply(prefs)).withHeaders("X-Opted-In-Email" -> prefs.digital.toString)
    }
    pendingEmailVerification(utr = authContext.principal.accounts.sa.get.utr, nino = authContext.principal.accounts.paye.map(_.nino))
  }
}