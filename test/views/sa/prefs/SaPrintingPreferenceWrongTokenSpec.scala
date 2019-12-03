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

package views.sa.prefs

import _root_.helpers.{ ConfigHelper, LanguageHelper }
import controllers.auth.AuthenticatedRequest
import org.jsoup.Jsoup
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import org.scalatestplus.play.PlaySpec
import views.html.sa.prefs.sa_printing_preference_wrong_token

class SaPrintingPreferenceWrongTokenSpec
    extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val template = app.injector.instanceOf[sa_printing_preference_wrong_token]

  "sa printing preferences wrong token template" should {
    "render the correct content in english" in {
      val document = Jsoup.parse(template()(engRequest, messagesInEnglish()).toString())

      document.getElementsByTag("title").first().text() mustBe "You've used a link that has now expired"
    }

    "render the correct content in welsh" in {
      val document = Jsoup.parse(template()(welshRequest, messagesInWelsh()).toString())

      document
        .getElementsByTag("title")
        .first()
        .text() mustBe "Rydych wedi defnyddio cysylltiad sydd bellach wedi dod i ben"
      document
        .getElementById("failure-heading")
        .text() mustBe "Rydych wedi defnyddio cysylltiad sydd bellach wedi dod i ben"
      document
        .getElementById("success-message")
        .text() mustBe "Mae'n bosibl ei fod wedi cael ei anfon i hen gyfeiriad e-bost neu un gwahanol. Dylech ddefnyddio'r cysylltiad yn yr e-bost dilysu diwethaf a anfonwyd i'r cyfeiriad e-bost a nodwyd gennych."
    }
  }
}
