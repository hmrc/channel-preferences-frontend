package views

import _root_.helpers.{ConfigHelper, TestFixtures}
import org.jsoup.Jsoup
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import uk.gov.hmrc.emailaddress.ObfuscatedEmailAddress
import uk.gov.hmrc.play.test.UnitSpec
import views.html.confirm_opt_back_into_paper

class ConfirmOptBackIntoPaperSpec extends UnitSpec with OneAppPerSuite {

  override implicit lazy val app: Application = ConfigHelper.fakeApp

  "preference print template" should {
    "render the correct content for the IPage cohort " in {
      val email = ObfuscatedEmailAddress("a@a.com")
      val document = Jsoup.parse(confirm_opt_back_into_paper(email)(None, FakeRequest("GET", "/"), applicationMessages, TestFixtures.sampleHostContext).toString())

      document.getElementsByTag("title").first().text() shouldBe "Stop notifications"
      document.getElementsByTag("h1").get(0).text() shouldBe "Stop notifications"
      document.getElementsByTag("p").get(1).text() shouldBe "You'll get letters again, instead of emails."
      document.getElementById("cancel-link").text() shouldBe "Cancel"
    }
  }
}
