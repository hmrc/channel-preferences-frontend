/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package partial.paperless.manage

import connectors.PreferenceResponse
import javax.inject.{ Inject, Singleton }
import model.{ Encrypted, HostContext }
import play.api.i18n.Messages
import play.api.mvc.Request
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.emailaddress.EmailAddress

@Singleton
class ManagePaperlessPartial @Inject()(
  digitalFalse: html.digital_false,
  digitalTrueBounced: html.digital_true_bounced,
  digitalTrueVerified: html.digital_true_verified,
  digitalTruePending: html.digital_true_pending) {

  def apply(prefs: Option[PreferenceResponse])(
    implicit request: Request[_],
    hostContext: HostContext,
    messages: Messages): HtmlFormat.Appendable = prefs match {
    case p @ Some(PreferenceResponse(map, Some(email))) if (p.exists(_.genericTermsAccepted)) =>
      (email.hasBounces, email.isVerified) match {
        case (true, _) => digitalTrueBounced(email)
        case (_, true) => digitalTrueVerified(email)
        case _         => digitalTruePending(email)
      }
    case p @ Some(PreferenceResponse(_, email)) =>
      val encryptedEmail = email map (emailPreference => Encrypted(EmailAddress(emailPreference.email)))
      digitalFalse(encryptedEmail)
    case _ => digitalFalse(None)
  }
}
