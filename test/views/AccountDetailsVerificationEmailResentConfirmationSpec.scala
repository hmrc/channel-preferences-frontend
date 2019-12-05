package views

import _root_.helpers.{ConfigHelper, TestFixtures, WelshLanguage}
import controllers.auth.AuthenticatedRequest
import org.jsoup.Jsoup
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec
import views.html.account_details_verification_email_resent_confirmation

class AccountDetailsVerificationEmailResentConfirmationSpec extends UnitSpec with OneAppPerSuite with WelshLanguage {

  override implicit lazy val app: Application = ConfigHelper.fakeApp

  "account details verification email resent confirmation template" should {
    "render the correct content in english" in {
      val email = "a@a.com"
      val document = Jsoup.parse(account_details_verification_email_resent_confirmation(email)(AuthenticatedRequest(FakeRequest("GET", "/"), None, None, None, None), applicationMessages, TestFixtures.sampleHostContext).toString())

      document.getElementsByTag("title").first().text() shouldBe "Verification email sent"
      document.getElementsByTag("h1").get(0).text() shouldBe "Verification email sent"
      document.getElementById("verification-mail-message").text() shouldBe s"A new email has been sent to $email. Click on the link in the email to verify your email address with HMRC."
    }

    "render the correct content in welsh" in {
      val email = "a@a.com"
      val document = Jsoup.parse(account_details_verification_email_resent_confirmation(email)(welshRequest, messagesInWelsh(applicationMessages), TestFixtures.sampleHostContext).toString())

      document.getElementsByTag("title").first().text() shouldBe "E-bost dilysu wedi'i anfon"
      document.getElementsByTag("h1").get(0).text() shouldBe "E-bost dilysu wedi'i anfon"
      document.getElementById("verification-mail-message").text() shouldBe s"Mae e-bost newydd wedi'i anfon i $email. Cliciwch ar y cysylltiad yn yr e-bost er mwyn dilysu'ch cyfeiriad e-bost gyda CThEM."
    }
  }
}
