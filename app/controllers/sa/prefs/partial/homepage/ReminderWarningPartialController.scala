package controllers.sa.prefs.partial.homepage

import connectors.PreferencesConnector
import controllers.sa.prefs.SaRegimeWithoutRedirection
import controllers.sa.prefs.config.Global
import play.api.mvc.Result
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

object ReminderWarningPartialController extends ReminderWarningPartialController {
  override def preferenceConnector: PreferencesConnector = PreferencesConnector

  override protected implicit def authConnector: AuthConnector = Global.authConnector
}

trait ReminderWarningPartialController
  extends FrontendController with RunMode with Actions with RenderViewForPreferences {

  def preferenceConnector: PreferencesConnector

  def pendingEmailVerification(utr: SaUtr, nino: Option[Nino])(implicit hc: HeaderCarrier): Future[Result] =
    preferenceConnector.getPreferences(utr, nino).map {
      case None => NotFound
      case Some(prefs) => Ok(renderPrefs(prefs)).withHeaders("X-Opted-In-Email" -> prefs.digital.toString)
    }

  def preferencesWarning() = AuthorisedFor(taxRegime = SaRegimeWithoutRedirection, redirectToOrigin = false).async {
    implicit authContext => implicit request =>
      pendingEmailVerification(authContext.principal.accounts.sa.get.utr, authContext.principal.accounts.paye.map(_.nino))
  }
}
