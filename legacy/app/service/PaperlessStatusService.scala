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

package service
import com.google.inject.ImplementedBy
import connectors.{ EmailPreference, PreferenceResponse }
import model.StatusName.{ Alright, BouncedEmail, EmailNotVerified, NewCustomer, NoEmail, Paper }
import model._

@ImplementedBy(classOf[PreferencesBasedStatusService])
trait PaperlessStatusService {

  def determineStatus(preference: Option[PreferenceResponse]): StatusName

}

class PreferencesBasedStatusService extends PaperlessStatusService {

  def determineStatus(preference: Option[PreferenceResponse]): StatusName =
    preference match {
      case Some(p @ PreferenceResponse(_, Some(email))) if p.genericTermsAccepted =>
        determinePaperlessStatus(email)
      case Some(p @ PreferenceResponse(_, None)) if p.genericTermsAccepted =>
        NoEmail
      case Some(p: PreferenceResponse) if !p.genericTermsAccepted =>
        Paper
      case _ => NewCustomer
    }

  private def determinePaperlessStatus(email: EmailPreference): StatusName =
    if (email.hasBounces)
      BouncedEmail
    else if (!email.isVerified)
      EmailNotVerified
    else
      Alright
}
