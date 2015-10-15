package controllers.sa.prefs.partial

import connectors.PreferencesConnector
import controllers.sa.prefs.SaRegimeWithoutRedirection
import controllers.sa.prefs.config.Global
import controllers.sa.prefs.internal.PreferencesControllerHelper
import partial.{ManagePaperlessPartial, PaperlessWarningPartial}
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
}

trait PaperlessPartialController
  extends FrontendController
  with Actions
  with PreferencesControllerHelper {

  def preferencesConnector: PreferencesConnector

  val displayManagePaperlessPartial = AuthorisedFor(taxRegime = SaRegimeWithoutRedirection, redirectToOrigin = false).async { authContext => implicit request =>
    preferencesConnector.getPreferences(utr = authContext.principal.accounts.sa.get.utr) map { pref =>
      Ok(ManagePaperlessPartial(pref))
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