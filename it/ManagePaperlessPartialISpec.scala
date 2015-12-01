
import java.util.UUID

import org.scalatest.BeforeAndAfterEach

class ManagePaperlessPartialISpec
  extends PreferencesFrontEndServer
  with BeforeAndAfterEach
  with EmailSupport {

  "Manage Paperless partial" should {

    "return not authorised when no credentials supplied" in new TestCase {
      `/paperless/manage`(returnUrl = "http://some/other/url", returnLinkText = "Continue").get should have(status(401))
    }

    "return opted out details when no preference is set" in new TestCaseWithFrontEndAuthentication {
      private val request = `/paperless/manage`(returnUrl = "http://some/other/url", returnLinkText = "Continue").withHeaders(cookie)
      val response = request.get()
      response should have(status(200))
      response.futureValue.body should (
        include("Sign up for paperless notifications") and
        not include "You need to verify"
      )
    }

    // FIXME remove when YTA no longer use these endpoints
    "be supported on the deprecated URL" in new TestCaseWithFrontEndAuthentication {
      private val request = `/account/account-details/sa/email-reminders-status`.withHeaders(cookie)
      val response = request.get()
      response should have(status(200))
      response.futureValue.body should (
        include("Sign up for paperless notifications") and
        not include "You need to verify"
      )
    }
  }

  "Manage Paperless partial for pending verification" should {

    "contain pending email verification details" in new TestCaseWithFrontEndAuthentication {
      val email = s"${UUID.randomUUID().toString}@email.com"
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))

      val response = `/paperless/manage`(returnUrl = "http://some/other/url", returnLinkText = "Continue").withHeaders(cookie).get()
      response should have(status(200))
      response.futureValue.body should include(s"You need to verify")
    }

    "contain new email details for a subsequent change email" in new TestCaseWithFrontEndAuthentication {
      val email = s"${UUID.randomUUID().toString}@email.com"
      val newEmail = s"${UUID.randomUUID().toString}@email.com"
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))
      `/portal/preferences/sa/individual`.postPendingEmail(utr, newEmail) should have(status(200))
      val response = `/paperless/manage`(returnUrl = "http://some/other/url", returnLinkText = "Continue").withHeaders(cookie).get()
      response should have(status(200))
      checkForChangedEmailDetailsInResponse(response.futureValue.body, email, newEmail, todayDate)
    }

    "contain sign up details for a subsequent opt out" in new TestCase with TestCaseWithFrontEndAuthentication {
      val email = s"${UUID.randomUUID().toString}@email.com"
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))
      `/portal/preferences/sa/individual`.postOptOut(utr, authHeader) should have(status(200))
      val response = `/paperless/manage`(returnUrl = "http://some/other/url", returnLinkText = "Continue").withHeaders(cookie).get()
      response should have(status(200))
      response.futureValue.body should (
        not include email and
        include(s"Sign up for paperless notifications")
      )
    }
  }

  "Manage Paperless partial for verified user" should {

    "contain new email details for a subsequent change email" in new TestCaseWithFrontEndAuthentication {
      val email = s"${UUID.randomUUID().toString}@email.com"
      val newEmail = s"${UUID.randomUUID().toString}@email.com"
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))
      `/preferences-admin/sa/individual`.verifyEmailFor(utr) should have(status(204))
      `/portal/preferences/sa/individual`.postPendingEmail(utr, newEmail) should have(status(200))
      val response = `/paperless/manage`(returnUrl = "http://some/other/url", returnLinkText = "Continue").withHeaders(cookie).get()
      response should have(status(200))
      checkForChangedEmailDetailsInResponse(response.futureValue.body, email, newEmail, todayDate)
    }

    "contain sign up details for a subsequent opt out" in new TestCaseWithFrontEndAuthentication {
      val email = s"${UUID.randomUUID().toString}@email.com"
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))
      `/preferences-admin/sa/individual`.verifyEmailFor(utr) should have(status(204))
      `/portal/preferences/sa/individual`.postOptOut(utr, authHeader) should have(status(200))
      val response = `/paperless/manage`(returnUrl = "http://some/other/url", returnLinkText = "Continue").withHeaders(cookie).get()
      response should have(status(200))
      response.futureValue.body should (
        not include email and
          include(s"Sign up for paperless notifications"))
    }
  }

  "Manage Paperless partial for a bounced verification email" should {

    "contain new email details for a subsequent change email" in new TestCaseWithFrontEndAuthentication {
      val email = s"${UUID.randomUUID().toString}@email.com"
      val newEmail = s"${UUID.randomUUID().toString}@email.com"
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))
      `/preferences-admin/sa/bounce-email`.post(email) should have(status(204))
      `/portal/preferences/sa/individual`.postPendingEmail(utr, newEmail) should have(status(200))
      val response = `/paperless/manage`(returnUrl = "http://some/other/url", returnLinkText = "Continue").withHeaders(cookie).get()
      response should have(status(200))
      checkForChangedEmailDetailsInResponse(response.futureValue.body, email, newEmail, todayDate)
    }

    "contain sign up details for a subsequent opt out" in new TestCaseWithFrontEndAuthentication {
      val email = s"${UUID.randomUUID().toString}@email.com"
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))
      `/preferences-admin/sa/bounce-email`.post(email) should have(status(204))
      `/portal/preferences/sa/individual`.postOptOut(utr, authHeader) should have(status(200))
      val response = `/paperless/manage`(returnUrl = "http://some/other/url", returnLinkText = "Continue").withHeaders(cookie).get()
      response should have(status(200))
      response.futureValue.body should (
        not include email and
          include(s"Sign up for paperless notifications"))
    }

  }

  override def beforeEach() = {
    val testCase = new TestCase()
    testCase.`/preferences-admin/sa/individual`.deleteAll should have(status(200))
  }

  def checkForChangedEmailDetailsInResponse(response: String, oldEmail: String, newEmail: String, currentFormattedDate: String) = {
    response should (
      include(s"You need to verify your email address.") and
        include(newEmail) and
        not include oldEmail and
        include(s"on $currentFormattedDate. Click on the link in the email to verify your email address."))
  }
}
