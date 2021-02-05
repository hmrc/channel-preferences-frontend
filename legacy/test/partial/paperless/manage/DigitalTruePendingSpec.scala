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

package partial.paperless.manage

import _root_.helpers.{ ConfigHelper, LanguageHelper, TestFixtures }
import connectors.EmailPreference
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import partial.paperless.manage.html.digital_true_pending
import play.api.Application
import play.api.test.FakeRequest
import views.sa.prefs.helpers.DateFormat

class DigitalTruePendingSpec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val template = app.injector.instanceOf[digital_true_pending]

  "digital true pending partial" should {
    "render the correct content in english" in {
      val emailAddress = "a@a.com"
      val localDate = new LocalDate("2017-12-01")
      val formattedLocalDate = DateFormat.longDateFormat(Some(localDate))
      val email = EmailPreference(emailAddress, true, true, false, Some(localDate))
      val document =
        Jsoup.parse(template(email)(FakeRequest(), messagesInEnglish(), TestFixtures.sampleHostContext).toString())

      document.getElementById("saEmailRemindersHeader").text() mustBe "Email for paperless notifications"
      document.getElementsByTag("p").get(0).text() mustBe "You need to verify your email address."
      document.getElementById("pending-status-message").childNodes().get(0).toString() mustBe "An email was sent to "
      document
        .getElementById("pending-status-message")
        .childNodes()
        .get(1)
        .childNodes()
        .get(0)
        .toString() mustBe emailAddress
      document
        .getElementById("pending-status-message")
        .childNodes()
        .get(2)
        .toString() mustBe s" on ${formattedLocalDate.get}. Click on the link in the email to verify your email address."
      document.getElementsByTag("p").get(2).childNodes().get(0).toString() mustBe " If you can't find it, we can "
      document.getElementById("resend-email-button").text() mustBe "Send a new verification email"
    }

    "render the correct content in welsh" in {
      val emailAddress = "a@a.com"
      val localDate = new LocalDate("2017-12-01")
      val formattedLocalDate = DateFormat.longDateFormat(Some(localDate))
      val email = EmailPreference(emailAddress, true, true, false, Some(localDate))
      val document =
        Jsoup.parse(template(email)(welshRequest, messagesInWelsh(), TestFixtures.sampleHostContext).toString())

      document.getElementById("saEmailRemindersHeader").text() mustBe "E-bost ar gyfer hysbysiadau di-bapur"
      document.getElementsByTag("p").get(0).text() mustBe "Mae angen i chi ddilysuch cyfeiriad e-bost."
      document.getElementById("pending-status-message").childNodes().get(0).toString() mustBe "Anfonwyd e-bost at "
      document
        .getElementById("pending-status-message")
        .childNodes()
        .get(1)
        .childNodes()
        .get(0)
        .toString() mustBe emailAddress
      document
        .getElementById("pending-status-message")
        .childNodes()
        .get(2)
        .toString() mustBe s" ar ${formattedLocalDate.get}. Cliciwch ar y cysylltiad yn yr e-bost er mwyn dilysu'ch cyfeiriad e-bost."
      document.getElementsByTag("p").get(2).childNodes().get(0).toString() mustBe " Os na allwch ddod o hyd iddo, "
      document.getElementById("resend-email-button").text() mustBe "Gallwch gael e-bost newydd wedi'i anfon atoch o"
    }
  }
}
