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

import _root_.helpers.{ ConfigHelper, LanguageHelper, TestFixtures }
import connectors.EmailPreference
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import partial.paperless.warnings.html.pending_email_verification
import play.api.Application
import play.api.data.FormError
import play.api.test.FakeRequest
import views.sa.prefs.helpers.DateFormat

class PendingEmailVerificationSpec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp

  "pending email verification partial" should {
    "render the correct content in english" in {
      val emailAddress = "a@a.com"
      val localDate = new LocalDate("2017-12-01")
      val formattedLocalDate = DateFormat.longDateFormat(Some(localDate))
      val emailPreference = EmailPreference(emailAddress, true, true, false, Some(localDate))
      val errors = Seq((FormError("ErrorKey", Seq("Error Message"), Seq()), "Outer Error Message"))
      val document = Jsoup.parse(
        pending_email_verification(emailPreference, TestFixtures.sampleHostContext)(FakeRequest(), messagesInEnglish()).toString
      )

      document
        .getElementsByTag("summary")
        .first()
        .childNode(0)
        .toString mustBe "Verify your email address for paperless notifications"
      document.getElementsByClass("flag--urgent").first().text() mustBe "Now"
      document
        .getElementsByTag("p")
        .get(0)
        .text() mustBe s"An email was sent to $emailAddress on ${formattedLocalDate.get}. Click on the link in the email to verify your email address with HMRC."
      document
        .getElementsByTag("p")
        .get(1)
        .childNode(0)
        .toString mustBe "If you can't find it you can get a new email sent to you from "
    }

    "render the correct content in welsh" in {
      val emailAddress = "a@a.com"
      val localDate = new LocalDate("2017-12-01")
      val formattedLocalDate = DateFormat.longDateFormat(Some(localDate))
      val emailPreference = EmailPreference(emailAddress, true, true, false, Some(localDate))
      val errors = Seq((FormError("ErrorKey", Seq("Error Message"), Seq()), "Outer Error Message"))
      val document = Jsoup.parse(
        pending_email_verification(emailPreference, TestFixtures.sampleHostContext)(welshRequest, messagesInWelsh()).toString
      )

      document
        .getElementsByTag("summary")
        .first()
        .childNode(0)
        .toString mustBe "Dilyswch eich cyfeiriad e-bost ar gyfer hysbysiadau di-bapur"
      document.getElementsByClass("flag--urgent").first().text() mustBe "Nawr"
      document
        .getElementsByTag("p")
        .get(0)
        .text() mustBe s"Anfonwyd e-bost at $emailAddress ar ${formattedLocalDate.get}. Cliciwch ar y cysylltiad yn yr e-bost er mwyn dilysu'ch cyfeiriad e-bost gyda CThEM."
      document
        .getElementsByTag("p")
        .get(1)
        .childNode(0)
        .toString mustBe "Os na allwch ddod o hyd iddo, gallwch gael e-bost newydd wedi'i anfon atoch o "
    }
  }
}
