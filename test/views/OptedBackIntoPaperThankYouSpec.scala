package views

import _root_.helpers.{ConfigHelper, TestFixtures}
import org.jsoup.Jsoup
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import uk.gov.hmrc.emailaddress.ObfuscatedEmailAddress
import uk.gov.hmrc.play.test.UnitSpec
import views.html.opted_back_into_paper_thank_you

class OptedBackIntoPaperThankYouSpec extends UnitSpec with OneAppPerSuite {

  override implicit lazy val app: Application = ConfigHelper.fakeApp

  "opted back into paper template" should {
    "render the correct content in english" in {
      val email = ObfuscatedEmailAddress("a@a.com")
      val document = Jsoup.parse(opted_back_into_paper_thank_you()(None, FakeRequest("GET", "/"), applicationMessages, TestFixtures.sampleHostContext).toString())

      document.getElementsByTag("title").get(0).text() shouldBe "Paperless notifications have stopped"
      document.getElementsByTag("h1").get(0).text() shouldBe "Paperless notifications have stopped"
      document.getElementsByTag("p").get(1).text() shouldBe "You'll get letters again, instead of emails."
      document.getElementsByTag("p").get(2).text() shouldBe "You've been sent an email confirming this."
      document.getElementsByTag("p").get(3).text() shouldBe "You can go paperless again at any time."
    }
  }
}
