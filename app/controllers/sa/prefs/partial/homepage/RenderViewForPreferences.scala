package controllers.sa.prefs.partial.homepage

import connectors.{SaEmailPreference, SaPreference}
import play.twirl.api.HtmlFormat
import views.html.sa.prefs.warning.{bounced_email, pending_email_verification}
import SaEmailPreference.Status

trait RenderViewForPreferences {
  def renderPrefs(prefs: Option[SaPreference]): Option[HtmlFormat.Appendable] = prefs collect {
    case SaPreference(_, Some(email@SaEmailPreference(_, Status.pending, _, _, _))) => pending_email_verification(email)
    case SaPreference(_, Some(SaEmailPreference(_, Status.bounced, mailBoxFull, _, _))) => bounced_email(mailBoxFull)
  }
}
