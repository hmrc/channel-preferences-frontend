package controllers.sa.prefs.partial.homepage

import connectors.{SaEmailPreference, SaPreference}
import play.api.mvc.Results
import play.twirl.api.HtmlFormat
import views.html.sa.prefs.warning.{bounced_email, pending_email_verification}

trait RenderViewForPreferences extends Results {
  def renderPrefs(prefs: Option[SaPreference]): Option[HtmlFormat.Appendable] = prefs match {
    case Some(SaPreference(_, Some(email))) if email.status == SaEmailPreference.Status.pending => Some(pending_email_verification(email))
    case Some(SaPreference(_, Some(SaEmailPreference(_,status,true,_,_)))) if status == SaEmailPreference.Status.bounced => Some(bounced_email(true))
    case Some(SaPreference(_, Some(SaEmailPreference(_,status,false,_,_)))) if status == SaEmailPreference.Status.bounced => Some(bounced_email(false))
    case _ => None
  }
}
