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

package partial.paperless.warnings

import _root_.helpers.{ ConfigHelper, LanguageHelper }
import org.jsoup.Jsoup
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import org.scalatestplus.play.PlaySpec
import html.bounced_email
import play.api.test.FakeRequest

class BouncedEmailSpec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp

  "bounced email partial" should {
    "render the correct content in english if the mailbox is full" in {
      val document =
        Jsoup.parse(bounced_email(true, "returnUrl", "returnLinkText")(FakeRequest(), messagesInEnglish()).toString())

      document
        .getElementsByAttributeValue("role", "alert")
        .first()
        .childNodes()
        .get(0)
        .toString() mustBe "There's a problem with your paperless notification emails "
      document.getElementsByClass("flag--urgent").first().text() mustBe "Urgent"
      document.getElementsByTag("p").get(0).text() mustBe "Your inbox is full."
      document.getElementsByTag("p").get(1).childNodes().get(0).toString() mustBe "Go to "
      document.getElementsByTag("p").get(1).childNodes().get(2).toString() mustBe " for more information."
    }

    "render the correct content in welsh if the mailbox is full" in {
      val document =
        Jsoup.parse(bounced_email(true, "returnUrl", "returnLinkText")(welshRequest, messagesInWelsh()).toString())

      document
        .getElementsByAttributeValue("role", "alert")
        .first()
        .childNodes()
        .get(0)
        .toString() mustBe "Mae yna broblem gyda'ch e-byst hysbysu di-bapur "
      document.getElementsByClass("flag--urgent").first().text() mustBe "Ar frys"
      document.getElementsByTag("p").get(0).text() mustBe "Mae'ch mewnflwch yn llawn."
      document.getElementsByTag("p").get(1).childNodes().get(0).toString() mustBe "Am ragor o wybodaeth, ewch i "
      document.getElementsByTag("p").get(1).childNodes().get(2).toString() mustBe " "
    }
  }
}
