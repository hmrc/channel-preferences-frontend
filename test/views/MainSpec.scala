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

import _root_.helpers.{ ConfigHelper, LanguageHelper }
import controllers.auth.AuthenticatedRequest
import org.jsoup.Jsoup
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.emailaddress.ObfuscatedEmailAddress
import org.scalatestplus.play.PlaySpec
import views.html.main

class MainSpec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val template = app.injector.instanceOf[main]

  "main template" should {
    "render the correct content in english" in {
      val email = ObfuscatedEmailAddress("a@a.com")
      val document = Jsoup.parse(template("title")(Html("Some HTML"))(engRequest, messagesInEnglish()).toString())

      document.getElementsByClass("header__menu__proposition-name").get(0).text() mustBe "Your tax account"
    }

    "render the correct content in welsh" in {
      val email = ObfuscatedEmailAddress("a@a.com")
      val document = Jsoup.parse(template("title")(Html("Some HTML"))(welshRequest, messagesInWelsh()).toString())

      document.getElementsByClass("header__menu__proposition-name").get(0).text() mustBe "Eich cyfrif treth"
    }
  }
}
