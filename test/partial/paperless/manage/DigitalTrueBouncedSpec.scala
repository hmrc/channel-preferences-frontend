/*
 * Copyright 2020 HM Revenue & Customs
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
import html.digital_true_bounced
import play.api.test.FakeRequest

class DigitalTrueBouncedSpec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val template = app.injector.instanceOf[digital_true_bounced]

  "digital true bounced partial" should {
    "render the correct content in english when the mailbox is not full" in {
      val emailAddress = "a@a.com"
      val email = EmailPreference(emailAddress, true, true, false, None)
      val document =
        Jsoup.parse(template(email)(FakeRequest(), messagesInEnglish(), TestFixtures.sampleHostContext).toString())

      document.getElementById("bouncing-status-message").text() mustBe s"You need to verify $emailAddress"
      document.getElementById("bounce-reason").text() mustBe "The email telling you how to do this can't be delivered."
      document.getElementsByTag("p").get(2).text() mustBe "Use a different email address"
    }

    "render the correct content in english when the mailbox is full" in {
      val emailAddress = "b@b.com"
      val email = EmailPreference(emailAddress, true, true, true, None)
      val document =
        Jsoup.parse(template(email)(FakeRequest(), messagesInEnglish(), TestFixtures.sampleHostContext).toString())

      document
        .getElementById("bounce-reason")
        .text() mustBe "The email telling you how to do this can't be sent because your inbox is full."
      document.getElementById("bounce-reason-more").text() mustBe "Clear your inbox or use a different email address."
    }

    "render the correct content in welsh when the mailbox is not full" in {
      val emailAddress = "a@a.com"
      val email = EmailPreference(emailAddress, true, true, false, None)
      val document =
        Jsoup.parse(template(email)(welshRequest, messagesInWelsh(), TestFixtures.sampleHostContext).toString())

      document.getElementById("bouncing-status-message").text() mustBe s"Mae angen i chi ddilysu $emailAddress"
      document
        .getElementById("bounce-reason")
        .text() mustBe "Ni all yr e-bost sy'n rhoi gwybod i chi sut i wneud hyn gyrraedd pen ei daith."
      document.getElementsByTag("p").get(2).text() mustBe "Defnyddiwch gyfeiriad e-bost gwahanol"
    }

    "render the correct content in welsh when the mailbox is full" in {
      val emailAddress = "b@b.com"
      val email = EmailPreference(emailAddress, true, true, true, None)
      val document =
        Jsoup.parse(template(email)(welshRequest, messagesInWelsh(), TestFixtures.sampleHostContext).toString())

      document
        .getElementById("bounce-reason")
        .text() mustBe "Ni ellir anfon yr e-bost sy'n rhoi gwybod i chi sut i wneud hyn oherwydd bod eich mewnflwch yn llawn."
      document
        .getElementById("bounce-reason-more")
        .text() mustBe "Cliriwch eich mewnflwch neu defnyddiwch gyfeiriad e-bost gwahanol."
    }
  }
}
