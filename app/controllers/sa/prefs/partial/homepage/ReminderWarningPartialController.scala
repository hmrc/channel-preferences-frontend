package controllers.sa.prefs.partial.homepage

import connectors.PreferencesConnector
import controllers.common.BaseController
import controllers.sa.prefs.SaRegimeWithoutRedirection
import play.api.mvc.Result
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.auth.frontend.connectors.AuthConnector
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.frontend.auth.Actions

import scala.concurrent.Future

object ReminderWarningPartialController extends ReminderWarningPartialController {
  override def preferenceConnector: PreferencesConnector = PreferencesConnector

  override protected implicit def authConnector: AuthConnector = AuthConnector
}

trait ReminderWarningPartialController
  extends BaseController with RunMode with Actions with RenderViewForPreferences {

  def preferenceConnector: PreferencesConnector

  def pendingEmailVerification(utr: SaUtr)(implicit hc: HeaderCarrier): Future[Result] =
    preferenceConnector.getPreferences(utr).map {
      case None => NotFound
      case Some(prefs) => Ok(renderPrefs(prefs)).withHeaders("X-Opted-In-Email" -> prefs.digital.toString)
    }

  def preferencesWarning() = AuthorisedFor(regime = SaRegimeWithoutRedirection, redirectToOrigin = false).async {
    implicit user => implicit request => pendingEmailVerification(user.userAuthority.accounts.sa.get.utr)
  }
}
