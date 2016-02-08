import org.scalatest.BeforeAndAfterEach
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.domain.SaUtr

class GoPaperlessPageISpec extends PreferencesFrontEndServer with BeforeAndAfterEach{

  "go paperless page" should {
    "have a digital false response from get preference for opted out user" in new GoPaperlessTestCase{
      createOptedOutPreference
      val response = `/preferences/sa/individual/utr/print-suppression`(authHeader).getPreference(utr).futureValue
      response should have ('status(200))
      response.body should include(""""digital":false""")
    }

    "have a digital true response from get preference for opted in user" in new GoPaperlessTestCase{
      createOptedInPreference should (have('status(200)) or have('status(201)))
      val response = `/preferences/sa/individual/utr/print-suppression`(authHeader).getPreference(utr).futureValue
      response should have ('status(200))
      response.body should include(""""digital":true""")
    }

    "have a not found response from get preference for a user with no preference" in new GoPaperlessTestCase{
      `/preferences-admin/sa/individual`.delete(utr)
      val response = `/preferences/sa/individual/utr/print-suppression`(authHeader).getPreference(utr).futureValue
      response should have ('status(404))
      response.body should include (s"Preferences for '${utr}' not found")
    }
  }

  trait GoPaperlessTestCase extends TestCaseWithFrontEndAuthentication{

    val authHeader =  createGGAuthorisationHeader(SaUtr(utr))

    def createOptedOutPreference(): WSResponse = {
      await(`/portal/preferences/sa/individual`.postOptOut(utr))
    }

    def createOptedInPreference(): WSResponse = {
      await(`/preferences-admin/sa/individual`.delete(utr))
      `/portal/preferences/sa/individual`.postPendingEmail(utr, uniqueEmail)
    }
  }
}
