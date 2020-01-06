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

package views.includes

import controllers.auth.AuthenticatedRequest
import helpers.{ ConfigHelper, LanguageHelper }
import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import org.scalatestplus.play.PlaySpec
import views.html.includes.last_sign_in

class LastSignInSpec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp

  "Last sign in template" should {
    "render the correct content in english" in {
      val document = Jsoup.parse(last_sign_in(new DateTime(), None)(engRequest, messagesInEnglish()).toString())
      document
        .getElementsByClass("last-login__more-details")
        .first()
        .text() must (startWith("Last sign in:") and endWith("not right?"))
      document.getElementsByClass("minimise").first().text() mustBe "Minimise"
      document.getElementsByClass("alert__message").first().text() must startWith("Last sign in:")
      document
        .getElementsByAttributeValue("href", "https://www.gov.uk/hmrc-online-services-helpdesk")
        .text() mustBe ("Contact HMRC")
      document.getElementsByClass("flush--bottom").first().text() must endWith("if it wasn't you.")
    }

    "render the correct content in welsh" in {
      val document = Jsoup.parse(last_sign_in(new DateTime(), None)(welshRequest, messagesInWelsh()).toString())
      document
        .getElementsByClass("last-login__more-details")
        .first()
        .text() must (startWith("Mewngofnodwyd diwethaf:") and endWith("dim yn gywir?"))
      document.getElementsByClass("minimise").first().text() mustBe "Lleihau"
      document.getElementsByClass("alert__message").first().text() must startWith("Mewngofnodwyd diwethaf:")
      document
        .getElementsByAttributeValue("href", "https://www.gov.uk/hmrc-online-services-helpdesk")
        .text() mustBe ("Cysylltwch â CThEM")
      document.getElementsByClass("flush--bottom").first().text() must endWith("os nad chi ydoedd.")
    }
  }
}
