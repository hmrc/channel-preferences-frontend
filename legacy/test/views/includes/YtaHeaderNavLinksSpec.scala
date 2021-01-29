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
import views.html.includes.yta_header_nav_links

class YtaHeaderNavLinksSpec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val template = app.injector.instanceOf[yta_header_nav_links]

  "Yta Header Nav Links" should {
    "render the correct content in english" in {
      val document = Jsoup.parse(template()(engRequest, messagesInEnglish()).toString())
      document.getElementById("homeNavHref").text() mustBe "Home"
      document.getElementById("accountDetailsNavHref").text() mustBe "Manage account"
      document.getElementsByAttributeValue("href", "/contact/contact-hmrc").text() mustBe "Help and contact"
      document.getElementById("logOutNavHref").text() mustBe "Sign out"
    }

    "render the correct content in welsh" in {
      val document = Jsoup.parse(template()(welshRequest, messagesInWelsh()).toString())
      document.getElementById("homeNavHref").text() mustBe "Hafan"
      document.getElementById("accountDetailsNavHref").text() mustBe "Rheoli'r cyfrif"
      document.getElementsByAttributeValue("href", "/contact/contact-hmrc").text() mustBe "Help a chysylltu"
      document.getElementById("logOutNavHref").text() mustBe "Allgofnodi"
    }
  }
}
