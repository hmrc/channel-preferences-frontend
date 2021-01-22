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

package views.sa.prefs

import _root_.helpers.{ ConfigHelper, LanguageHelper }
import org.jsoup.Jsoup
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import views.html.sa.prefs.sa_printing_preference_expired_email

class SaPrintingPreferenceExpiredEmailSpec
    extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val saPrintingPreferenceExpiredEmail =
    app.injector.instanceOf[sa_printing_preference_expired_email]

  "printing preferences expired emai; template" should {
    "render the correct content in english" in {
      val foo = saPrintingPreferenceExpiredEmail()(engRequest, messagesInEnglish()).toString()
      val document = Jsoup.parse(saPrintingPreferenceExpiredEmail()(engRequest, messagesInEnglish()).toString())

      document
        .getElementById("link-to-home")
        .childNodes()
        .get(2)
        .toString
        .trim() mustBe "and request a new verification link"
    }

    "render the correct content in welsh" in {
      val document = Jsoup.parse(saPrintingPreferenceExpiredEmail()(welshRequest, messagesInWelsh()).toString())

      document.getElementsByTag("title").first().text() mustBe "NID yw'ch cyfeiriad e-bost wedi'i ddilysu"
      document
        .getElementById("link-to-home")
        .childNodes()
        .get(1)
        .childNode(0)
        .toString mustBe "Yn eich blaen i'ch cyfrif ar-lein gyda CThEM"
      document
        .getElementById("link-to-home")
        .childNodes()
        .get(2)
        .toString
        .trim() mustBe "a gwnewch gais am gysylltiad dilysu newydd"
    }
  }
}
