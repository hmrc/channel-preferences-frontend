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
import controllers.internal._
import org.jsoup.Jsoup
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.mvc.Call
import views.html.sa.prefs.sa_printing_preference

class SaPrintingPreferenceViewSpec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val template = app.injector.instanceOf[sa_printing_preference]

  "preference print template" should {
    "render the correct content for the IPage cohort " in {
      val document = Jsoup.parse(
        template(
          emailForm = EmailForm(),
          submitPrefsFormAction = Call("GET", "/"),
          cohort = CohortCurrent.ipage
        )(engRequest, messagesInEnglish()).toString()
      )

      document.getElementById("opt-in").hasAttr("checked") mustBe false
    }
  }
}
