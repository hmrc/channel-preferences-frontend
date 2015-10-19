package partial

import java.net.URLEncoder

import connectors.SaEmailPreference.Status
import connectors.{SaEmailPreference, SaPreference}
import helpers.ConfigHelper
import org.joda.time.LocalDate
import org.scalatest.concurrent.ScalaFutures
import play.api.test.FakeRequest
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.http.test.WithHeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class ManagePaperlessPartialSpec extends UnitSpec with WithHeaderCarrier with WithFakeApplication with ScalaFutures {
  override lazy val fakeApplication = ConfigHelper.fakeApp

  val sampleReturnUrl = "https://host:3453/some/path"

  "Manage Paperless partial" should {
    implicit val request = FakeRequest("GET", "/portal/sa/123456789")

    "contain pending email details in content when opted-in and unverified" in {
      val emailPreferences = SaEmailPreference(email = "test@test.com",
        status = Status.pending,
        mailboxFull = false,
        linkSent = Some(new LocalDate(2014,10,2)))
      val saPreference = SaPreference(digital = true, Some(emailPreferences))

      ManagePaperlessPartial(Some(saPreference), sampleReturnUrl).body should (
        include(emailPreferences.email) and
        include("send a new verification email") and
        include("/paperless/resend-validation-email?returnUrl=" + URLEncoder.encode(sampleReturnUrl, "UTF-8")) and
        include("/account/account-details/sa/opt-out-email-reminders") and
        include("/account/account-details/sa/update-email-address") and
        include("2 October 2014")
      )
    }

    "contain verified email details in content when opted-in and verified" in {
      val emailPreferences: SaEmailPreference = SaEmailPreference("test@test.com", Status.verified, false)
      val saPreference = SaPreference(true, Some(emailPreferences))

      ManagePaperlessPartial(Some(saPreference), sampleReturnUrl).body should (
        include("Emails are sent to") and
        include(EmailAddress(emailPreferences.email).obfuscated) and
        include("/account/account-details/sa/update-email-address") and
        include("/account/account-details/sa/opt-out-email-reminders") and
        not include "/paperless/resend-validation-email"
      )
    }

    "contain bounced email with 'mailbox filled up' details in content when the 'current' email is bounced with full mailbox error" in {
      val emailPreferences: SaEmailPreference = SaEmailPreference("test@test.com", Status.bounced, true)
      val saPreference = SaPreference(true, Some(emailPreferences))

      ManagePaperlessPartial(Some(saPreference), sampleReturnUrl).body should (
        include("You need to verify") and
        include(emailPreferences.email) and
        include("your inbox is full") and
        include("/account/account-details/sa/update-email-address") and
        include("/account/account-details/sa/opt-out-email-reminders") and
        not include "/paperless/resend-validation-email"
      )
    }

    "contain bounced email but no 'full mailbox' details in content when the 'current' email is bounced with other error" in {
      val emailPreferences: SaEmailPreference = SaEmailPreference("test@test.com", Status.bounced, false)
      val saPreference = SaPreference(true, Some(emailPreferences))

      ManagePaperlessPartial(Some(saPreference), sampleReturnUrl).body should (
        include("You need to verify") and
        include(emailPreferences.email) and
        include("can't be delivered") and
        include("/account/account-details/sa/update-email-address") and
        include("/account/account-details/sa/opt-out-email-reminders") and
        not include "your inbox is full" and
        not include "/paperless/resend-validation-email"
      )
    }

    "contain opted out details in content when user is opted-out" in {
      val saPreference: SaPreference = SaPreference(false, None)

      ManagePaperlessPartial(Some(saPreference), sampleReturnUrl).body should (
        include("Replace the letters you get about taxes with emails.") and
        include("/account/account-details/sa/opt-in-email-reminders") and
        not include "/paperless/resend-validation-email"
      )
    }

    "contain opted out details in content when user has no preference set" in {
      ManagePaperlessPartial(None, sampleReturnUrl).body should (
        include("Replace the letters you get about taxes with emails.") and
        include("/account/account-details/sa/opt-in-email-reminders") and
        not include "/paperless/resend-validation-email"
      )
    }
  }
}

