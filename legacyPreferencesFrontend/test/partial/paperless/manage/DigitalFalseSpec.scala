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
import org.jsoup.Jsoup
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import partial.paperless.manage.html.digital_false
import play.api.Application

class DigitalFalseSpec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val template = app.injector.instanceOf[digital_false]

  "digital false partial" should {
    "render the correct content in english" in {
      val document = Jsoup.parse(template(None)(TestFixtures.sampleHostContext, messagesInEnglish()).toString)

      document.getElementById("saEmailRemindersHeader").text() mustBe "Go paperless"
      document
        .getElementById("opt-out-status-message")
        .text() mustBe "Replace the letters you get about taxes with emails."
      document.getElementById("opt-in-to-digital-email-link").text() mustBe "Sign up for paperless notifications"
    }

    "render the correct content in welsh" in {
      val document = Jsoup.parse(template(None)(TestFixtures.sampleHostContext, messagesInWelsh()).toString)

      document.getElementById("saEmailRemindersHeader").text() mustBe "Ewch yn ddi-bapur"
      document
        .getElementById("opt-out-status-message")
        .text() mustBe "Cael e-byst, yn lle'r llythyrau a gewch, ynghylch trethi."
      document.getElementById("opt-in-to-digital-email-link").text() mustBe "Cofrestrwch ar gyfer hysbysiadau di-bapur"
    }
  }
}
