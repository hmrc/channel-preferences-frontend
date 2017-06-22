package partial

import connectors.SaEmailPreference.Status
import connectors.{PreferenceResponse, SaEmailPreference, SaPreference}
import controllers.ExternalUrlPrefixes
import controllers.internal.routes
import helpers.{ConfigHelper, TestFixtures}
import org.joda.time.LocalDate
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.OneAppPerSuite
import partial.paperless.manage.ManagePaperlessPartial
import play.api.Application
import play.api.mvc.Call
import play.api.test.FakeRequest
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import connectors.PreferenceResponse._
import model.Encrypted
import play.api.libs.json.Json

class ManagePaperlessPartialSpec extends UnitSpec with OneAppPerSuite with ScalaFutures {
  implicit val hc = HeaderCarrier()

  implicit val hostContext = TestFixtures.sampleHostContext
  override implicit lazy val app : Application = ConfigHelper.fakeApp

  def linkTo(s: Call) = ExternalUrlPrefixes.pfUrlPrefix + s.url.replaceAll("&", "&amp;")

  "Manage Paperless partial" should {
    implicit val request = FakeRequest("GET", "/portal/sa/123456789")

    "contain pending email details in content when opted-in and unverified" in {
      val emailPreferences = SaEmailPreference(email = "test@test.com",
        status = Status.Pending,
        mailboxFull = false,
        linkSent = Some(new LocalDate(2014,10,2)))
      val saPreference = SaPreference(digital = true, Some(emailPreferences)).toNewPreference()

      ManagePaperlessPartial(Some(saPreference)).body should (
        include("Email for paperless notifications") and
        include(emailPreferences.email) and
        include("send a new verification email") and
        include(linkTo(routes.ManagePaperlessController.resendVerificationEmail(hostContext))) and
        include(linkTo(routes.ManagePaperlessController.displayStopPaperless(hostContext))) and
        include(linkTo(routes.ManagePaperlessController.displayChangeEmailAddress(None, hostContext))) and
        include("2 October 2014")
      )
    }

    "contain verified email details in content when opted-in and verified" in {
      val emailPreferences: SaEmailPreference = SaEmailPreference("test@test.com", Status.Verified, false)
      val saPreference = SaPreference(true, Some(emailPreferences)).toNewPreference()

      ManagePaperlessPartial(Some(saPreference)).body should (
        include("Email address for paperless notifications") and
        include("Emails are sent to") and
        include(EmailAddress(emailPreferences.email).obfuscated) and
        include(linkTo(routes.ManagePaperlessController.displayChangeEmailAddress(None, hostContext))) and
        include(linkTo(routes.ManagePaperlessController.displayStopPaperless(hostContext))) and
        not include linkTo(routes.ManagePaperlessController.resendVerificationEmail(hostContext))
      )
    }

    "contain bounced email with 'mailbox filled up' details in content when the 'current' email is bounced with full mailbox error" in {
      val emailPreferences: SaEmailPreference = SaEmailPreference("test@test.com", Status.Bounced, mailboxFull = true)
      val saPreference = SaPreference(true, Some(emailPreferences)).toNewPreference()

      ManagePaperlessPartial(Some(saPreference)).body should (
        include("You need to verify") and
        include(emailPreferences.email) and
        include("your inbox is full") and
        include(linkTo(routes.ManagePaperlessController.displayChangeEmailAddress(None, hostContext))) and
        include(linkTo(routes.ManagePaperlessController.displayStopPaperless(hostContext))) and
        not include linkTo(routes.ManagePaperlessController.resendVerificationEmail(hostContext))
      )
    }

    "contain bounced email with 'email can't be delivered' in content when the 'current' email is bounced with email can't be delivered error" in {
      val emailPreferences: SaEmailPreference = SaEmailPreference("test@test.com", Status.Bounced, mailboxFull = false)
      val saPreference = SaPreference(true, Some(emailPreferences)).toNewPreference()

      ManagePaperlessPartial(Some(saPreference)).body should (
        include("You need to verify") and
        include(emailPreferences.email) and
        include("The email telling you how to do this can't be delivered.") and
        include(linkTo(routes.ManagePaperlessController.displayChangeEmailAddress(None, hostContext))) and
        include(linkTo(routes.ManagePaperlessController.displayStopPaperless(hostContext))) and
        not include linkTo(routes.ManagePaperlessController.resendVerificationEmail(hostContext))
      )
    }

    "contain bounced email but no 'full mailbox' details in content when the 'current' email is bounced with other error" in {
      val emailPreferences: SaEmailPreference = SaEmailPreference("test@test.com", Status.Bounced, false)
      val saPreference = SaPreference(true, Some(emailPreferences)).toNewPreference()

      ManagePaperlessPartial(Some(saPreference)).body should (
        include("You need to verify") and
        include(emailPreferences.email) and
        include("can't be delivered") and
        include(linkTo(routes.ManagePaperlessController.displayChangeEmailAddress(None, hostContext))) and
        include(linkTo(routes.ManagePaperlessController.displayStopPaperless(hostContext))) and
        not include "your inbox is full" and
        not include linkTo(routes.ManagePaperlessController.resendVerificationEmail(hostContext))
      )
    }

    "contain opted out details in content when user is opted-out" in {
      val saPreference =  SaPreference(false, None).toNewPreference()

      ManagePaperlessPartial(Some(saPreference)).body should (
        include("Replace the letters you get about taxes with emails.") and
        include(linkTo(routes.ChoosePaperlessController.redirectToDisplayFormWithCohort(None, hostContext))) and
        not include linkTo(routes.ManagePaperlessController.resendVerificationEmail(hostContext))
      )
    }

    "contain opted out details in content when user has no preference set" in {
      ManagePaperlessPartial(None).body should (
        include("Replace the letters you get about taxes with emails.") and
        include(linkTo(routes.ChoosePaperlessController.redirectToDisplayFormWithCohort(None, hostContext))) and
        not include linkTo(routes.ManagePaperlessController.resendVerificationEmail(hostContext))
      )
    }


    "contain existing email if user has one already (Opted in for taxCredits but not generic we will prepopulate the email address)" in {
      val taxCreditsOptedInPreference = Json.parse(
        s"""{
           |  "termsAndConditions": {
           |    "taxCredits": {
           |      "accepted": true
           |    }
           |  },
           |  "email": {
           |    "email": "pihklyljtgoxeoh@mail.com",
           |    "isVerified": true,
           |    "hasBounces": false,
           |    "mailboxFull": false,
           |    "status": "verified"
           |  },
           |  "digital": true
           |}""".stripMargin
      ).as[PreferenceResponse]

      ManagePaperlessPartial(Some(taxCreditsOptedInPreference)).body should (
        include("Replace the letters you get about taxes with emails.") and
          include(linkTo(routes.ChoosePaperlessController.redirectToDisplayFormWithCohort(Some(Encrypted(EmailAddress("pihklyljtgoxeoh@mail.com"))), hostContext))) and
          not include linkTo(routes.ManagePaperlessController.resendVerificationEmail(hostContext))
        )
    }
  }
}

