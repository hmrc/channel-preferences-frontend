/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package partial.paperless.warnings

import connectors.{ EmailPreference, PreferenceResponse }
import play.api.i18n.Messages
import play.api.mvc.Request
import play.twirl.api.HtmlFormat

import model.HostContext

object PaperlessWarningPartial {
  def apply(prefs: PreferenceResponse, hostContext: HostContext)(implicit request: Request[_], messages: Messages) =
    prefs match {
      case PreferenceResponse(_, Some(EmailPreference(_, false, true, mailBoxFull, _, _)))
          if prefs.genericTermsAccepted =>
        html.bounced_email(mailBoxFull, hostContext)
      case PreferenceResponse(_, Some(email @ EmailPreference(_, false, _, _, _, _))) if prefs.genericTermsAccepted =>
        html.pending_email_verification(email, hostContext)
      case _ => HtmlFormat.empty
    }
}
