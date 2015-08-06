import connectors.PreferencesConnector
import controllers.sa.prefs.Encrypted
import controllers.sa.prefs.internal.{routes, UpgradeRemindersController}
import org.scalatest.mock.MockitoSugar
import play.api.libs.ws.{WS, WSResponse}
import uk.gov.hmrc.crypto.{PlainText, ApplicationCrypto}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

class UpgradePreferencesISpec extends PreferencesFrontEndServer with EmailSupport with MockitoSugar {

  "Upgrading preferences should" should {

    "set upgraded to paperless and allow subsequent activation"  in new UpgradeTestCase  {

      createOptedInVerifiedPreferenceWithNino()

      `/preferences/paye/individual/:nino/activations/paye`(nino,authHeader).put().futureValue.status should be (412)

      val response = `/upgrade-email-reminders`.post(submitButton = "digital").futureValue
      response should have('status(303))
      response.header("Location").get should be (routes.UpgradeRemindersController.thankYou(Encrypted(returnUrl)).toString())

      `/preferences/paye/individual/:nino/activations/paye`(nino, authHeader).put().futureValue.status should be (200)
    }

    "set not upgraded to paperless and don't allow subsequent activation"  in new UpgradeTestCase  {

      createOptedInVerifiedPreferenceWithNino()

      `/preferences/paye/individual/:nino/activations/paye`(nino,authHeader).put().futureValue.status should be (412)

      val response = `/upgrade-email-reminders`.post(submitButton = "non-digital").futureValue
      response should have('status(303))
      response.header("Location") should contain (returnUrl)

      `/preferences/paye/individual/:nino/activations/paye`(nino, authHeader).put().futureValue.status should be (409)
    }

  }

  trait UpgradeTestCase extends TestCaseWithFrontEndAuthentication {
    import play.api.Play.current

    val returnUrl = "/test/return/url"
    override def utr: String = "1097172564"

    override val gatewayId: String = "UpgradePreferencesISpec"
    val authHeader = bearerTokenHeader()

    val nino = "CE123457D"

    val `/upgrade-email-reminders` = new {

      val url = WS.url(resource("/account/account-details/sa/upgrade-email-reminders")).withQueryString("returnUrl" -> ApplicationCrypto.QueryParameterCrypto.encrypt(PlainText(returnUrl)).value)

      def post(submitButton: String) = {
        url.withHeaders(cookie,"Csrf-Token"->"nocheck").withFollowRedirects(false).post(
          Map("submitButton" -> Seq(submitButton))
        )
      }

      def get() = url.withHeaders(cookie).get()
    }

    def createOptedInVerifiedPreferenceWithNino() : WSResponse = {

      await(`/preferences-admin/sa/individual`.delete(utr))
      `/portal/preferences/sa/individual`.postPendingEmail(utr, uniqueEmail) should (have(status(200)) or have(status(201)))
      `/preferences-admin/sa/individual`.verifyEmailFor(utr)
      await(`/preferences-admin/sa/process-nino-determination`.post())
    }

    val controller = new UpgradeRemindersController {
      override def preferencesConnector: PreferencesConnector = mock[PreferencesConnector]

      override def authConnector: AuthConnector = mock[AuthConnector]

      override def auditConnector: AuditConnector = mock[AuditConnector]
    }
  }
}