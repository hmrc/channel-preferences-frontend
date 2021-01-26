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

package views.includes

import helpers.{ ConfigHelper, LanguageHelper }
import org.jsoup.Jsoup
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import views.html.includes.header_nav_links

class HeaderNavLinksSpec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val template = app.injector.instanceOf[header_nav_links]

  "Header Nav Links" should {
    "Display the sign out text from messages" in {
      val document = Jsoup.parse(template()(engRequest, messagesInEnglish()).toString)
      document.getElementById("logOutNavHref").text() mustBe "Sign out"
    }

    "Display the sign out text from messages in welsh" in {
      val document = Jsoup.parse(template()(welshRequest, messagesInWelsh()).toString)
      document.getElementById("logOutNavHref").text() mustBe "Allgofnodi"
    }
  }
}
