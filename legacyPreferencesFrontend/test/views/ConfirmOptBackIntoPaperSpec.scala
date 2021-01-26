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

package views

import _root_.helpers.{ ConfigHelper, LanguageHelper, TestFixtures }
import org.jsoup.Jsoup
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import uk.gov.hmrc.emailaddress.ObfuscatedEmailAddress
import views.html.confirm_opt_back_into_paper

class ConfirmOptBackIntoPaperSpec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val template = app.injector.instanceOf[confirm_opt_back_into_paper]

  "confirm opt back into paperless template" should {
    "render the correct content in english" in {
      val email = ObfuscatedEmailAddress("a@a.com")
      val document =
        Jsoup.parse(template(email)(engRequest, messagesInEnglish(), TestFixtures.sampleHostContext).toString)

      document.getElementsByTag("title").first().text() mustBe "Confirm how you want to get your tax letters"
      document.getElementsByTag("h1").get(0).text() mustBe "Confirm how you want to get your tax letters"
      document
        .getElementsByTag("p")
        .first()
        .text() mustBe "Save all your online tax letters together in one place. We always email to let you know when you have a new online letter."
      document.getElementById("cancel-link").text() mustBe "Keep online tax letters"
    }

    "render the correct content in welsh" in {
      val email = ObfuscatedEmailAddress("a@a.com")
      val document =
        Jsoup.parse(template(email)(welshRequest, messagesInWelsh(), TestFixtures.sampleHostContext).toString)

      document.getElementsByTag("title").first().text() mustBe "Cadarnhau sut hoffech gael eich llythyrau treth"
      document.getElementsByTag("h1").get(0).text() mustBe "Cadarnhau sut hoffech gael eich llythyrau treth"
      document
        .getElementsByTag("p")
        .first()
        .text() mustBe "Cadwch eich holl lythyrau treth ar-lein gyda’i gilydd mewn un lle. Byddwn bob tro yn anfon e-bost atoch i roi gwybod i chi pan fydd llythyr ar-lein newydd wedi’ch cyrraedd."
      document.getElementById("cancel-link").text() mustBe "Parhau i gael llythyrau treth ar-lein"
    }
  }
}