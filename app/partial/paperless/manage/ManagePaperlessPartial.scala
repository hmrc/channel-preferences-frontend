package partial.paperless.manage

import connectors.PreferenceResponse
import model.{Encrypted, HostContext}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.Request
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.emailaddress.EmailAddress

object ManagePaperlessPartial {
  def apply(prefs: Option[PreferenceResponse])(implicit request: Request[_], hostContext: HostContext): HtmlFormat.Appendable = prefs match {
    case p@Some(PreferenceResponse(map, Some(email))) if (p.exists(_.genericTermsAccepted)) => (email.hasBounces, email.isVerified) match {
      case (true, _) => html.digital_true_bounced(email)
      case (_, true) => html.digital_true_verified(email)
      case _ => html.digital_true_pending(email)
    }
    case p@Some(PreferenceResponse(_, email)) =>
      val encryptedEmail = email map (emailPreference => Encrypted(EmailAddress(emailPreference.email)))
      html.digital_false(encryptedEmail)
    case _ => html.digital_false(None)
  }
}
