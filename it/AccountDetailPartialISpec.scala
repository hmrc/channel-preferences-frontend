import java.util.UUID

import org.scalatest.BeforeAndAfterEach


class AccountDetailPartialISpec
  extends PreferencesFrontEndServer
  with UserAuthentication
  with BeforeAndAfterEach
  with EmailSupport {

  "Account detail partial" should {
    "return not authorised when no credentials supplied" in new TestCase {
      `/email-reminders-status`.get should have(status(401))
    }

    "return not authorised when not enrolled in SA" in new TestCase {
      `/email-reminders-status`.withHeaders(authenticationCookie(userId = "643212300020", password)).get should have (status(401))
    }

    "return opted out details when no preference is set" in new TestCase {
      val response = `/email-reminders-status`.withHeaders(authenticationCookie(userId, password)).get
      response should have(status(200))
      response.futureValue.body should (
          include("Self Assessment email reminders") and
          not include ("You need to verify")
        )
    }

    "contain pending details when a pending email is present" in new TestCase {
      val email = s"${UUID.randomUUID().toString}@email.com"
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))

      val response = `/email-reminders-status`.withHeaders(authenticationCookie(userId, password)).get
      response should have(status(200))
      response.futureValue.body should (
        include(s"You need to verify")
      )
    }
  }

  override def beforeEach() = {
    val testCase = new TestCase()
    testCase.`/preferences-admin/sa/individual`.deleteAll should have(status(200))
  }

}