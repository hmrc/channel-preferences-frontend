package views

import _root_.helpers.{ConfigHelper, TestFixtures}
import org.jsoup.Jsoup
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec
import views.html.account_details_update_email_address_verify_email

class AccountDetailsUpdateEmailAddressVerifyEmailSpec extends UnitSpec with OneAppPerSuite {

  override implicit lazy val app: Application = ConfigHelper.fakeApp

  "account details update email address verify email template" should {
    "render the correct content in english" in {
      val email = "a@a.com"
      val document = Jsoup.parse(account_details_update_email_address_verify_email(email)(None, FakeRequest("GET", "/"), applicationMessages, TestFixtures.sampleHostContext).toString())

      document.getElementsByTag("title").first().text() shouldBe "Check your email address"
      document.getElementsByTag("h1").get(0).text() shouldBe "Check your email address"
      document.getElementsByTag("p").get(1).text() shouldBe s"Are you sure $email is correct?"
      document.getElementById("emailIsCorrectLink").text() shouldBe "This email address is correct"
      document.getElementById("emailIsNotCorrectLink").text() shouldBe "Change this email address"
    }
  }
}
