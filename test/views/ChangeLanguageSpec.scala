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

package views.manage

import _root_.helpers.{ ConfigHelper, LanguageHelper, TestFixtures }
import controllers.auth.AuthenticatedRequest
import controllers.internal.LanguageForm
import org.jsoup.Jsoup
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.mvc.Call
import play.api.test.FakeRequest
import views.html._

class ChangeLanguageSpec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val template = app.injector.instanceOf[change_language]

  val authRequest = AuthenticatedRequest(FakeRequest(), None, None, None, None)
  "change language page" should {
    val form = LanguageForm()
    "render the correct content in english" in {
      val document =
        Jsoup.parse(
          template(form, Call("/myurl", "GET"))(authRequest, messagesInEnglish(), TestFixtures.sampleHostContext).toString
        )

      document
        .getElementsByClass("govuk-fieldset__heading")
        .first()
        .text() mustBe "Get your paperless email notification in Welsh"
    }

    "render the correct content in welsh" in {
      val document =
        Jsoup.parse(
          template(form, Call("/myurl", "GET"))(authRequest, messagesInWelsh(), TestFixtures.sampleHostContext).toString
        )

      document
        .getElementsByClass("govuk-fieldset__heading")
        .first()
        .text() mustBe "Cael eich hysbysiadau di-bapur drwy e-bost yn Gymraeg"
    }
  }
}
