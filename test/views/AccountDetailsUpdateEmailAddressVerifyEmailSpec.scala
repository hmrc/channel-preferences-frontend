package views

import _root_.helpers.{ConfigHelper, TestFixtures, WelshLanguage}
import controllers.auth.AuthenticatedRequest
import org.jsoup.Jsoup
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec
import views.html.account_details_update_email_address_verify_email


class AccountDetailsUpdateEmailAddressVerifyEmailSpec extends UnitSpec with OneAppPerSuite with WelshLanguage {

  override implicit lazy val app: Application = ConfigHelper.fakeApp

  "account details update email address verify email template" should {
    "render the correct content in english" in {
      val email = "a@a.com"
      val document = Jsoup.parse(account_details_update_email_address_verify_email(email)(AuthenticatedRequest(FakeRequest("GET", "/"), None, None, None, None), applicationMessages, TestFixtures.sampleHostContext).toString())

      document.getElementsByTag("title").first().text() shouldBe "Check your email address"
      document.getElementsByTag("h1").get(0).text() shouldBe "Check your email address"
      document.getElementsByTag("p").get(1).text() shouldBe s"Are you sure $email is correct?"
      document.getElementById("emailIsCorrectLink").text() shouldBe "This email address is correct"
      document.getElementById("emailIsNotCorrectLink").text() shouldBe "Change this email address"
    }

    "render the correct content in welsh" in {
      val email = "a@a.com"
      val document = Jsoup.parse(account_details_update_email_address_verify_email(email)(welshRequest, messagesInWelsh(applicationMessages), TestFixtures.sampleHostContext).toString())

      document.getElementsByTag("title").first().text() shouldBe "Gwirio'ch cyfeiriad e-bost"
      document.getElementsByTag("h1").get(0).text() shouldBe "Gwirio'ch cyfeiriad e-bost"
      document.getElementsByTag("p").get(1).text() shouldBe s"A ydych yn siŵr bod $email yn gywir?"
      document.getElementById("emailIsCorrectLink").text() shouldBe "Mae'r cyfeiriad e-bost hwn yn gywir"
      document.getElementById("emailIsNotCorrectLink").text() shouldBe "Newid y cyfeiriad e-bost hwn"
    }
  }
}
