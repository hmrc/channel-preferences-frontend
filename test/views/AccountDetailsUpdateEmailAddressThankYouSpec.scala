package views

import _root_.helpers.{ConfigHelper, TestFixtures}
import org.jsoup.Jsoup
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import uk.gov.hmrc.emailaddress.ObfuscatedEmailAddress
import uk.gov.hmrc.play.test.UnitSpec
import views.html.account_details_update_email_address_thank_you

class AccountDetailsUpdateEmailAddressThankYouSpec extends UnitSpec with OneAppPerSuite {

  override implicit lazy val app: Application = ConfigHelper.fakeApp

  "preference print template" should {
    "render the correct content for the IPage cohort " in {
      val currentEmail = ObfuscatedEmailAddress("a@a.com")
      val document = Jsoup.parse(account_details_update_email_address_thank_you(currentEmail)(None, FakeRequest("GET", "/"), applicationMessages, TestFixtures.sampleHostContext).toString())

      document.getElementsByTag("title").first().text() shouldBe "Verify your new email address"
      document.getElementsByTag("h1").get(0).text() shouldBe "Verify your new email address"
      document.getElementsByTag("p").get(1).childNodes().get(0).toString shouldBe "An email has been sent to "
      document.getElementById("verification-mail-message").text() shouldBe "Click on the link in the email to verify the address."
      document.getElementsByTag("p").get(3).text() shouldBe "Until you do this, emails will continue to go to your old address."
    }
  }
}