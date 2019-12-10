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

package connectors

import controllers.LanguageController
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test._

import scala.concurrent.Future

class LanguageControllerSuite extends PlaySpec with GuiceOneAppPerSuite with Results {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .build()
  val controller = app.injector.instanceOf[LanguageController]
  "Validating english/welsh switch" should {
    "return display english when 'english' selected" in {
      val result: Future[Result] =
        controller.switchToEnglish().apply(FakeRequest().withHeaders("Referer" -> "/paperless/choose"))
      val bodyText: String = contentAsString(result)
      val cook: Cookies = cookies(result)
      cook.get("PLAY_LANG").get.value mustBe "en"
    }
    "return display welsh when 'welsh' selected" in {
      val result: Future[Result] =
        controller.switchToWelsh().apply(FakeRequest().withHeaders("Referer" -> "/paperless/choose"))
      val bodyText: String = contentAsString(result)
      val cook: Cookies = cookies(result)
      cook.get("PLAY_LANG").get.value mustBe "cy"
    }
  }
}
