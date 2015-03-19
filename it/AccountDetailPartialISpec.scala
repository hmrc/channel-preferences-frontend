
import java.util.UUID

import org.scalatest.BeforeAndAfterEach

class AccountDetailPartialISpec
  extends PreferencesFrontEndServer
  with BeforeAndAfterEach
  with EmailSupport {

  "Account detail partial" should {

    "return not authorised when no credentials supplied" in new TestCase {
      `/email-reminders-status`.get should have(status(401))
    }

    //TODO: Fix this one
//    "return not authorised when not enrolled in SA" in new TestCase {
//      `/email-reminders-status`.withHeaders(authenticationCookie(userId = "643212300020", password)).get should have (status(401))
//    }

    "return opted out details when no preference is set" in new TestCaseWithFrontEndAuthentication {
      private val request = `/email-reminders-status`.withHeaders(cookie)
      val response = request.get
      response should have(status(200))
      response.futureValue.body should (
        include("Self Assessment email reminders") and
          not include ("You need to verify")
        )
    }
  }

  "Account detail partial for pending verification" should {

    "contain pending email verification details" in new TestCaseWithFrontEndAuthentication {
      val email = s"${UUID.randomUUID().toString}@email.com"
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))

      val response = `/email-reminders-status`.withHeaders(cookie).get
      response should have(status(200))
      response.futureValue.body should (
        include(s"You need to verify")
        )
    }

    "contain new email details for a subsequent change email" in new TestCaseWithFrontEndAuthentication {
      val email = s"${UUID.randomUUID().toString}@email.com"
      val newEmail = s"${UUID.randomUUID().toString}@email.com"
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))
      `/portal/preferences/sa/individual`.postPendingEmail(utr, newEmail) should have(status(201))
      val response = `/email-reminders-status`.withHeaders(cookie).get
      response should have(status(200))
      checkForChangedEmailDetailsInResponse(response.futureValue.body, email, newEmail, todayDate)
    }

    "contain sign up details for a subsequent opt out" in new TestCase with TestCaseWithFrontEndAuthentication {
      val email = s"${UUID.randomUUID().toString}@email.com"
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))
      `/portal/preferences/sa/individual`.postOptOut(utr) should have(status(201))
      val response = `/email-reminders-status`.withHeaders(cookie).get
      response should have(status(200))
      response.futureValue.body should(
        not include(email) and
          include(s"Sign up for Self Assessment email reminders"))
    }
  }

  "Account detail partial for verified user" should {

    "contain new email details for a subsequent change email" in new TestCaseWithFrontEndAuthentication {
      val email = s"${UUID.randomUUID().toString}@email.com"
      val newEmail = s"${UUID.randomUUID().toString}@email.com"
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))
      `/preferences-admin/sa/individual`.verifyEmailFor(utr) should have(status(204))
      `/portal/preferences/sa/individual`.postPendingEmail(utr, newEmail) should have(status(201))
      val response = `/email-reminders-status`.withHeaders(cookie).get
      response should have(status(200))
      checkForChangedEmailDetailsInResponse(response.futureValue.body, email, newEmail, todayDate)
    }

    "contain sign up details for a subsequent opt out" in new TestCaseWithFrontEndAuthentication {
      val email = s"${UUID.randomUUID().toString}@email.com"
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))
      `/preferences-admin/sa/individual`.verifyEmailFor(utr) should have(status(204))
      `/portal/preferences/sa/individual`.postOptOut(utr) should have(status(201))
      val response = `/email-reminders-status`.withHeaders(cookie).get
      response should have(status(200))
      response.futureValue.body should(
        not include(email) and
          include(s"Sign up for Self Assessment email reminders"))
    }
  }

  "Account detail partial for a bounced verification email" should {

    "contain new email details for a subsequent change email" in new TestCaseWithFrontEndAuthentication {
      val email = s"${UUID.randomUUID().toString}@email.com"
      val newEmail = s"${UUID.randomUUID().toString}@email.com"
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))
      `/preferences-admin/sa/bounce-email`.post(email) should have(status(204))
      `/portal/preferences/sa/individual`.postPendingEmail(utr, newEmail) should have(status(201))
      val response = `/email-reminders-status`.withHeaders(cookie).get
      response should have(status(200))
      checkForChangedEmailDetailsInResponse(response.futureValue.body, email, newEmail, todayDate)
    }

    "contain sign up details for a subsequent opt out" in new TestCaseWithFrontEndAuthentication {
      val email = s"${UUID.randomUUID().toString}@email.com"
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))
      `/preferences-admin/sa/bounce-email`.post(email) should have(status(204))
      `/portal/preferences/sa/individual`.postOptOut(utr) should have(status(201))
      val response = `/email-reminders-status`.withHeaders(cookie).get
      response should have(status(200))
      response.futureValue.body should(
        not include(email) and
          include(s"Sign up for Self Assessment email reminders"))
    }

  }

  override def beforeEach() = {
    val testCase = new TestCase()
    testCase.`/preferences-admin/sa/individual`.deleteAll should have(status(200))
  }

  def checkForChangedEmailDetailsInResponse(response: String, oldEmail: String, newEmail: String, currentFormattedDate: String) = {
    response should (
      include(s"You need to verify your email address with HMRC") and
      include(newEmail) and
      not include(oldEmail) and
      include(s"on $currentFormattedDate. Click on the link in the email to verify your email address."))
  }

}
