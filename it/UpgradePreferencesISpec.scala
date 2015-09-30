import controllers.sa.prefs.{EmailPreference, Encrypted}
import controllers.sa.prefs.internal.routes
import org.scalatest.mock.MockitoSugar
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.libs.ws.{WS, WSResponse}
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}

import scala.concurrent.Future
import scala.text
import scala.util.Random

class UpgradePreferencesISpec extends PreferencesFrontEndServer with EmailSupport with MockitoSugar {

  "Upgrading preferences should" should {

    "set upgraded to paperless and allow subsequent activation"  in new UpgradeTestCase  {
      createOptedInVerifiedPreferenceWithNino()

      `/preferences/paye/individual/:nino/activations/paye`(nino,authHeader).put().futureValue.status should be (412)

      val response = `/upgrade-email-reminders`.post(optIn = true).futureValue
      response should have('status(303))
      response.header("Location").get should be (routes.UpgradeRemindersController.thankYou(Encrypted(returnUrl)).toString())

      `/preferences/paye/individual/:nino/activations/paye`(nino, authHeader).put().futureValue.status should be (200)
    }

    "set not upgraded to paperless and don't allow subsequent activation"  in new UpgradeTestCase  {
      createOptedInVerifiedPreferenceWithNino()

      `/preferences/paye/individual/:nino/activations/paye`(nino,authHeader).put().futureValue.status should be (412)

      val response = `/upgrade-email-reminders`.post(optIn = false).futureValue
      response should have('status(303))
      response.header("Location") should contain (returnUrl)

      `/preferences/paye/individual/:nino/activations/paye`(nino, authHeader).put().futureValue.status should be (409)
    }
  }

  "New user preferences" should {
    "set generic terms and conditions as true including email address" in new NewUserTestCase {
      val response = post(true, Some(email), true).futureValue
      response.status should be (200)
    }

    "set generic terms and conditions as false without email address" in new NewUserTestCase {
      val response = post(false, None, false).futureValue
      response.status should be (200)
    }
  }


  trait NewUserTestCase extends TestCaseWithFrontEndAuthentication {
    import play.api.Play.current

    override val gatewayId: String = "UpgradePreferencesISpec"
    override val utr : String = Math.abs(Random.nextInt()).toString.substring(0, 6)

    val email = "a@b.com"
    val returnUrl = "/test/return/url"
    val authHeader = bearerTokenHeader()
    val url = WS.url(resource("/account/account-details/sa/login-opt-in-email-reminders"))
        .withQueryString("returnUrl" -> ApplicationCrypto.QueryParameterCrypto.encrypt(PlainText(returnUrl)).value)

    def post(optIn:Boolean, email: Option[String], acceptTAndC: Boolean): Future[WSResponse] = {

      val params = Map("opt-in" -> Seq(optIn.toString), "accept-tc" -> Seq(acceptTAndC.toString))
      val paramsWithEmail = email match {
        case None => params
        case Some(emailValue) => params + ("email.main" -> Seq(emailValue), "email.confirm" -> Seq(emailValue), "emailVerified" -> Seq(true.toString))
      }

      url.withHeaders(cookie,"Csrf-Token"->"nocheck", authHeader).post(paramsWithEmail)
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

      def post(optIn: Boolean) = {
        url.withHeaders(cookie,"Csrf-Token"->"nocheck").withFollowRedirects(false).post(
          Map("opt-in" -> Seq(optIn.toString))
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
  }
}