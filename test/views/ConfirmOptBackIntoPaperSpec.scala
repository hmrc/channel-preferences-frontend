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

package views

import _root_.helpers.{ ConfigHelper, TestFixtures, LanguageHelper }
import controllers.auth.AuthenticatedRequest
import org.jsoup.Jsoup
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import uk.gov.hmrc.emailaddress.ObfuscatedEmailAddress
import org.scalatestplus.play.PlaySpec
import views.html.confirm_opt_back_into_paper

class ConfirmOptBackIntoPaperSpec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val template = app.injector.instanceOf[confirm_opt_back_into_paper]

  "confirm opt back into paperless template" should {
    "render the correct content in english" in {
      val email = ObfuscatedEmailAddress("a@a.com")
      val document =
        Jsoup.parse(template(email)(engRequest, messagesInEnglish(), TestFixtures.sampleHostContext).toString())

      document.getElementsByTag("title").first().text() mustBe "Stop notifications"
      document.getElementsByTag("h1").get(0).text() mustBe "Stop notifications"
      document.getElementsByTag("p").get(1).text() mustBe "You'll get letters again, instead of emails."
      document.getElementById("cancel-link").text() mustBe "Cancel"
    }

    "render the correct content in welsh" in {
      val email = ObfuscatedEmailAddress("a@a.com")
      val document =
        Jsoup.parse(template(email)(welshRequest, messagesInWelsh(), TestFixtures.sampleHostContext).toString())

      document.getElementsByTag("title").first().text() mustBe "Atal hysbysiadau"
      document.getElementsByTag("h1").get(0).text() mustBe "Atal hysbysiadau"
      document
        .getElementsByTag("p")
        .get(1)
        .text() mustBe "Byddwch yn cael llythyrau unwaith eto, yn hytrach nag e-byst."
      document.getElementById("cancel-link").text() mustBe "Canslo"
    }
  }
}
