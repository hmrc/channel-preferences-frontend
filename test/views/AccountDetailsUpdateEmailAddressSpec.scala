package views

import _root_.helpers.{TestFixtures, ConfigHelper, WelshLanguage}
import controllers.internal.EmailForm
import org.jsoup.Jsoup
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec
import views.html.account_details_update_email_address

class AccountDetailsUpdateEmailAddressSpec extends UnitSpec with OneAppPerSuite with WelshLanguage {

  override implicit lazy val app: Application = ConfigHelper.fakeApp

  "account details update email address template" should {
    "render the correct content in english" in {
      val currentEmail = "a@a.com"
      val form = EmailForm()
      val document = Jsoup.parse(account_details_update_email_address(currentEmail, form)(None, FakeRequest("GET", "/"), applicationMessages, TestFixtures.sampleHostContext).toString())

      document.getElementsByTag("title").first().text() shouldBe "Change your email address"
      document.getElementsByTag("h1").get(0).text() shouldBe "Change your email address"
      document.getElementsByTag("p").get(1).childNodes().get(0).toString shouldBe "Emails are sent to "
      document.getElementById("submit-email-button").text() shouldBe "Change email address"
      document.getElementById("cancel-link").text() shouldBe "Cancel"
      document.getElementsByAttributeValue("for", "email.main").first().child(0).text() shouldBe "New email address"
      document.getElementsByAttributeValue("for", "email.confirm").first().child(0).text() shouldBe "Confirm new email address"
    }

    "render the correct content in welsh" in {
      val currentEmail = "a@a.com"
      val form = EmailForm()
      val document = Jsoup.parse(account_details_update_email_address(currentEmail, form)(None, welshRequest, messagesInWelsh(applicationMessages), TestFixtures.sampleHostContext).toString())

      document.getElementsByTag("title").first().text() shouldBe "Newid eich cyfeiriad e-bost"
      document.getElementsByTag("h1").get(0).text() shouldBe "Newid eich cyfeiriad e-bost"
      document.getElementsByTag("p").get(1).childNodes().get(0).toString shouldBe "Anfonir e-byst at "
      document.getElementById("submit-email-button").text() shouldBe "Newid y cyfeiriad e-bost"
      document.getElementById("cancel-link").text() shouldBe "Canslo"
      document.getElementsByAttributeValue("for", "email.main").first().child(0).text() shouldBe "Cyfeiriad e-bost newydd"
      document.getElementsByAttributeValue("for", "email.confirm").first().child(0).text() shouldBe "Cadarnhau'r cyfeiriad e-bost newydd"
    }
  }
}