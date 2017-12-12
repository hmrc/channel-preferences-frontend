package partial.paperless.warnings

import connectors.{EmailPreference, PreferenceResponse}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.Request
import play.twirl.api.HtmlFormat

object PaperlessWarningPartial {
  def apply(prefs: PreferenceResponse, returnUrl: String, returnLinkText: String)(implicit request: Request[_]) = prefs match {
    case PreferenceResponse(_, Some(EmailPreference(_,false, true, mailBoxFull, _))) if prefs.genericTermsAccepted => html.bounced_email(mailBoxFull, returnUrl, returnLinkText)
    case PreferenceResponse(_, Some(email@EmailPreference(_, false, _ , _, _))) if prefs.genericTermsAccepted => html.pending_email_verification(email, returnUrl, returnLinkText)
    case _                                                                              => HtmlFormat.empty
  }
}