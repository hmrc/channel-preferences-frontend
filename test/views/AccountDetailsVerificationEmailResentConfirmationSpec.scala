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

package views

import _root_.helpers.{ ConfigHelper, LanguageHelper, TestFixtures }
import controllers.auth.AuthenticatedRequest
import org.jsoup.Jsoup
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import org.scalatestplus.play.PlaySpec
import views.html.account_details_verification_email_resent_confirmation

class AccountDetailsVerificationEmailResentConfirmationSpec
    extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val template = app.injector.instanceOf[account_details_verification_email_resent_confirmation]

  "account details verification email resent confirmation template" should {
    "render the correct content in english" in {
      val email = "a@a.com"
      val document =
        Jsoup.parse(template(email)(engRequest, messagesInEnglish(), TestFixtures.sampleHostContext).toString())

      document.getElementsByTag("title").first().text() mustBe "Verification email sent"
      document.getElementsByTag("h1").get(0).text() mustBe "Verification email sent"
      document
        .getElementById("verification-mail-message")
        .text() mustBe s"A new email has been sent to $email. Click on the link in the email to verify your email address with HMRC."
    }

    "render the correct content in welsh" in {
      val email = "a@a.com"
      val document =
        Jsoup.parse(template(email)(welshRequest, messagesInWelsh(), TestFixtures.sampleHostContext).toString())

      document.getElementsByTag("title").first().text() mustBe "E-bost dilysu wedi'i anfon"
      document.getElementsByTag("h1").get(0).text() mustBe "E-bost dilysu wedi'i anfon"
      document
        .getElementById("verification-mail-message")
        .text() mustBe s"Mae e-bost newydd wedi'i anfon i $email. Cliciwch ar y cysylltiad yn yr e-bost er mwyn dilysu'ch cyfeiriad e-bost gyda CThEM."
    }
  }
}
