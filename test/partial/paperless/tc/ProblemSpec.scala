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

package partial.paperless.tc

import _root_.helpers.{ ConfigHelper, LanguageHelper }
import org.jsoup.Jsoup
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import org.scalatestplus.play.PlaySpec
import html.problem
import play.api.data.FormError
import play.api.test.FakeRequest

class ProblemSpec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp

  "problem partial" should {
    "render the correct content in english" in {
      val errors = Seq((FormError("ErrorKey", Seq("Error Message"), Seq()), "Outer Error Message"))
      val document = Jsoup.parse(problem(errors)(FakeRequest(), messagesInEnglish()).toString())

      document.getElementById("error-summary-heading").text() mustBe "There is a problem"
    }

    "render the correct content in welsh" in {
      val errors = Seq((FormError("ErrorKey", Seq("Error Message"), Seq()), "Outer Error Message"))
      val document = Jsoup.parse(problem(errors)(welshRequest, messagesInWelsh()).toString())

      document.getElementById("error-summary-heading").text() mustBe "Mae yna broblem"
    }
  }
}
