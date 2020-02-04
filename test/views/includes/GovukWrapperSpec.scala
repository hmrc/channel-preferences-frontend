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

import helpers.{ ConfigHelper, LanguageHelper }
import org.jsoup.Jsoup
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import org.scalatestplus.play.PlaySpec
import views.html.govuk_wrapper

class GovukWrapperSpec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val template = app.injector.instanceOf[govuk_wrapper]

  "GovukWrapper" should {
    """Display the language switch for "English" page""" in {
      val document = Jsoup.parse(template(title = "", navTitle = "")(engRequest, messagesInEnglish()).toString())
      document.getElementsByTag("li").get(0).text() mustBe "English | Newid yr iaith i'r Gymraeg Cymraeg"
    }

    """Display the language switch for "Welsh" page""" in {
      val document = Jsoup.parse(template(title = "", navTitle = "")(welshRequest, messagesInWelsh()).toString())
      document.getElementsByTag("li").get(0).text() mustBe "Newid yr iaith i'r Saesneg English | Cymraeg"
    }
  }
}
