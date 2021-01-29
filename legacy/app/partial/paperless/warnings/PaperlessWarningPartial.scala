/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package partial.paperless.warnings

import connectors.{ EmailPreference, PreferenceResponse }
import model.HostContext
import play.api.i18n.Messages
import play.api.mvc.Request
import play.twirl.api.HtmlFormat

object PaperlessWarningPartial {
  def apply(prefs: PreferenceResponse, hostContext: HostContext)(implicit request: Request[_], messages: Messages) =
    prefs match {
      case PreferenceResponse(_, Some(EmailPreference(_, false, true, mailBoxFull, _, _, _)))
          if prefs.genericTermsAccepted =>
        html.bounced_email(mailBoxFull, hostContext)
      case PreferenceResponse(_, Some(email @ EmailPreference(_, false, _, _, _, _, _)))
          if prefs.genericTermsAccepted =>
        html.pending_email_verification(email, hostContext)
      case _ => HtmlFormat.empty
    }
}
