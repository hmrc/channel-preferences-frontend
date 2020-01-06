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

package views.sa.prefs.cohorts

import controllers.auth.AuthenticatedRequest
import controllers.internal
import controllers.internal.EmailForm
import helpers.{ConfigHelper, LanguageHelper, TestFixtures}
import org.jsoup.Jsoup
import org.junit.Ignore
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import org.scalatestplus.play.PlaySpec
import views.html.sa.prefs.cohorts.i_page

class IPageSpec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp

  val template = app.injector.instanceOf[i_page]
  "I Page Template" should {
    "render the correct content in english" in {
      val form = EmailForm()
      val document = Jsoup.parse(
        template(form, internal.routes.ChoosePaperlessController.submitForm(TestFixtures.sampleHostContext))(
          engRequest,
          messagesInEnglish()).toString())

      document.getElementsByTag("title").text() mustBe "Choose how to get your legal notices, penalty notices and tax letters"
      document.getElementsByTag("h1").text() mustBe "Choose how to get your legal notices, penalty notices and tax letters"
      document
        .getElementsByClass("lede")
        .first()
        .text() mustBe "You can choose to get some of your tax documents and information sent through your HMRC online account instead of by post."
      document
        .getElementsByTag("p")
        .get(2)
        .text() mustBe "You will need to take action when you receive some of the documents. They include:"

      document
          .getElementsByTag("li").get(1).text() mustBe "Legal notice to file tax return"

      document
        .getElementsByTag("li").get(2).text() mustBe "Late filing penalty notice"

      document
        .getElementsByTag("li").get(3).text() mustBe "Late payment penalty notice"

      document
        .getElementsByTag("p")
        .get(3)
        .text() mustBe "We may also send you other information, including letting you know about a change to your personal tax code, if you have one."

     document.getElementsByTag("h2").get(0).text() mustBe "How do you want to get you legal notices, penalty notices and tax letters?"

      document
        .getElementsByTag("p")
        .get(4)
        .childNodes()
        .get(0)
        .toString
        .trim mustBe "We’ll email to tell you when you have a new item in your online account. This email cannot include personal information, so it is your responsibility to sign into your online account and read the full details."
      document.getElementsByClass("selectable").get(0).text() mustBe "Through my HMRC online account"

      document
        .getElementsByTag("p")
        .get(5)
        .childNodes()
        .get(0)
        .toString
        .trim mustBe "Because we cannot send all letters online yet, you will continue to get some by post."

      document.getElementsByClass("selectable").get(1).text() mustBe "By post only"
      document.getElementById("privacy-policy").text() must include("read the privacy notice")
      document.getElementsByAttributeValue("name", "submitButton").text()  mustBe "Continue"

    }

    "render the correct content in welsh" in  {
      val form = EmailForm()
      val document = Jsoup.parse(
        template(form, internal.routes.ChoosePaperlessController.submitForm(TestFixtures.sampleHostContext))(
          welshRequest,
          messagesInWelsh()).toString())

      document.getElementsByTag("title").text() mustBe "Dewis sut i gael eich hysbysiadau cyfreithiol, hysbysiadau o gosb a llythyrau treth"
      document.getElementsByTag("h1").text() mustBe "Dewis sut i gael eich hysbysiadau cyfreithiol, hysbysiadau o gosb a llythyrau treth"
      document
        .getElementsByClass("lede")
        .first()
        .text() mustBe "Gallwch ddewis cael rhai o’ch dogfennau treth a gwybodaeth drwy’ch cyfrif CThEM ar-lein, yn hytrach na thrwy’r post."
      document
        .getElementsByTag("p")
        .get(2)
        .text() mustBe "Bydd yn rhaid i chi gymryd camau pan fyddwch yn cael rhai o’r dogfennau. Maent yn cynnwys:"

      document
        .getElementsByTag("li").get(1).text() mustBe "Hysbysiad cyfreithiol i gyflwyno Ffurflen Dreth"

      document
        .getElementsByTag("li").get(2).text() mustBe "Hysbysiad cyfreithiol i gyflwyno Ffurflen Dreth"

      document
        .getElementsByTag("li").get(3).text() mustBe "Hysbysiad o gosb am dalu’n hwyr"

      document
        .getElementsByTag("p")
        .get(3)
        .text() mustBe "Mae’n bosibl y byddwn hefyd yn anfon gwybodaeth arall atoch, gan gynnwys rhoi gwybod i chi am newid i’ch cod treth personol, os oes un gennych."

      document.getElementsByTag("h2").get(0).text() mustBe "Sut yr hoffech gael eich hysbysiadau cyfreithiol, hysbysiadau o gosb a llythyrau treth?"

      document
        .getElementsByTag("p")
        .get(4)
        .childNodes()
        .get(0)
        .toString
        .trim mustBe "Byddwn yn anfon e-bost atoch i roi gwybod i chi pan fydd eitem newydd yn eich cyfrif ar-lein. Ni all yr e-bost hwn gynnwys gwybodaeth bersonol, felly, eich cyfrifoldeb chi yw mewngofnodi i’ch cyfrif ar-lein a darllen y manylion llawn."
      document.getElementsByClass("selectable").get(0).text() mustBe "Drwy fy nghyfrif CThEM ar-lein"

      document
        .getElementsByTag("p")
        .get(5)
        .childNodes()
        .get(0)
        .toString
        .trim mustBe "Oherwydd na allwn anfon pob llythyr ar-lein eto, byddwch yn parhau i gael rhai llythyrau drwy’r post."

      document.getElementsByClass("selectable").get(1).text() mustBe "Drwy’r post yn unig"
      document.getElementById("privacy-policy").text() must include("darllenwch yr hysbysiad preifatrwydd")
      document.getElementsByAttributeValue("name", "submitButton").text()  mustBe "Yn eich blaen"
    }
  }
}
