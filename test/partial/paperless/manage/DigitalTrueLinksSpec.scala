/*
 * Copyright 2019 HM Revenue & Customs
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
import org.jsoup.Jsoup
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import org.scalatestplus.play.PlaySpec
import html.digital_true_links
import play.api.test.FakeRequest

class DigitalTrueLinksSpec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val template = app.injector.instanceOf[digital_true_links]

  "digital true links partial" should {
    "render the correct content in english" in {
      val emailAddress = "a@a.com"
      val linkId = "test_id"
      val email = EmailPreference(emailAddress, true, true, false, None)
      val document = Jsoup.parse(
        template(email, linkId)(FakeRequest(), messagesInEnglish(), TestFixtures.sampleHostContext).toString())

      document.getElementById(linkId).text() mustBe "Change your email address"
      document.getElementById("opt-out-of-email-link").text() mustBe "Stop emails from HMRC"
    }

    "render the correct content in welsh" in {
      val emailAddress = "a@a.com"
      val linkId = "test_id"
      val email = EmailPreference(emailAddress, true, true, false, None)
      val document =
        Jsoup.parse(template(email, linkId)(welshRequest, messagesInWelsh(), TestFixtures.sampleHostContext).toString())

      document.getElementById(linkId).text() mustBe "Newid eich cyfeiriad e-bost"
      document.getElementById("opt-out-of-email-link").text() mustBe "Atal e-byst gan CThEM"
    }
  }
}
