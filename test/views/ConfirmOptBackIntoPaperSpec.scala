package views

import _root_.helpers.{ConfigHelper, TestFixtures, WelshLanguage}
import org.jsoup.Jsoup
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import uk.gov.hmrc.emailaddress.ObfuscatedEmailAddress
import uk.gov.hmrc.play.test.UnitSpec
import views.html.confirm_opt_back_into_paper

class ConfirmOptBackIntoPaperSpec extends UnitSpec with OneAppPerSuite with WelshLanguage {

  override implicit lazy val app: Application = ConfigHelper.fakeApp

  "confirm opt back into paperless template" should {
    "render the correct content in english" in {
      val email = ObfuscatedEmailAddress("a@a.com")
      val document = Jsoup.parse(confirm_opt_back_into_paper(email)(None, FakeRequest("GET", "/"), applicationMessages, TestFixtures.sampleHostContext).toString())

      document.getElementsByTag("title").first().text() shouldBe "Stop notifications"
      document.getElementsByTag("h1").get(0).text() shouldBe "Stop notifications"
      document.getElementsByTag("p").get(1).text() shouldBe "You'll get letters again, instead of emails."
      document.getElementById("cancel-link").text() shouldBe "Cancel"
    }

    "render the correct content in welsh" in {
      val email = ObfuscatedEmailAddress("a@a.com")
      val document = Jsoup.parse(confirm_opt_back_into_paper(email)(None, welshRequest, messagesInWelsh(applicationMessages), TestFixtures.sampleHostContext).toString())

      document.getElementsByTag("title").first().text() shouldBe "Atal hysbysiadau"
      document.getElementsByTag("h1").get(0).text() shouldBe "Atal hysbysiadau"
      document.getElementsByTag("p").get(1).text() shouldBe "Byddwch yn cael llythyrau unwaith eto, yn hytrach nag e-byst."
      document.getElementById("cancel-link").text() shouldBe "Canslo"
    }
  }
}
