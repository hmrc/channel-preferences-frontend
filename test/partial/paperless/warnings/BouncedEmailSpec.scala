package partial.paperless.warnings

import _root_.helpers.{ConfigHelper, WelshLanguage}
import org.jsoup.Jsoup
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import uk.gov.hmrc.play.test.UnitSpec
import html.bounced_email
import play.api.test.FakeRequest

class BouncedEmailSpec extends UnitSpec with OneAppPerSuite with WelshLanguage {

  override implicit lazy val app: Application = ConfigHelper.fakeApp

  "bounced email partial" should {
    "render the correct content in english if the mailbox is full" in {
      val document = Jsoup.parse(bounced_email(true, "returnUrl", "returnLinkText")(FakeRequest(), applicationMessages).toString())

      document.getElementsByAttributeValue("role", "alert").first().childNodes().get(0).toString() shouldBe "There's a problem with your paperless notification emails "
      document.getElementsByClass("flag--urgent").first().text() shouldBe "Urgent"
      document.getElementsByTag("p").get(0).text() shouldBe "Your inbox is full."
      document.getElementsByTag("p").get(1).childNodes().get(0).toString() shouldBe "Go to "
      document.getElementsByTag("p").get(1).childNodes().get(2).toString()  shouldBe " for more information."
    }

    "render the correct content in welsh if the mailbox is full" in {
      val document = Jsoup.parse(bounced_email(true, "returnUrl", "returnLinkText")(welshRequest, messagesInWelsh(applicationMessages)).toString())

      document.getElementsByAttributeValue("role", "alert").first().childNodes().get(0).toString() shouldBe "Mae yna broblem gyda'ch e-byst hysbysu di-bapur "
      document.getElementsByClass("flag--urgent").first().text() shouldBe "Ar frys"
      document.getElementsByTag("p").get(0).text() shouldBe "Mae'ch mewnflwch yn llawn."
      document.getElementsByTag("p").get(1).childNodes().get(0).toString() shouldBe "Am ragor o wybodaeth, ewch i "
      document.getElementsByTag("p").get(1).childNodes().get(2).toString() shouldBe " "
    }
  }
}