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
import helpers.{ ConfigHelper, LanguageHelper, TestFixtures }
import org.jsoup.Jsoup
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
      val form = EmailForm().bind(Map("emailAlreadyStored" -> "true"))
      val document = Jsoup.parse(
        template(form, internal.routes.ChoosePaperlessController.submitForm(TestFixtures.sampleHostContext))(
          engRequest,
          messagesInEnglish()).toString())
      document
        .getElementsByClass("lede")
        .first()
        .text() mustBe "You can choose to get electronic communications instead of letters from HMRC."
      document
        .getElementsByTag("p")
        .get(2)
        .text() mustBe "These electronic communications include statutory notices, decisions, estimates and reminders relating to your tax affairs, such as notices to file a tax return, make a payment, penalties due, or information about other matters."
      document
        .getElementsByTag("p")
        .get(3)
        .text() mustBe "When you have a new electronic communication we will send you an email notification requiring you to log in to your HMRC online account."
      document.getElementsByTag("h2").get(0).text() mustBe "Go paperless now"
      document.getElementsByClass("selectable").get(0).text() mustBe "Yes, send me electronic communications"
      document
        .getElementsByTag("p")
        .get(4)
        .childNodes()
        .get(0)
        .toString
        .trim mustBe "You have signed up for digital communications for Tax Credits with this email address."
      document
        .getElementsByTag("p")
        .get(4)
        .childNodes()
        .get(2)
        .toString
        .trim mustBe "If you wish to change the email address you can do this later within manage your account"
      document.getElementsByTag("p").get(5).text() mustBe "By signing up, you confirm that you:"
      document
        .getElementsByTag("li")
        .get(1)
        .text() mustBe "want to receive statutory notices, decisions, estimates and reminders electronically in connection with your tax affairs"
      document
        .getElementsByTag("li")
        .get(2)
        .text() mustBe "will keep your communications preferences and email address up to date using your HMRC online account to make sure you get your email notifications"
      document.getElementsByClass("selectable").get(1).text() mustBe "No, I want to keep receiving letters"
    }

    "render the correct content in welsh" in {
      val form = EmailForm().bind(Map("emailAlreadyStored" -> "true"))
      val document = Jsoup.parse(
        template(form, internal.routes.ChoosePaperlessController.submitForm(TestFixtures.sampleHostContext))(
          welshRequest,
          messagesInWelsh()).toString())
      document
        .getElementsByClass("lede")
        .first()
        .text() mustBe "Gallwch ddewis cyfathrebu drwy ddull electronig, yn hytrach nag ar bapur, gyda CThEM."
      document
        .getElementsByTag("p")
        .get(2)
        .text() mustBe "Mae'r cyfathrebu electronig hyn yn cynnwys hysbysiadau statudol, penderfyniadau, amcangyfrifon a nodynnau atgoffa sy'n ymwneud â'ch materion treth, megis hysbysiadau i gyflwyno Ffurflen Dreth, gwneud taliad, cosbau sy'n ddyledus, neu wybodaeth am faterion eraill."
      document
        .getElementsByTag("p")
        .get(3)
        .text() mustBe "Pan fo gennych ddogfen gyfathrebu electronig newydd, byddwn yn anfon hysbysiad e-bost atoch yn gofyn i chi fewngofnodi i'ch cyfrif ar-lein gyda CThEM."
      document.getElementsByTag("h2").get(0).text() mustBe "Ewch yn ddi-bapur nawr"
      document.getElementsByClass("selectable").get(0).text() mustBe "Iawn, cyfathrebwch â mi drwy ddull electronig"
      document
        .getElementsByTag("p")
        .get(4)
        .childNodes()
        .get(0)
        .toString
        .trim mustBe "Rydych wedi cofrestru i gyfathrebu'n ddigidol ar gyfer Credydau Treth gyda'r cyfeiriad e-bost hwn."
      document
        .getElementsByTag("p")
        .get(4)
        .childNodes()
        .get(2)
        .toString
        .trim mustBe "Os ydych am newid y cyfeiriad e-bost, gallwch wneud hyn nes ymlaen yn yr adran rheoli'ch cyfrif"
      document.getElementsByTag("p").get(5).text() mustBe "Drwy gofrestru, rydych yn cadarnhau'r canlynol:"
      document
        .getElementsByTag("li")
        .get(1)
        .text() mustBe "rydych am gael hysbysiadau statudol, penderfyniadau, amcangyfrifon a nodynnau atgoffa sy'n ymwneud â'ch materion treth drwy ddull electronig"
      document
        .getElementsByTag("li")
        .get(2)
        .text() mustBe "byddwch yn cadw'ch dewisiadau o ran cyfathrebu a'ch cyfeiriad e‑bost wedi'u diweddaru drwy ddefnyddio'ch cyfrif ar-lein gyda CThEM, er mwyn gwneud yn siŵr eich bod yn cael eich hysbysiadau e-bost"
      document.getElementsByClass("selectable").get(1).text() mustBe "Na, rwyf am barhau i gael llythyrau"
    }
  }
}
