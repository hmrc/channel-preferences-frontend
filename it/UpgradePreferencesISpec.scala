import play.api.libs.ws.WS
import play.api.mvc.Results.EmptyContent

class UpgradePreferencesISpec extends PreferencesFrontEndServer with EmailSupport {

  "Upgrading preferences should" should {
    "leave the preference opted-in for all form types"  in new UpgradeTestCase  {
      val email = uniqueEmail
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should (have(status(200)) or have(status(201)))
      `/preferences-admin/sa/individual`.verifyEmailFor(utr)

      val response = `/upgrade-email-reminders`.post().futureValue
      response.status should be (200)

      //TODO: Add check that teh prefernce has been updated in preferences
    }
  }

  trait UpgradeTestCase extends TestCaseWithFrontEndAuthentication {
    import play.api.Play.current

    val `/upgrade-email-reminders` = new {
      def post() = WS.url(resource("/account/account-details/sa/upgrade-email-reminders")).withHeaders(cookie).post(EmptyContent())
    }
  }
}
