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
import views.html.account_details_update_email_address_thank_you

class AccountDetailsUpdateEmailAddressThankYouSpec
    extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val template = app.injector.instanceOf[account_details_update_email_address_thank_you]

  "account details update emaill address thank you template" should {
    "render the correct content in english" in {
      val currentEmail = ObfuscatedEmailAddress("a@a.com")
      val document =
        Jsoup.parse(template(currentEmail)(engRequest, messagesInEnglish(), TestFixtures.sampleHostContext).toString)

      document.getElementsByTag("title").first().text() mustBe "Verify your new email address"
      document.getElementsByTag("h1").get(0).text() mustBe "Verify your new email address"
      document.getElementsByTag("p").get(0).childNodes().get(0).toString mustBe "An email has been sent to "
      document
        .getElementById("verification-mail-message")
        .text() mustBe "Click on the link in the email to verify the address."
      document
        .getElementsByTag("p")
        .get(2)
        .text() mustBe "Until you do this, emails will continue to go to your old address."
      document
        .getElementById("return-to-dashboard-button")
        .attr(
          "href"
        ) mustBe "/paperless/check-settings?returnUrl=kvXgJfoJJ%2FbmaHgdHhhRpg%3D%3D&returnLinkText=huhgy5odc6KaXfFIMZXkeZjs11wvNGxKPz2CtY8L8GM%3D"
      document
        .getElementById("return-to-dashboard-button")
        .text() mustBe "Continue"
    }

    "render the correct content in welsh" in {
      val currentEmail = ObfuscatedEmailAddress("a@a.com")
      val document =
        Jsoup.parse(template(currentEmail)(welshRequest, messagesInWelsh(), TestFixtures.sampleHostContext).toString)

      document.getElementsByTag("title").first().text() mustBe "Dilysu'ch cyfeiriad e-bost newydd"
      document.getElementsByTag("h1").get(0).text() mustBe "Dilysu'ch cyfeiriad e-bost newydd"
      document.getElementsByTag("p").get(0).childNodes().get(0).toString mustBe "Anfonwyd e-bost at "
      document
        .getElementById("verification-mail-message")
        .text() mustBe "Cliciwch ar y cysylltiad yn yr e-bost er mwyn dilysu'r cyfeiriad."
      document
        .getElementsByTag("p")
        .get(2)
        .text() mustBe "Hyd nes eich bod yn gwneud hyn, bydd e-byst yn dal i gael eu hanfon i'ch hen gyfeiriad"
      document
        .getElementById("return-to-dashboard-button")
        .attr(
          "href"
        ) mustBe "/paperless/check-settings?returnUrl=kvXgJfoJJ%2FbmaHgdHhhRpg%3D%3D&returnLinkText=huhgy5odc6KaXfFIMZXkeZjs11wvNGxKPz2CtY8L8GM%3D"
      document
        .getElementById("return-to-dashboard-button")
        .text() mustBe "Yn eich blaen"
    }
  }
}
