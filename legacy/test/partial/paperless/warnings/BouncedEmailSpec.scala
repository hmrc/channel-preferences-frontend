/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package partial.paperless.warnings

import _root_.helpers.{ ConfigHelper, LanguageHelper, TestFixtures }
import org.jsoup.Jsoup
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import partial.paperless.warnings.html.bounced_email
import play.api.Application
import play.api.test.FakeRequest

class BouncedEmailSpec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp

  "bounced email partial" should {
    "render the correct content in english if the mailbox is full" in {
      val document =
        Jsoup.parse(bounced_email(true, TestFixtures.sampleHostContext)(FakeRequest(), messagesInEnglish()).toString())

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
        Jsoup.parse(bounced_email(true, TestFixtures.sampleHostContext)(welshRequest, messagesInWelsh()).toString())

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
