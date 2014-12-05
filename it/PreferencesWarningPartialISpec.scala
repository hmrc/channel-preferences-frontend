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

    "be empty if user has opted out" in new TestCase {
      `/portal/preferences/sa/individual`.postOptOut(utr) should have(status(201))

      val response = `/account/preferences/warnings`.withHeaders(authenticationCookie(userId, password)).get

      response should have(status(204))
    }

    "have warning content for pending unverified email" in new TestCase {
      val email = s"${UUID.randomUUID().toString}@email.com"
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))

      val response =`/account/preferences/warnings`.withHeaders(authenticationCookie(userId, password)).get

      response should have(status(200))
    }

    "have warning content for bounced" ignore new TestCase{
      val email = s"${UUID.randomUUID().toString}@email.com"
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))

      val response =`/account/preferences/warnings`.withHeaders(authenticationCookie(userId, password)).get

      response should have(status(200))
    }
  }
  
  val todayDate = DateFormat.longDateFormat(Some(DateTimeUtils.now.toLocalDate)).get.body
}
