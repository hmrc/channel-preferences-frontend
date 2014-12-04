import java.util.UUID

import uk.gov.hmrc.time.DateTimeUtils
import views.sa.prefs.helpers.DateFormat

class PreferencesWarningPartialISpec
  extends PreferencesFrontEndServer
  with UserAuthentication {

  "partial html for pending verification email" should {

    "return not authorised when no credentials supplied" in new TestCase {
      `/account/preferences/warnings`.get() should have(status(401))
    }

    "be empty if email is already verified" in new TestCase {
      val email = s"${UUID.randomUUID().toString}@email.com"
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))
      `/preferences-admin/sa/individual`.verifyEmail(utr) should have(status(204))

      val response = `/account/preferences/warnings`.withHeaders(authenticationCookie(userId, password)).get

      response should have(status(204))
    }

    "be empty if user has opted out" in new TestCase {
      `/portal/preferences/sa/individual`.postOptOut(utr) should have(status(201))

      val response = `/account/preferences/warnings`.withHeaders(authenticationCookie(userId, password)).get

      response should have(status(204))
    }

    "contain last verification email sent date and email address" in new TestCase {
      val email = s"${UUID.randomUUID().toString}@email.com"
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))

      val response =`/account/preferences/warnings`.withHeaders(authenticationCookie(userId, password)).get

      response should have(status(200))
      response.futureValue.body should (
          include("Verify your Self Assessment email address") and
          include(email) and
          include(todayDate) and
          include("Your details")
        )
    }
  }
  
  val todayDate = DateFormat.longDateFormat(Some(DateTimeUtils.now.toLocalDate)).get.body
}
