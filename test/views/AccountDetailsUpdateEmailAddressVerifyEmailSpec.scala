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
import org.scalatestplus.play.PlaySpec
import views.html.account_details_update_email_address_verify_email

class AccountDetailsUpdateEmailAddressVerifyEmailSpec
    extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val template = app.injector.instanceOf[account_details_update_email_address_verify_email]

  "account details update email address verify email template" should {
    "render the correct content in english" in {
      val email = "a@a.com"
      val document =
        Jsoup.parse(template(email)(engRequest, messagesInEnglish(), TestFixtures.sampleHostContext).toString())

      document.getElementsByTag("title").first().text() mustBe "Check your email address"
      document.getElementsByTag("h1").get(0).text() mustBe "Check your email address"
      document.getElementsByTag("p").get(1).text() mustBe s"Are you sure $email is correct?"
      document.getElementById("emailIsCorrectLink").text() mustBe "This email address is correct"
      document.getElementById("emailIsNotCorrectLink").text() mustBe "Change this email address"
    }

    "render the correct content in welsh" in {
      val email = "a@a.com"
      val document =
        Jsoup.parse(template(email)(welshRequest, messagesInWelsh(), TestFixtures.sampleHostContext).toString())

      document.getElementsByTag("title").first().text() mustBe "Gwirio'ch cyfeiriad e-bost"
      document.getElementsByTag("h1").get(0).text() mustBe "Gwirio'ch cyfeiriad e-bost"
      document.getElementsByTag("p").get(1).text() mustBe s"A ydych yn siŵr bod $email yn gywir?"
      document.getElementById("emailIsCorrectLink").text() mustBe "Mae'r cyfeiriad e-bost hwn yn gywir"
      document.getElementById("emailIsNotCorrectLink").text() mustBe "Newid y cyfeiriad e-bost hwn"
    }
  }
}
