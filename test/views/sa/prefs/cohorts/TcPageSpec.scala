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

package views.sa.prefs.cohorts

import controllers.auth.AuthenticatedRequest
import controllers.internal
import controllers.internal.EmailForm
import helpers.{ ConfigHelper, TestFixtures, LanguageHelper }
import org.jsoup.Jsoup
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import org.scalatestplus.play.PlaySpec
import views.html.sa.prefs.cohorts.tc_page

class TcPageSpec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val template = app.injector.instanceOf[tc_page]

  "Tax Credit Template" should {
    "render the correct content in english" in {
      val email = "a@a.com"
      val form = EmailForm().bind(Map("email.main" -> email))
      val document = Jsoup.parse(
        template(form, internal.routes.ChoosePaperlessController.submitForm(TestFixtures.sampleHostContext))(
          engRequest,
          messagesInEnglish()).toString())
      document.getElementsByClass("header__menu__proposition-name").get(0).text() mustBe "Tax credits service"
      document.getElementById("email-display").text() mustBe s"The email address we we will store securely is $email"
      document
        .getElementsByTag("p")
        .get(1)
        .text() mustBe "By letting us store your email address, you confirm that you:"
      document
        .getElementsByTag("li")
        .get(1)
        .text() mustBe "want to get notifications and prompts about your tax credits"
      document
        .getElementsByTag("li")
        .get(2)
        .text() mustBe "will keep your email address up to date using your HMRC online account to make sure you get your email notifications"
      document.getElementsByClass("selectable").get(0).text() mustBe "Yes, store my email address"
      document.getElementsByClass("selectable").get(1).text() mustBe "No, I do not want my email address stored"
    }

    "render the correct content in welsh" in {
      val email = "a@a.com"
      val form = EmailForm().bind(Map("email.main" -> email))
      val document = Jsoup.parse(
        template(form, internal.routes.ChoosePaperlessController.submitForm(TestFixtures.sampleHostContext))(
          welshRequest,
          messagesInWelsh()).toString())
      document
        .getElementsByTag("title")
        .text() mustBe "A hoffech gael hysbysiadau e-bost eraill ynghylch eich credydau treth?"
      document.getElementsByClass("header__menu__proposition-name").get(0).text() mustBe "Gwasanaeth Credydau Treth"
      document
        .getElementsByTag("h1")
        .get(0)
        .text() mustBe "A hoffech gael hysbysiadau e-bost eraill ynghylch eich credydau treth?"
      document
        .getElementById("email-display")
        .text() mustBe s"Y cyfeiriad e-bost y byddwn yn ei storio'n ddiogel yw $email"
      document
        .getElementsByTag("p")
        .get(1)
        .text() mustBe "Drwy ganiatáu i ni storio'ch cyfeiriad e-bost, rydych yn cadarnhau'r canlynol:"
      document
        .getElementsByTag("li")
        .get(1)
        .text() mustBe "eich bod am gael hysbysiadau a negeseuon annog ynghylch eich credydau treth"
      document
        .getElementsByTag("li")
        .get(2)
        .text() mustBe "byddwch yn cadw'ch cyfeiriad e-bost wedi'i ddiweddaru drwy ddefnyddio'ch cyfrif ar-lein gyda CThEM i wneud yn siŵr eich bod yn cael eich hysbysiadau"
      document
        .getElementById("termsAndConditions_accept-tc")
        .parentNode
        .childNodes()
        .get(3)
        .toString
        .contains("Rwy'n cytuno â'r")
      //document.getElementById("terms-and-conditions").childNodes().get(0).toString() mustBe "telerau ac amodau"
      document.getElementsByClass("selectable").get(0).text() mustBe "Iawn, storiwch fy nghyfeiriad e-bost"
      document
        .getElementsByClass("selectable")
        .get(1)
        .text() mustBe "Na, nid wyf am i'm cyfeiriad e-bost gael ei storio"
      document.getElementById("submitEmailButton").text() mustBe "Yn eich blaen"
    }
  }
}
