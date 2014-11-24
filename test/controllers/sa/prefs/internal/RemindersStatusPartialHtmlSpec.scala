package controllers.sa.prefs.internal

import connectors.SaEmailPreference.Status
import connectors.{PreferencesConnector, SaEmailPreference, SaPreference}
import controllers.sa.prefs.AuthorityUtils._
import controllers.sa.prefs.internal.routes.{BizTaxPrefsController, AccountDetailsController}
import org.scalatest.concurrent.ScalaFutures
import play.api.test.FakeRequest
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.connectors.HeaderCarrier
import uk.gov.hmrc.play.http.{HttpPut, HttpPost, HttpGet}
import uk.gov.hmrc.play.test.{WithFakeApplication, WithHeaderCarrier}
import uk.gov.hmrc.test.UnitSpec
import scala.concurrent.Future

class RemindersStatusPartialHtmlSpec extends UnitSpec with WithHeaderCarrier with WithFakeApplication with ScalaFutures {

  "Reminders Partial Html" should {
    val utr = "1234567890"
    implicit val hc = new HeaderCarrier()
    implicit val request = FakeRequest("GET", "/portal/sa/123456789")
    implicit val saUser = User(userId = "userId", userAuthority = saAuthority("userId", utr))

    "contain pending email details in content when opted-in and unverified" in new TestCase {
      val emailPreferences = SaEmailPreference("test@test.com", Status.pending, false)
      val saPreference = SaPreference(true, Some(emailPreferences))

      val partialHtml = new PartialHtml(Some(saPreference)).detailsStatus(SaUtr(utr)).futureValue

      partialHtml.body should (
        include(emailPreferences.email) and
          include("Send verification email") and
          include(`/account/account-details/sa/resend-validation-email`) and
          include(`/account/account-details/sa/opt-out-email-reminders`) and
          include(`/account/account-details/sa/update-email-address`)
        )
    }

    "contain verified email details in content when opted-in and verified" in new TestCase {
      val emailPreferences: SaEmailPreference = SaEmailPreference("test@test.com", Status.verified, false)
      val saPreference = SaPreference(true, Some(emailPreferences))
      val partialHtml = new PartialHtml(Some(saPreference)).detailsStatus(SaUtr(utr)).futureValue

      partialHtml.body should (
        include("Reminders are sent to") and
          include(EmailAddress(emailPreferences.email).obfuscated) and
          include(`/account/account-details/sa/update-email-address`) and
          include(`/account/account-details/sa/opt-out-email-reminders`) and
          not include(`/account/account-details/sa/resend-validation-email`)
        )
    }

    "contain bounced email with 'mailbox filled up' details in content when the 'current' email is bounced with full mailbox error" in new TestCase {
      val emailPreferences: SaEmailPreference = SaEmailPreference("test@test.com", Status.bounced, true)
      val saPreference = SaPreference(true, Some(emailPreferences))
      val partialHtml = new PartialHtml(Some(saPreference)).detailsStatus(SaUtr(utr)).futureValue

      partialHtml.body should (
        include("You need to verify") and
          include(emailPreferences.email) and
          include("your inbox is full") and
          include(`/account/account-details/sa/update-email-address`) and
          include(`/account/account-details/sa/opt-out-email-reminders`) and
          not include(`/account/account-details/sa/resend-validation-email`)
        )
    }

    "contain bounced email but no 'full mailbox' details in content when the 'current' email is bounced with other error" in new TestCase {
      val emailPreferences: SaEmailPreference = SaEmailPreference("test@test.com", Status.bounced, false)
      val saPreference = SaPreference(true, Some(emailPreferences))
      val partialHtml = new PartialHtml(Some(saPreference)).detailsStatus(SaUtr(utr)).futureValue

      partialHtml.body should (
        include("You need to verify") and
          include(emailPreferences.email) and
          include("can’t be delivered") and
          include(`/account/account-details/sa/update-email-address`) and
          include(`/account/account-details/sa/opt-out-email-reminders`) and
          not include("your inbox is full") and
          not include(`/account/account-details/sa/resend-validation-email`)
        )
    }

    "contain opted out details in content when user is opted-out" in new TestCase {
      val saPreference: SaPreference = SaPreference(false, None)
      val partialHtml = new PartialHtml(Some(saPreference)).detailsStatus(SaUtr(utr)).futureValue

      partialHtml.body should (
        include("Replace the letters you get about Self Assessment with emails") and
          include(`/account/account-details/sa/opt-in-email-reminders`) and
          not include(`/account/account-details/sa/resend-validation-email`)
        )
    }

    "contain opted out details in content when user has no preference set" in new TestCase {
      val partialHtml = new PartialHtml(None).detailsStatus(SaUtr(utr)).futureValue

      partialHtml.body should (
        include("Replace the letters you get about Self Assessment with emails") and
          include(`/account/account-details/sa/opt-in-email-reminders`) and
          not include(`/account/account-details/sa/resend-validation-email`)
        )
    }

  }
}

class TestCase {

  class PartialHtml(saPreference: Option[SaPreference]) extends RemindersStatusPartialHtml {

    override val preferencesConnector = new PreferencesConnector {
      override def http: HttpGet with HttpPost with HttpPut = ???

      override def serviceUrl = ???

      override def getPreferences(utr: SaUtr)(implicit headerCarrier: HeaderCarrier): Future[Option[SaPreference]] = Future.successful(saPreference)
    }
  }

  val `/account/account-details/sa/update-email-address` = AccountDetailsController.changeEmailAddress(None).toString
  val `/account/account-details/sa/resend-validation-email` = AccountDetailsController.resendValidationEmail.toString
  val `/account/account-details/sa/opt-out-email-reminders` = AccountDetailsController.optOutOfEmailReminders.toString
  val `/account/account-details/sa/opt-in-email-reminders` = BizTaxPrefsController.displayPrefsForm(None).toString
}
