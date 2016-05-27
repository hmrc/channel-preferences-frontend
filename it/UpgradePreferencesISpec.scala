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

  "An existing user" should {
    "not be redirected to go paperless if they have already have a preference" in new NewUserTestCase {
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postOptOut.futureValue.status should be (201)

      `/paperless/activate/:form-type/:tax-identifier`("sa-all", SaUtr(utr))().put().futureValue.status should be (200)
    }
  }

  "New user preferences" should {
    "set generic terms and conditions as true including email address for utr only" in new NewUserTestCase {
      val response = post(true, Some(email), true).futureValue
      response.status should be (200)
      val preferencesResponse =  `/portal/preferences`.getForUtr(utr)
      preferencesResponse should have(status(200))
      val body = preferencesResponse.futureValue.body
      body should include(""""digital":true""")
      body should include(s""""email":"$email""")
    }

    "set generic terms and conditions as true including email address for nino only" in new NewUserTestCase {
      val response = post(true, Some(email), true, cookie = cookieWithNino, authHeader = ggAuthHeaderWithNino).futureValue
      response.status should be (200)
      val preferencesResponse =  `/portal/preferences`.getForNino(nino.value)
      preferencesResponse should have(status(200))
      val body = preferencesResponse.futureValue.body
      body should include(""""digital":true""")
      body should include(s""""email":"$email""")
    }

    "set generic terms and conditions as false without email address" in new NewUserTestCase {
      val response = post(false, None, false).futureValue
      response.status should be (303)

      val preferencesResponse =  `/portal/preferences`.getForUtr(utr)
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
      val preferencesResponse = `/preferences`(ggAuthHeaderWithUtrAndNino).getPreference.futureValue
      preferencesResponse should have ('status(200))
      preferencesResponse.body should include(""""digital":true""")
      preferencesResponse.body should include(s"""email":"$pendingEmail"""")
    }
  }

  trait NewUserTestCase extends TestCaseWithFrontEndAuthentication {
    import play.api.Play.current

    override val utr : String = Math.abs(Random.nextInt()).toString.substring(0, 6)

    val email = "a@b.com"

    val url = WS.url(resource("/paperless/choose"))
        .withQueryString("returnUrl" -> ApplicationCrypto.QueryParameterCrypto.encrypt(PlainText(returnUrl)).value)
        .withQueryString("returnLinkText" -> ApplicationCrypto.QueryParameterCrypto.encrypt(PlainText("Go-somewhere")).value)

    def `/paperless/choose/:cohort`(cohort: String = "8") = new {
      val url = WS.url(resource(s"/paperless/choose/$cohort"))
        .withQueryString("returnUrl" -> ApplicationCrypto.QueryParameterCrypto.encrypt(PlainText(returnUrl)).value)
        .withQueryString("returnLinkText" -> ApplicationCrypto.QueryParameterCrypto.encrypt(PlainText("Go-somewhere")).value)
        .withHeaders(cookieWithUtr,"Csrf-Token"->"nocheck", ggAuthHeaderWithUtrAndNino).withFollowRedirects(false)

      def get() = {
        url.get().futureValue
      }
    }

    def post(optIn:Boolean, email: Option[String], acceptTAndC: Boolean, cookie : (String, String) = cookieWithUtrAndNino, authHeader : (String, String) = ggAuthHeaderWithUtrAndNino ): Future[WSResponse] = {
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

    val `/paperless/upgrade` = new {

      val url = WS.url(resource("/paperless/upgrade")).withQueryString("returnUrl" -> ApplicationCrypto.QueryParameterCrypto.encrypt(PlainText(returnUrl)).value)

      def post(optIn: Boolean, acceptedTandC: Option[Boolean], cookie : (String, String) = cookieWithUtr) = {
        url.withHeaders(cookie,"Csrf-Token"->"nocheck").withFollowRedirects(false).post(
          Seq(Some("opt-in" -> Seq(optIn.toString)), acceptedTandC.map(a => "accept-tc" -> Seq(a.toString))).flatten.toMap
        )
      }

      def get(cookie : (String, String) = cookieWithUtr) = url.withHeaders(cookie).get()
    }

    def createOptedInVerifiedPreferenceWithNino() : WSResponse = {
      val entityId = `/entity-resolver-admin/paye/:nino`(nino.value, true)
      val legacySupport = `/preferences-admin/sa/individual`
      await(legacySupport.postLegacyOptIn(entityId, uniqueEmail))
      await(legacySupport.verifyEmailFor(entityId))

    }
  }

}