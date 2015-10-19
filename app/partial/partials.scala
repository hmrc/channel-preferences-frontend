package partial

import connectors.SaEmailPreference.Status
import connectors.{SaEmailPreference, SaPreference}
import hostcontext.HostContext
import play.api.mvc.Request
import play.twirl.api.HtmlFormat
import views.html.sa.prefs.warning.{bounced_email, pending_email_verification}

object ManagePaperlessPartial {
  def apply(prefs: Option[SaPreference])(implicit request: Request[_], returnUrl: HostContext): HtmlFormat.Appendable = prefs match {
    case Some(SaPreference(true, Some(email))) => views.html.partial.managepaperless.digital_true(email)
    case _                                     => views.html.partial.managepaperless.digital_false()
  }
}

object PaperlessWarningPartial {
  def apply(prefs: SaPreference): HtmlFormat.Appendable = prefs match {
    case SaPreference(_, Some(email@SaEmailPreference(_, Status.pending, _, _, _))) => pending_email_verification(email)
    case SaPreference(_, Some(SaEmailPreference(_, Status.bounced, mailBoxFull, _, _))) => bounced_email(mailBoxFull)
    case _ => HtmlFormat.empty
  }
}