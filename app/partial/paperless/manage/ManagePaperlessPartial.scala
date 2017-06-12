package partial.paperless.manage

import connectors.NewPreferenceResponse
import model.HostContext
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.Request
import play.twirl.api.HtmlFormat

object ManagePaperlessPartial {
  def apply(prefs: Option[NewPreferenceResponse])(implicit request: Request[_], hostContext: HostContext): HtmlFormat.Appendable = prefs match {
    case p@Some(NewPreferenceResponse(map, Some(email))) if (p.fold(false)(_.genericTermsAccepted)) => (email.hasBounces, email.isVerified) match {
      case (true, _) => html.digital_true_bounced(email)
      case (_, true) => html.digital_true_verified(email)
      case _ => html.digital_true_pending(email)
    }
    case _ => html.digital_false()
  }
}
