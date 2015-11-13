import controllers.internal.routes
import model.Encrypted
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.JsString
import play.api.libs.ws.{WS, WSResponse}
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}

import scala.concurrent.Future
import scala.util.Random

class UpgradePreferencesISpec extends PreferencesFrontEndServer with EmailSupport with MockitoSugar {

  "Upgrading preferences for paye" should {

    "set upgraded to paperless and allow subsequent activation"  in new UpgradeTestCase  {
      createOptedInVerifiedPreferenceWithNino()

      `/preferences/paye/individual/:nino/activations/paye`(nino,authHeader).put().futureValue.status should be (412)

      val response = `/upgrade-email-reminders`.post(optIn = true, acceptedTandC = Some(true)).futureValue
      response should have('status(303))
      response.header("Location").get should be (routes.UpgradeRemindersController.displayUpgradeConfirmed(Encrypted(returnUrl)).toString())

      `/preferences/paye/individual/:nino/activations/paye`(nino, authHeader).put().futureValue.status should be (200)
    }

    "return bad request if T&Cs not accepted"  in new UpgradeTestCase  {
      createOptedInVerifiedPreferenceWithNino()

      `/preferences/paye/individual/:nino/activations/paye`(nino,authHeader).put().futureValue.status should be (412)

      val response = `/upgrade-email-reminders`.post(optIn = true, acceptedTandC = Some(false)).futureValue
      response should have('status(400))
    }

    "set not upgraded to paperless and don't allow subsequent activation"  in new UpgradeTestCase  {
      createOptedInVerifiedPreferenceWithNino()

      `/preferences/paye/individual/:nino/activations/paye`(nino,authHeader).put().futureValue.status should be (412)

      val response = `/upgrade-email-reminders`.post(optIn = false, acceptedTandC = None).futureValue
      response should have('status(303))
      response.header("Location") should contain (returnUrl)

      `/preferences/paye/individual/:nino/activations/paye`(nino, authHeader).put().futureValue.status should be (409)
    }
  }

  "An existing user" should {
    "not be redirected to go paperless if they have already opted-out of generic terms" in new NewUserTestCase {
      await(`/preferences-admin/sa/individual`.delete(utr))

      `/portal/preferences/sa/individual`.postOptOut(utr).futureValue.status should be (201)

      val activateResponse = `/preferences/sa/individual/:utr/activations`(utr).put().futureValue
      activateResponse.status should be (409)
    }
  }

  "Upgrading preferences for legacy SA user" should {
    val pendingEmail = "some@email.com"

    "set upgraded to paperless and allow subsequent activation when legacy user is verified" in new UpgradeTestCase {
      await(`/preferences-admin/sa/individual`.delete(utr))

      `/portal/preferences/sa/individual`.postPendingEmail(utr, pendingEmail).futureValue.status should be (201)
      `/preferences-admin/sa/individual`.verifyEmailFor(utr).futureValue.status should be (204)

      val activateResponse = `/preferences/sa/individual/:utr/activations`(utr).put().futureValue
      activateResponse.status should be (412)

      (activateResponse.json \ "redirectUserTo").as[JsString].value should include ("/account/account-details/sa/upgrade-email-reminders")

      val upgradeResponse = `/upgrade-email-reminders`.get().futureValue
      upgradeResponse.status should be (200)
      upgradeResponse.body should include ("Go paperless with HMRC")

      val response = `/upgrade-email-reminders`.post(optIn = true, acceptedTandC = Some(true)).futureValue
      response should have('status(303))
      response.header("Location").get should be (routes.UpgradeRemindersController.displayUpgradeConfirmed(Encrypted(returnUrl)).toString())

      `/preferences/sa/individual/:utr/activations`(utr).put().futureValue.status should be (200)
    }

    "set upgraded to paperless and allow subsequent activation when legacy user is pending verification" in new UpgradeTestCase {
      await(`/preferences-admin/sa/individual`.delete(utr))

      `/portal/preferences/sa/individual`.postPendingEmail(utr, pendingEmail).futureValue.status should be (201)

      val activateResponse = `/preferences/sa/individual/:utr/activations`(utr).put().futureValue
      activateResponse.status should be (412)

      (activateResponse.json \ "redirectUserTo").as[JsString].value should include ("/account/account-details/sa/upgrade-email-reminders")

      val upgradeResponse = `/upgrade-email-reminders`.get().futureValue
      upgradeResponse.status should be (200)
      upgradeResponse.body should include ("Go paperless with HMRC")

      val response = `/upgrade-email-reminders`.post(optIn = true, acceptedTandC = Some(true)).futureValue
      response should have('status(303))
      response.header("Location").get should be (routes.UpgradeRemindersController.displayUpgradeConfirmed(Encrypted(returnUrl)).toString())

      `/preferences/sa/individual/:utr/activations`(utr).put().futureValue.status should be (200)
    }

    "show go paperless and allow subsequent activation when legacy user is opted out" in new NewUserTestCase  {
      await(`/preferences-admin/sa/individual`.delete(utr))

      `/portal/preferences/sa/individual`.postLegacyOptOut(utr).futureValue.status should be (200)

      val activateResponse = `/preferences/sa/individual/:utr/activations`(utr).put().futureValue
      activateResponse.status should be (412)

      (activateResponse.json \ "redirectUserTo").as[JsString].value should include ("/account/account-details/sa/login-opt-in-email-reminders")

      val upgradeResponse = `/account/account-details/sa/login-opt-in-email-reminders/:cohort`().get()
      upgradeResponse.status should be (200)
      upgradeResponse.body should include ("Go paperless with HMRC")

      val postGoPaperless = post(optIn = true, Some(email), true).futureValue
      postGoPaperless should have('status(200))
      postGoPaperless.body should include ("Nearly done...")

      `/preferences/sa/individual/:utr/activations`(utr).put().futureValue.status should be (200)
    }

    "show go paperless and allow subsequent activation when legacy user is de-enrolled" in new NewUserTestCase {
      await(`/preferences-admin/sa/individual`.delete(utr))

      val a = `/portal/preferences/sa/individual`
      a.postPendingEmail(utr, pendingEmail).futureValue.status should be (201)
      a.postDeEnrolling(utr).futureValue.status should be (200)

      val activateResponse = `/preferences/sa/individual/:utr/activations`(utr).put().futureValue
      activateResponse.status should be (412)

      (activateResponse.json \ "redirectUserTo").as[JsString].value should include ("/account/account-details/sa/login-opt-in-email-reminders")

      val goPaperlessResponse = `/account/account-details/sa/login-opt-in-email-reminders/:cohort`().get()
      goPaperlessResponse.status should be (200)
      goPaperlessResponse.body should include ("Go paperless with HMRC")

      val postGoPaperless = post(optIn = true, Some(email), true).futureValue
      postGoPaperless should have('status(200))
      postGoPaperless.body should include ("Nearly done...")

      `/preferences/sa/individual/:utr/activations`(utr).put().futureValue.status should be (200)
    }
   }

  "New user preferences" should {
    "set generic terms and conditions as true including email address" in new NewUserTestCase {
      val response = post(true, Some(email), true).futureValue
      response.status should be (200)

      val preferencesResponse =  `/portal/preferences/sa/individual`.get(utr)
      preferencesResponse should have(status(200))
      val body = preferencesResponse.futureValue.body
      body should include(""""digital":true""")
      body should include(s""""email":"$email""")
    }

    "set generic terms and conditions as false without email address" in new NewUserTestCase {
      val response = post(false, None, false).futureValue
      response.status should be (303)

      val preferencesResponse =  `/portal/preferences/sa/individual`.get(utr)
      preferencesResponse should have(status(200))
      val body = preferencesResponse.futureValue.body
      body should include(""""digital":false""")
      body should not include(s""""email":"$email""")
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

    def `/account/account-details/sa/login-opt-in-email-reminders/:cohort`(cohort: String = "8") = new {
      val url = WS.url(resource(s"/account/account-details/sa/login-opt-in-email-reminders/$cohort")).withHeaders(cookie,"Csrf-Token"->"nocheck", authHeader).withFollowRedirects(false)

      def get() = {
        url.get().futureValue
      }
    }

    def post(optIn:Boolean, email: Option[String], acceptTAndC: Boolean): Future[WSResponse] = {

      val params = Map("opt-in" -> Seq(optIn.toString), "accept-tc" -> Seq(acceptTAndC.toString))
      val paramsWithEmail = email match {
        case None => params
        case Some(emailValue) => params + ("email.main" -> Seq(emailValue), "email.confirm" -> Seq(emailValue), "emailVerified" -> Seq(true.toString))
      }

      url.withHeaders(cookie,"Csrf-Token"->"nocheck", authHeader).withFollowRedirects(optIn).post(paramsWithEmail)
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

      def post(optIn: Boolean, acceptedTandC: Option[Boolean]) = {
        url.withHeaders(cookie,"Csrf-Token"->"nocheck").withFollowRedirects(false).post(
          Seq(Some("opt-in" -> Seq(optIn.toString)), acceptedTandC.map(a => "accept-tc" -> Seq(a.toString))).flatten.toMap
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