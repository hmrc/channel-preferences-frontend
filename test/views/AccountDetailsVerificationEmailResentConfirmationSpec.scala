package views

import _root_.helpers.{ConfigHelper, TestFixtures}
import org.jsoup.Jsoup
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec
import views.html.account_details_verification_email_resent_confirmation

class AccountDetailsVerificationEmailResentConfirmationSpec extends UnitSpec with OneAppPerSuite {

  override implicit lazy val app: Application = ConfigHelper.fakeApp

  "preference print template" should {
    "render the correct content for the IPage cohort " in {
      val email = "a@a.com"
      val document = Jsoup.parse(account_details_verification_email_resent_confirmation(email)(None, FakeRequest("GET", "/"), applicationMessages, TestFixtures.sampleHostContext).toString())

      document.getElementsByTag("title").first().text() shouldBe "Verification email sent"
      document.getElementsByTag("h1").get(0).text() shouldBe "Verification email sent"
      document.getElementById("verification-mail-message").text() shouldBe s"A new email has been sent to $email. Click on the link in the email to verify your email address with HMRC."
    }
  }
}
