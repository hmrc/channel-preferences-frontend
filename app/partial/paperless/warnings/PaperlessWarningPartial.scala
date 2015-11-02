package partial.paperless.warnings

import connectors.SaEmailPreference.Status
import connectors.{SaEmailPreference, SaPreference}
import play.twirl.api.HtmlFormat

object PaperlessWarningPartial {
  def apply(prefs: SaPreference) = prefs match {
    case SaPreference(_, Some(email@SaEmailPreference(_, Status.Pending, _, _, _)))     => html.pending_email_verification(email)
    case SaPreference(_, Some(SaEmailPreference(_, Status.Bounced, mailBoxFull, _, _))) => html.bounced_email(mailBoxFull)
    case _                                                                              => HtmlFormat.empty
  }
}