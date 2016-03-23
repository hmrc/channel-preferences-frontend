import controllers.internal.routes
import model.Encrypted
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.JsString
import play.api.libs.ws.{WS, WSResponse}
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.domain.{Nino, SaUtr}

import scala.concurrent.Future
import scala.util.Random

class UpgradePreferencesISpec extends PreferencesFrontEndServer with EmailSupport with MockitoSugar {

  "Upgrading preferences for paye" should {

    "set upgraded to paperless and allow subsequent activation" in new UpgradeTestCase {
      createOptedInVerifiedPreferenceWithNino()

      `/paperless/activate/:form-type/:tax-identifier`("notice-of-coding", nino)(SaUtr(utr)).put().futureValue.status should be (412)

      val response = `/paperless/upgrade`.post(optIn = true, acceptedTandC = Some(true)).futureValue
      response should have('status(303))
      response.header("Location").get should be (routes.UpgradeRemindersController.displayUpgradeConfirmed(Encrypted(returnUrl)).toString())

      `/paperless/activate/:form-type/:tax-identifier`("notice-of-coding", nino)(SaUtr(utr)).put().futureValue.status should be (200)
    }

    "return bad request if T&Cs not accepted"  in new UpgradeTestCase  {
      createOptedInVerifiedPreferenceWithNino()

      `/paperless/activate/:form-type/:tax-identifier`("notice-of-coding", nino)(SaUtr(utr)).put().futureValue.status should be (412)

      val response = `/paperless/upgrade`.post(optIn = true, acceptedTandC = Some(false)).futureValue
      response should have('status(400))
    }

    "set not upgraded to paperless and don't allow subsequent activation"  in new UpgradeTestCase  {
      createOptedInVerifiedPreferenceWithNino()

      `/paperless/activate/:form-type/:tax-identifier`("notice-of-coding", nino)(SaUtr(utr)).put().futureValue.status should be (412)

      val response = `/paperless/upgrade`.post(optIn = false, acceptedTandC = None).futureValue
      response should have('status(303))
      response.header("Location") should contain (returnUrl)

      `/paperless/activate/:form-type/:tax-identifier`("notice-of-coding", nino)(SaUtr(utr)).put().futureValue.status should be (409)
    }
  }

  "An existing user" should {
    "not be redirected to go paperless if they have already opted-out of generic terms" in new NewUserTestCase {
      `/preferences/taxIdentifier/terms-and-conditions`(ggAuthHeader).postOptOut(utr).futureValue.status should be (201)

      `/paperless/activate/:form-type/:tax-identifier`("sa-all", SaUtr(utr))().put().futureValue.status should be (409)
    }
  }

  "Upgrading preferences for legacy SA user" should {
    val pendingEmail = "some@email.com"

    "set upgraded to paperless and allow subsequent activation when legacy user is verified" in new UpgradeTestCase {
      val entityId = `/entity-resolver-admin/sa/:utr`(utr, true)

      `/preferences-admin/sa/individual`.postLegacyOptIn(entityId, pendingEmail).futureValue.status should be (200)
      `/preferences-admin/sa/individual`.verifyEmailFor(entityId).futureValue.status should be (204)

      val activateResponse = `/paperless/activate/:form-type/:tax-identifier`("sa-all", SaUtr(utr))().put().futureValue
      activateResponse.status should be (412)

      (activateResponse.json \ "redirectUserTo").as[JsString].value should include ("/paperless/upgrade")

      val upgradeResponse = `/paperless/upgrade`.get().futureValue
      upgradeResponse.status should be (200)
      upgradeResponse.body should include ("Go paperless with HMRC")

      val response = `/paperless/upgrade`.post(optIn = true, acceptedTandC = Some(true)).futureValue
      response should have('status(303))
      response.header("Location").get should be (routes.UpgradeRemindersController.displayUpgradeConfirmed(Encrypted(returnUrl)).toString())

      `/paperless/activate/:form-type/:tax-identifier`("sa-all", SaUtr(utr))().put().futureValue.status should be (200)
    }

    "set upgraded to paperless and allow subsequent activation when legacy user is pending verification" in new UpgradeTestCase {
      val entityId = `/entity-resolver-admin/sa/:utr`(utr, true)
      `/preferences-admin/sa/individual`.postLegacyOptIn(entityId, pendingEmail).futureValue.status should be (200)

      val activateResponse = `/paperless/activate/:form-type/:tax-identifier`("sa-all", SaUtr(utr))().put().futureValue
      activateResponse.status should be (412)

      (activateResponse.json \ "redirectUserTo").as[JsString].value should include ("/paperless/upgrade")

      val upgradeResponse = `/paperless/upgrade`.get().futureValue
      upgradeResponse.status should be (200)
      upgradeResponse.body should include ("Go paperless with HMRC")

      val response = `/paperless/upgrade`.post(optIn = true, acceptedTandC = Some(true)).futureValue
      response should have('status(303))
      response.header("Location").get should be (routes.UpgradeRemindersController.displayUpgradeConfirmed(Encrypted(returnUrl)).toString())

      `/paperless/activate/:form-type/:tax-identifier`("sa-all", SaUtr(utr))().put().futureValue.status should be (200)
    }

    "show go paperless and allow subsequent activation when legacy user is opted out" in new NewUserTestCase  {
      val entityId = `/entity-resolver-admin/sa/:utr`(utr, true)
      `/preferences-admin/sa/individual`.postLegacyOptOut(entityId).futureValue.status should be (200)

      val activateResponse = `/paperless/activate/:form-type/:tax-identifier`("sa-all", SaUtr(utr))().put().futureValue
      activateResponse.status should be (412)

      (activateResponse.json \ "redirectUserTo").as[JsString].value should include ("/paperless/choose")

      val upgradeResponse = `/paperless/choose/:cohort`().get()
      upgradeResponse.status should be (200)
      upgradeResponse.body should include ("Go paperless with HMRC")

      val postGoPaperless = post(optIn = true, Some(email), true).futureValue
      postGoPaperless should have('status(200))
      postGoPaperless.body should include ("Nearly done...")

      `/paperless/activate/:form-type/:tax-identifier`("sa-all", SaUtr(utr))().put().futureValue.status should be (200)
    }

    "show go paperless and allow subsequent activation when legacy user is de-enrolled" in new NewUserTestCase {
      val a = `/portal/preferences`
      `/preferences/taxIdentifier/terms-and-conditions`(ggAuthHeader).postPendingEmail(utr, pendingEmail).futureValue.status should be (201)
      a.postDeEnrolling(utr).futureValue.status should be (200)

      val activateResponse = `/paperless/activate/:form-type/:tax-identifier`("sa-all", SaUtr(utr))().put().futureValue
      activateResponse.status should be (412)

      (activateResponse.json \ "redirectUserTo").as[JsString].value should include ("/paperless/choose")

      val goPaperlessResponse = `/paperless/choose/:cohort`().get()
      goPaperlessResponse.status should be (200)
      goPaperlessResponse.body should include ("Go paperless with HMRC")

      val postGoPaperless = post(optIn = true, Some(email), true).futureValue
      postGoPaperless should have('status(200))
      postGoPaperless.body should include ("Nearly done...")

      `/paperless/activate/:form-type/:tax-identifier`("sa-all", SaUtr(utr))().put().futureValue.status should be (200)
    }
   }

  "New user preferences" should {
    "set generic terms and conditions as true including email address" in new NewUserTestCase {
      val response = post(true, Some(email), true).futureValue
      response.status should be (200)
      val preferencesResponse =  `/portal/preferences`.get(utr)
      preferencesResponse should have(status(200))
      val body = preferencesResponse.futureValue.body
      body should include(""""digital":true""")
      body should include(s""""email":"$email""")
    }

    "set generic terms and conditions as false without email address" in new NewUserTestCase {
      val response = post(false, None, false).futureValue
      response.status should be (303)

      val preferencesResponse =  `/portal/preferences`.get(utr)
      preferencesResponse should have(status(200))
      val body = preferencesResponse.futureValue.body
      body should include(""""digital":false""")
      body should not include s""""email":"$email"""
    }
  }

  "Upgrade preferences page" should {
    "receive a digital true response from get preference call for legacy opted in user" in new NewUserTestCase {
      val pendingEmail = "some@email.com"
      val entityId = `/entity-resolver-admin/sa/:utr`(utr, true)
      `/preferences-admin/sa/individual`.postLegacyOptIn(entityId, pendingEmail).futureValue.status should be (200)
      val preferencesResponse = `/preferences/taxIdentifier`(authHeader).getPreference(utr).futureValue
      preferencesResponse should have ('status(200))
      preferencesResponse.body should include(""""digital":true""")
      preferencesResponse.body should include(s"""email":"$pendingEmail"""")
    }
  }

  trait NewUserTestCase extends TestCaseWithFrontEndAuthentication {
    import play.api.Play.current

    override val utr : String = Math.abs(Random.nextInt()).toString.substring(0, 6)

    val email = "a@b.com"

    implicit val authHeader = createGGAuthorisationHeader(SaUtr(utr))
    override lazy val cookie = cookieForUtr(SaUtr(utr)).futureValue

    val url = WS.url(resource("/paperless/choose"))
        .withQueryString("returnUrl" -> ApplicationCrypto.QueryParameterCrypto.encrypt(PlainText(returnUrl)).value)
        .withQueryString("returnLinkText" -> ApplicationCrypto.QueryParameterCrypto.encrypt(PlainText("Go-somewhere")).value)

    def `/paperless/choose/:cohort`(cohort: String = "8") = new {
      val url = WS.url(resource(s"/paperless/choose/$cohort"))
        .withQueryString("returnUrl" -> ApplicationCrypto.QueryParameterCrypto.encrypt(PlainText(returnUrl)).value)
        .withQueryString("returnLinkText" -> ApplicationCrypto.QueryParameterCrypto.encrypt(PlainText("Go-somewhere")).value)
        .withHeaders(cookie,"Csrf-Token"->"nocheck", authHeader).withFollowRedirects(false)

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

    val nino = GenerateRandom.nino()

    val authHeader =  createGGAuthorisationHeader(SaUtr(utr), nino)
    override lazy val cookie = cookieForUtrAndNino(SaUtr(utr), nino).futureValue

    val `/paperless/upgrade` = new {

      val url = WS.url(resource("/paperless/upgrade")).withQueryString("returnUrl" -> ApplicationCrypto.QueryParameterCrypto.encrypt(PlainText(returnUrl)).value)

      def post(optIn: Boolean, acceptedTandC: Option[Boolean]) = {
        url.withHeaders(cookie,"Csrf-Token"->"nocheck").withFollowRedirects(false).post(
          Seq(Some("opt-in" -> Seq(optIn.toString)), acceptedTandC.map(a => "accept-tc" -> Seq(a.toString))).flatten.toMap
        )
      }

      def get() = url.withHeaders(cookie).get()
    }

    def createOptedInVerifiedPreferenceWithNino() : WSResponse = {
      val entityId = `/entity-resolver-admin/paye/:nino`(nino.value, true)
      val legacySupport = `/preferences-admin/sa/individual`
      await(legacySupport.postLegacyOptIn(entityId, uniqueEmail))
      await(legacySupport.verifyEmailFor(entityId))

    }
  }

}