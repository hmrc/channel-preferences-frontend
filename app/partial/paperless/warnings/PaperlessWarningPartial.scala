package partial.paperless.warnings

import connectors.SaEmailPreference.Status
import connectors.{SaEmailPreference, SaPreference}
import play.twirl.api.HtmlFormat
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

object PaperlessWarningPartial {
  def apply(prefs: SaPreference, returnUrl: String, returnLinkText: String) = prefs match {
    case SaPreference(_, Some(email@SaEmailPreference(_, Status.Pending, _, _, _)))     => html.pending_email_verification(email, returnUrl, returnLinkText)
    case SaPreference(_, Some(SaEmailPreference(_, Status.Bounced, mailBoxFull, _, _))) => html.bounced_email(mailBoxFull, returnUrl, returnLinkText)
    case _                                                                              => HtmlFormat.empty
  }
}