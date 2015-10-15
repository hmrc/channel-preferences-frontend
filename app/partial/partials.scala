package partial

import connectors.SaEmailPreference.Status
import connectors.{SaEmailPreference, SaPreference}
import play.api.mvc.Request
import play.twirl.api.HtmlFormat
import views.html.sa.prefs.email.{digital_false, digital_true}
import views.html.sa.prefs.warning.{bounced_email, pending_email_verification}

object ManagePaperlessPartial {
  def apply(prefs: Option[SaPreference])(implicit request: Request[_]): HtmlFormat.Appendable = prefs match {
    case Some(SaPreference(true, Some(email))) => digital_true(email)
    case _ => digital_false()
  }
}

object PaperlessWarningPartial {
  def apply(prefs: SaPreference): HtmlFormat.Appendable = prefs match {
    case SaPreference(_, Some(email@SaEmailPreference(_, Status.pending, _, _, _))) => pending_email_verification(email)
    case SaPreference(_, Some(SaEmailPreference(_, Status.bounced, mailBoxFull, _, _))) => bounced_email(mailBoxFull)
    case _ => HtmlFormat.empty
  }
}