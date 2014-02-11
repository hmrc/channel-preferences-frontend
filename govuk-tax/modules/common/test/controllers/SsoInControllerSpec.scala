package controllers

import common.service.SsoWhiteListService
import org.scalatest.mock.MockitoSugar
import play.api.test.{FakeRequest, WithApplication}
import uk.gov.hmrc.common.microservice.governmentgateway.{GovernmentGatewayConnector, GovernmentGatewayLoginResponse, SsoLoginRequest}
import org.mockito.Mockito._
import java.net.{URI, URLEncoder}
import uk.gov.hmrc.common.BaseSpec
import controllers.common._
import play.api.test.Helpers._
import uk.gov.hmrc.common.microservice.UnauthorizedException
import play.api.libs.ws.Response
import play.api.test.FakeApplication
import play.api.mvc.SimpleResult
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.mockito.{ArgumentCaptor, Matchers}
import controllers.common.actions.HeaderCarrier
import scala.concurrent.Future
import uk.gov.hmrc.common.microservice.audit.{AuditEvent, AuditConnector}
import uk.gov.hmrc.common.crypto.ApplicationCrypto.SsoPayloadCrypto
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import controllers.domain.AuthorityUtils._

class SsoInControllerSpec extends BaseSpec with MockitoSugar with ScalaFutures with IntegrationPatience {

  val loginPage = routes.LoginController.businessTaxLogin().url

  "The Single Sign-on input page" should {
    "create a new session when the token is valid, the time not expired and no session exists - POST" in new WithSsoControllerInFakeApplication {
      when(mockGovernmentGatewayService.ssoLogin(Matchers.eq(SsoLoginRequest(john.encodedToken, john.loginTimestamp)))(Matchers.any[HeaderCarrier])).thenReturn(GovernmentGatewayLoginResponse(john.userId, john.credId, john.name, john.affinityGroup, john.encodedToken))
      when(mockSsoWhiteListService.check(URI.create(redirectUrl).toURL)).thenReturn(true)
      when(mockAuthConnector.exchangeCredIdForBearerToken(Matchers.any[String])(Matchers.any[HeaderCarrier])).thenReturn(Future.successful(AuthExchangeResponse(AuthToken("someAuthToken"), saAuthority(john.userId, "utr"))))

      val encryptedPayload = SsoPayloadCrypto.encrypt( s"""{"gw": "${john.encodedToken}", "time": ${john.loginTimestamp}, "dest": "$redirectUrl"}""")
      val response = controller.postIn(FakeRequest("POST", s"www.governmentgateway.com").withFormUrlEncodedBody("payload" -> encryptedPayload))
      whenReady(response) {
        result =>
          val SimpleResult(header, _, _) = result
          header.status shouldBe 303
          header.headers("Location") shouldBe redirectUrl
          header.headers("Set-Cookie") should include(sessionEntry(SessionKeys.userId, s"/auth/oid/${john.userId}"))
          header.headers("Set-Cookie") should include(sessionEntry(SessionKeys.name, john.name))
          header.headers("Set-Cookie") should include(sessionEntry(SessionKeys.affinityGroup, john.affinityGroup))
          header.headers("Set-Cookie") should include(sessionEntry(SessionKeys.token, john.encodedToken))
          header.headers("Set-Cookie") should include(sessionEntry(SessionKeys.authToken, "someAuthToken"))
      }

      expectAnSsoLoginSuccessfulAuditEventFor(john)
    }

    "create a new session when the token is valid, the time not expired and no session exists - GET" in new WithSsoControllerInFakeApplication {
      when(mockGovernmentGatewayService.ssoLogin(Matchers.eq(SsoLoginRequest(john.encodedToken, john.loginTimestamp)))(Matchers.any[HeaderCarrier])).thenReturn(GovernmentGatewayLoginResponse(john.userId, john.credId, john.name, john.affinityGroup, john.encodedToken))
      when(mockSsoWhiteListService.check(URI.create(redirectUrl).toURL)).thenReturn(true)
      when(mockAuthConnector.exchangeCredIdForBearerToken(Matchers.any[String])(Matchers.any[HeaderCarrier])).thenReturn(Future.successful(AuthExchangeResponse(AuthToken("someAuthToken"), saAuthority(john.userId, "utr"))))

      val encryptedPayload = URLEncoder.encode(SsoPayloadCrypto.encrypt( s"""{"gw": "${john.encodedToken}", "time": ${john.loginTimestamp}, "dest": "$redirectUrl"}"""), "UTF-8")
      val response = controller.getIn(encryptedPayload)(FakeRequest())
      whenReady(response) {
        result =>
          val SimpleResult(header, _, _) = result
          header.status shouldBe 303
          header.headers("Location") shouldBe redirectUrl
          header.headers("Set-Cookie") should include(sessionEntry(SessionKeys.userId, s"/auth/oid/${john.userId}"))
          header.headers("Set-Cookie") should include(sessionEntry(SessionKeys.name, john.name))
          header.headers("Set-Cookie") should include(sessionEntry(SessionKeys.affinityGroup, john.affinityGroup))
          header.headers("Set-Cookie") should include(sessionEntry(SessionKeys.token, john.encodedToken))
          header.headers("Set-Cookie") should include(sessionEntry(SessionKeys.authToken, "someAuthToken"))
      }

      expectAnSsoLoginSuccessfulAuditEventFor(john)
    }

    "Is able to handle encoded payload" in new WithSsoControllerInFakeApplication {
      val payload = """GL%2FIIjC9NeLGrJh6Aqw1SQ1eFUt%2FMTn%2BYmuFtYduXSC9TPeNvZ8%2BWfEFIiL%2F0Y%2BOD6velzt%2F9pxeZWS7ni8vQs3Dr6Gr7anR5pbmwumQ6K75FlzZO7Fpl2rI42DsZPNTVAVNOb0XyQOiRgKMnDJNEBMustrwIcIMDtrnXKnXC6L91Lc07al6BOpE%2FppSHtgBzsbD%2FE%2FdEnSgTnZ9HT4TAE94TBX7Sz3N4oCZDRZH7SvX%2B9VAzHOBoNOwlp3Uu%2BKXms2oo3In%2F%2F20cNzAqPCe7vaFnfZ%2Fo7GEu%2BdCn2C4rWGWGmrSJHwaRJMnbZ%2Bd0fw1%2F54ew9SMgu0avlxeGohmKlZAEw%2FqIFWwVRXgfFK13sJwZ%2BT3%2FK1yN5vj%2FSbetib5ThwRJHYMHsEekUt0KKu1GcItyAmFSrYr0KkmGykkx420sSEHQNHfh9IRsrwBOQjjcE32UJs%2FPcpTrCHUFKBUJWmYA3rbRaQTkHRrMY1YOM2c0FFognBoj7jNP2jCgQcIvrlommoQ%2FC21%2FujKRpTKu5zQUWiCcGiPuM0%2FaMKBBwhtOfA5u78IcsTpn5cJvyoc2%2BxLElY9kdmCzYm3Dgt2M5Dyt7R0O3xX4YBwtEAmGf%2FPHqDxcz0NFbWxM3JYcmzpdpUiQEkijXLwtMOQ1YyoXRle1Idg9Fvic39TgYnITS6cUQsJmAInlNFLzWdq6QSlE2zBFO7o3BlGe57axL2G7hABJM1z4%2FNQ2gFxpbITEKsTLSJKONYMDO26vIR8MQwrkuxJgib9NbPG6qK%2BO1GCw%2BEjnseOWYQ75QFg8cXKVn%2Fn%2FxlR6BViqEE6z84%2FmrCNaFKY4AR%2Fw6zrfXvAZ1bqh3RJgpRU8iSUnD7wv7yc9mPi78B%2BsO%2FHCzLxAzDBWFrywEghZImbHOfaw2hb2q0zB93XwnYkJcblLEanwucUK1ikXQwO4TxdoRx4uFKI%2Fo6qtuXtSiVlnNocHDLCwneW0TFsatvOoblE2KBReKJ7ZliZ0tJyEUHGQtNi70bo3W4zOhLEd5VPBouJLS4bYt7Ujg%3D%3D"""
      when(mockGovernmentGatewayService.ssoLogin(Matchers.eq(SsoLoginRequest(john.encodedToken, john.loginTimestamp)))(Matchers.any[HeaderCarrier])).thenReturn(GovernmentGatewayLoginResponse(john.userId, john.credId, john.name, john.affinityGroup, john.encodedToken))
      when(mockSsoWhiteListService.check(URI.create(redirectUrl).toURL)).thenReturn(true)
      when(mockAuthConnector.exchangeCredIdForBearerToken(Matchers.any[String])(Matchers.any[HeaderCarrier])).thenReturn(Future.successful(AuthExchangeResponse(AuthToken("someAuthToken"), saAuthority(john.userId, "utr"))))

      val response = controller.getIn(payload)(FakeRequest())
      status(response) should not be 500
    }

    "Is able to handle not-encoded payload" in new WithSsoControllerInFakeApplication {
      val payload = """GL/IIjC9NeLGrJh6Aqw1SQ1eFUt/MTn+YmuFtYduXSC9TPeNvZ8+WfEFIiL/0Y+OD6velzt/9pxeZWS7ni8vQs3Dr6Gr7anR5pbmwumQ6K75FlzZO7Fpl2rI42DsZPNTVAVNOb0XyQOiRgKMnDJNEBMustrwIcIMDtrnXKnXC6L91Lc07al6BOpE/ppSHtgBzsbD/E/dEnSgTnZ9HT4TAE94TBX7Sz3N4oCZDRZH7SvX+9VAzHOBoNOwlp3Uu+KXms2oo3In//20cNzAqPCe7vaFnfZ/o7GEu+dCn2C4rWGWGmrSJHwaRJMnbZ+d0fw1/54ew9SMgu0avlxeGohmKlZAEw/qIFWwVRXgfFK13sJwZ+T3/K1yN5vj/Sbetib5ThwRJHYMHsEekUt0KKu1GcItyAmFSrYr0KkmGykkx420sSEHQNHfh9IRsrwBOQjjcE32UJs/PcpTrCHUFKBUJWmYA3rbRaQTkHRrMY1YOM2c0FFognBoj7jNP2jCgQcIvrlommoQ/C21/ujKRpTKu5zQUWiCcGiPuM0/aMKBBwhtOfA5u78IcsTpn5cJvyoc2+xLElY9kdmCzYm3Dgt2M5Dyt7R0O3xX4YBwtEAmGf/PHqDxcz0NFbWxM3JYcmzpdpUiQEkijXLwtMOQ1YyoXRle1Idg9Fvic39TgYnITS6cUQsJmAInlNFLzWdq6QSlE2zBFO7o3BlGe57axL2G7hABJM1z4/NQ2gFxpbITEKsTLSJKONYMDO26vIR8MQwrkuxJgib9NbPG6qK+O1GCw+EjnseOWYQ75QFg8cXKVn/n/xlR6BViqEE6z84/mrCNaFKY4AR/w6zrfXvAZ1bqh3RJgpRU8iSUnD7wv7yc9mPi78B+sO/HCzLxAzDBWFrywEghZImbHOfaw2hb2q0zB93XwnYkJcblLEanwucUK1ikXQwO4TxdoRx4uFKI/o6qtuXtSiVlnNocHDLCwneW0TFsatvOoblE2KBReKJ7ZliZ0tJyEUHGQtNi70bo3W4zOhLEd5VPBouJLS4bYt7Ujg=="""
      when(mockGovernmentGatewayService.ssoLogin(Matchers.eq(SsoLoginRequest(john.encodedToken, john.loginTimestamp)))(Matchers.any[HeaderCarrier])).thenReturn(GovernmentGatewayLoginResponse(john.userId, john.credId, john.name, john.affinityGroup, john.encodedToken))
      when(mockSsoWhiteListService.check(URI.create(redirectUrl).toURL)).thenReturn(true)
      when(mockAuthConnector.exchangeCredIdForBearerToken(Matchers.any[String])(Matchers.any[HeaderCarrier])).thenReturn(Future.successful(AuthExchangeResponse(AuthToken("someAuthToken"), saAuthority(john.userId, "utr"))))

      val response = controller.getIn(payload)(FakeRequest())
      status(response) should not be 500
    }

    "replace any current session with a new one when the token is valid, and the time not expired - POST" in new WithSsoControllerInFakeApplication {

      val encryptedPayload = SsoPayloadCrypto.encrypt( s"""{"gw": "${bob.encodedToken}", "time": ${bob.loginTimestamp}, "dest": "$redirectUrl"}""")
      val request = FakeRequest("POST", s"www.governmentgateway.com")
        .withFormUrlEncodedBody("payload" -> encryptedPayload)
        .withSession(SessionKeys.userId -> john.userId, SessionKeys.name -> john.name, SessionKeys.affinityGroup -> john.affinityGroup, SessionKeys.token -> john.encodedToken)

      when(mockGovernmentGatewayService.ssoLogin(Matchers.eq(SsoLoginRequest(bob.encodedToken, bob.loginTimestamp)))(Matchers.any[HeaderCarrier])).thenReturn(GovernmentGatewayLoginResponse(bob.userId, bob.credId, bob.name, bob.affinityGroup, bob.encodedToken))
      when(mockSsoWhiteListService.check(URI.create(redirectUrl).toURL)).thenReturn(true)
      when(mockAuthConnector.exchangeCredIdForBearerToken(Matchers.any[String])(Matchers.any[HeaderCarrier])).thenReturn(Future.successful(AuthExchangeResponse(AuthToken("someAuthToken"), saAuthority(bob.userId, "utr"))))


      val response = controller.postIn(request)

      whenReady(response) {
        result =>
          val SimpleResult(header, _, _) = result
          header.status shouldBe 303
          header.headers("Location") shouldBe redirectUrl
          header.headers("Set-Cookie") should include(sessionEntry(SessionKeys.userId, s"/auth/oid/${bob.userId}"))
          header.headers("Set-Cookie") should include(sessionEntry(SessionKeys.name, bob.name))
          header.headers("Set-Cookie") should include(sessionEntry(SessionKeys.affinityGroup, bob.affinityGroup))
          header.headers("Set-Cookie") should include(sessionEntry(SessionKeys.token, bob.encodedToken))
          header.headers("Set-Cookie") should include(sessionEntry(SessionKeys.authToken, "someAuthToken"))
      }

      expectAnSsoLoginSuccessfulAuditEventFor(bob)
    }

    "replace any current session with a new one when the token is valid, and the time not expired - GET" in new WithSsoControllerInFakeApplication {

      val encryptedPayload = URLEncoder.encode(SsoPayloadCrypto.encrypt( s"""{"gw": "${bob.encodedToken}", "time": ${bob.loginTimestamp}, "dest": "$redirectUrl"}"""), "UTF-8")
      val request = FakeRequest()
        .withSession(SessionKeys.userId -> john.userId, SessionKeys.name -> john.name, SessionKeys.affinityGroup -> john.affinityGroup, SessionKeys.token -> john.encodedToken)

      when(mockGovernmentGatewayService.ssoLogin(Matchers.eq(SsoLoginRequest(bob.encodedToken, bob.loginTimestamp)))(Matchers.any[HeaderCarrier])).thenReturn(GovernmentGatewayLoginResponse(bob.userId, bob.credId, bob.name, bob.affinityGroup, bob.encodedToken))
      when(mockSsoWhiteListService.check(URI.create(redirectUrl).toURL)).thenReturn(true)
      when(mockAuthConnector.exchangeCredIdForBearerToken(Matchers.any[String])(Matchers.any[HeaderCarrier])).thenReturn(Future.successful(AuthExchangeResponse(AuthToken("someAuthToken"), saAuthority(bob.userId, "utr"))))


      val response = controller.getIn(encryptedPayload)(request)

      whenReady(response) {
        result =>
          val SimpleResult(header, _, _) = result
          header.status shouldBe 303
          header.headers("Location") shouldBe redirectUrl
          header.headers("Set-Cookie") should include(sessionEntry(SessionKeys.userId, s"/auth/oid/${bob.userId}"))
          header.headers("Set-Cookie") should include(sessionEntry(SessionKeys.name, bob.name))
          header.headers("Set-Cookie") should include(sessionEntry(SessionKeys.affinityGroup, bob.affinityGroup))
          header.headers("Set-Cookie") should include(sessionEntry(SessionKeys.token, bob.encodedToken))
          header.headers("Set-Cookie") should include(sessionEntry(SessionKeys.authToken, "someAuthToken"))
      }

      expectAnSsoLoginSuccessfulAuditEventFor(bob)
    }

    "invalidate the session if a session already exists but the login is incorrect" in new WithSsoControllerInFakeApplication {
      val johnInvalidCredentials = john.copy(encodedToken = "invalidToken")
      when(mockGovernmentGatewayService.ssoLogin(Matchers.eq(SsoLoginRequest(johnInvalidCredentials.encodedToken, johnInvalidCredentials.loginTimestamp)))(Matchers.any[HeaderCarrier])).thenReturn(Future.failed(new IllegalStateException("error")))
      when(mockSsoWhiteListService.check(URI.create(redirectUrl).toURL)).thenReturn(true)
      val encryptedPayload = SsoPayloadCrypto.encrypt( s"""{"gw": "${johnInvalidCredentials.encodedToken}", "time": ${johnInvalidCredentials.loginTimestamp}, "dest": "$redirectUrl"}""")

      val response = controller.postIn(FakeRequest("POST", s"www.governmentgateway.com")
        .withFormUrlEncodedBody("payload" -> encryptedPayload)
        .withSession(SessionKeys.userId -> johnInvalidCredentials.userId, SessionKeys.name -> johnInvalidCredentials.name, SessionKeys.token -> johnInvalidCredentials.encodedToken))

      whenReady(response) {
        result =>
          val SimpleResult(header, _, _) = result
          header.status shouldBe 303
          header.headers("Location") shouldBe loginPage
          header.headers("Set-Cookie") should not include SessionKeys.userId
          header.headers("Set-Cookie") should not include SessionKeys.name
          header.headers("Set-Cookie") should not include SessionKeys.token
          header.headers("Set-Cookie") should not include SessionKeys.authToken
      }

      expectAnSsoLoginFailedAuditEventFor(johnInvalidCredentials, transactionFailureReason = "Invalid Token")
    }

    "invalidate the session if a session already exists but the login throws an Unauthorised Exception" in new WithSsoControllerInFakeApplication {
      val johnWithInvalidTimestamp = john.copy(loginTimestamp = 2222L)
      val mockResponse = mock[Response]
      private val request: SsoLoginRequest = SsoLoginRequest(johnWithInvalidTimestamp.encodedToken, johnWithInvalidTimestamp.loginTimestamp)
      when(mockGovernmentGatewayService.ssoLogin(Matchers.eq(request))(Matchers.any[HeaderCarrier])).thenReturn(Future.failed(new UnauthorizedException("error", mockResponse)))
      when(mockSsoWhiteListService.check(URI.create(redirectUrl).toURL)).thenReturn(true)

      val encryptedPayload = SsoPayloadCrypto.encrypt( s"""{"gw": "${johnWithInvalidTimestamp.encodedToken}", "time": ${johnWithInvalidTimestamp.loginTimestamp}, "dest": "$redirectUrl"}""")

      val response = controller.postIn(FakeRequest("POST", s"www.governmentgateway.com")
        .withFormUrlEncodedBody("payload" -> encryptedPayload)
        .withSession(SessionKeys.userId -> johnWithInvalidTimestamp.userId, SessionKeys.name -> johnWithInvalidTimestamp.name, SessionKeys.affinityGroup -> johnWithInvalidTimestamp.affinityGroup, SessionKeys.token -> john.encodedToken))

      whenReady(response) {
        result =>
          val SimpleResult(header, _, _) = result
          header.status shouldBe 303
          header.headers("Location") shouldBe loginPage
          header.headers("Set-Cookie") should not include SessionKeys.userId
          header.headers("Set-Cookie") should not include SessionKeys.name
          header.headers("Set-Cookie") should not include SessionKeys.affinityGroup
          header.headers("Set-Cookie") should not include SessionKeys.token
          header.headers("Set-Cookie") should not include SessionKeys.authToken
      }

      expectAnSsoLoginFailedAuditEventFor(johnWithInvalidTimestamp, transactionFailureReason = "Unauthorized")
    }
    "invalidate the session if a session already exists but the login throws some other exception" in new WithSsoControllerInFakeApplication {
      val mockResponse = mock[Response]
      val request = SsoLoginRequest(john.encodedToken, john.loginTimestamp)
      val errorMessage = "something went wrong"
      when(mockGovernmentGatewayService.ssoLogin(Matchers.eq(request))(Matchers.any[HeaderCarrier])).thenReturn(Future.failed(new RuntimeException(errorMessage)))
      when(mockSsoWhiteListService.check(URI.create(redirectUrl).toURL)).thenReturn(true)

      val encryptedPayload = SsoPayloadCrypto.encrypt( s"""{"gw": "${john.encodedToken}", "time": ${john.loginTimestamp}, "dest": "$redirectUrl"}""")

      val response = controller.postIn(FakeRequest("POST", s"www.governmentgateway.com")
        .withFormUrlEncodedBody("payload" -> encryptedPayload)
        .withSession(SessionKeys.userId -> john.userId, SessionKeys.name -> john.name, SessionKeys.affinityGroup -> john.affinityGroup, SessionKeys.token -> john.encodedToken))

      whenReady(response) {
        result =>
          val SimpleResult(header, _, _) = result
          header.status shouldBe 303
          header.headers("Location") shouldBe loginPage
          header.headers("Set-Cookie") should not include SessionKeys.userId
          header.headers("Set-Cookie") should not include SessionKeys.name
          header.headers("Set-Cookie") should not include SessionKeys.affinityGroup
          header.headers("Set-Cookie") should not include SessionKeys.token
          header.headers("Set-Cookie") should not include SessionKeys.authToken
      }

      expectAnSsoLoginFailedAuditEventFor(john, transactionFailureReason = s"Unknown - $errorMessage")
    }

    "return 400 if the provided dest field is not a valid URL" in new WithSsoControllerInFakeApplication {
      val invalidUrl = "invalid_url"
      val encryptedPayload = SsoPayloadCrypto.encrypt( s"""{"gw": "${john.encodedToken}", "time": ${john.loginTimestamp}, "dest": "$invalidUrl"}""")

      val result = controller.postIn(FakeRequest("POST", s"www.governmentgateway.com")
        .withFormUrlEncodedBody("payload" -> encryptedPayload)
        .withSession(SessionKeys.userId -> john.userId, SessionKeys.name -> john.name, SessionKeys.token -> john.encodedToken))

      status(result) shouldBe 400

      expectAnSsoLoginFailedAuditEventFor(john, transactionFailureReason = "Invalid destination")
    }

    "return 400 if the dest field is not allowed by the white list" in new WithSsoControllerInFakeApplication {
      when(mockSsoWhiteListService.check(URI.create(redirectUrl).toURL)).thenReturn(false)

      val encryptedPayload = SsoPayloadCrypto.encrypt( s"""{"gw": "${john.encodedToken}", "time": ${john.loginTimestamp}, "dest": "$redirectUrl"}""")

      val result = controller.postIn(FakeRequest("POST", s"www.governmentgateway.com")
        .withFormUrlEncodedBody("payload" -> encryptedPayload)
        .withSession(SessionKeys.userId -> john.userId, SessionKeys.name -> john.name, SessionKeys.affinityGroup -> john.affinityGroup, SessionKeys.token -> john.encodedToken))

      status(result) shouldBe 400

      expectAnSsoLoginFailedAuditEventFor(john, transactionFailureReason = "Invalid destination")
    }

    "return 400 if the dest field is missing" in new WithSsoControllerInFakeApplication {
      val encryptedPayload = SsoPayloadCrypto.encrypt( s"""{"gw": "${john.encodedToken}", "time": ${john.loginTimestamp}}""")

      val result = controller.postIn(FakeRequest("POST", s"www.governmentgateway.com")
        .withFormUrlEncodedBody("payload" -> encryptedPayload)
        .withSession(SessionKeys.userId -> john.userId, SessionKeys.name -> john.name, SessionKeys.affinityGroup -> john.affinityGroup, SessionKeys.token -> john.encodedToken))

      status(result) shouldBe 400

      expectAnSsoLoginFailedAuditEventFor(john, transactionFailureReason = "Invalid destination")
    }
  }
  "The Single Sign-on logout page" should {
    " logout a logged-in user and redirect to the Portal loggedout page" in new WithSsoControllerInFakeApplication {

      val result = controller.out(FakeRequest("POST", s"www.governmentgateway.com").withSession(SessionKeys.userId -> john.userId, SessionKeys.name -> john.name, SessionKeys.token -> john.encodedToken))

      whenReady(result) {
        case SimpleResult(header, _, _) => {
          header.status shouldBe 303
          header.headers("Location") shouldBe "http://localhost:8080/portal/loggedout"
          header.headers("Set-Cookie") should not include SessionKeys.userId
          header.headers("Set-Cookie") should not include SessionKeys.name
          header.headers("Set-Cookie") should not include SessionKeys.affinityGroup
          header.headers("Set-Cookie") should not include SessionKeys.token
        }
        case _ => fail("the response from the SsoIn (logout) controller was not of the expected format")
      }

    }

    " logout a not logged-in user and redirect to the Portal loggedout page" in new WithSsoControllerInFakeApplication {

      val result = controller.out(FakeRequest("POST", s"www.governmentgateway.com").withSession("somePortalData" -> "somedata"))

      whenReady(result) {
        case SimpleResult(header, _, _) => {
          header.status shouldBe 303
          header.headers("Location") shouldBe "http://localhost:8080/portal/loggedout"
          header.headers("Set-Cookie") should not include SessionKeys.userId
          header.headers("Set-Cookie") should not include SessionKeys.name
          header.headers("Set-Cookie") should not include SessionKeys.affinityGroup
          header.headers("Set-Cookie") should not include SessionKeys.token
        }
        case _ => fail("the response from the SsoIn (logout) controller was not of the expected format")
      }

    }
  }

}


abstract class WithSsoControllerInFakeApplication extends WithApplication(FakeApplication(additionalConfiguration = Map("sso.encryption.key" -> "eU1qMlpESFRPN0hRNGJxNg==")))
with MockitoSugar with org.scalatest.Matchers {

  lazy val mockGovernmentGatewayService = mock[GovernmentGatewayConnector]
  val mockSsoWhiteListService = mock[SsoWhiteListService]
  val mockAuditConnector = mock[AuditConnector]
  val mockAuthConnector = mock[AuthConnector]


  val redirectUrl = "http://www.redirect-url.co.uk"

  def controller = new SsoInController(mockSsoWhiteListService, mockGovernmentGatewayService, mockAuditConnector)(mockAuthConnector)

  case class User(name: String,
                  userId: String,
                  credId: String,
                  affinityGroup: String,
                  encodedToken: String,
                  loginTimestamp: Long)

  val bob = User(
    name = "Bob Jones",
    userId = "authId/ROBERT",
    credId = "ROB'S-CRED-ID",
    encodedToken = "bobsToken",
    affinityGroup = "Partnership",
    loginTimestamp = 123456L
  )

  val john = User(
    name = "John Smith",
    userId = "authId/JOHNNY",
    credId = "JOHN'S-CRED-ID",
    encodedToken = "johnsToken",
    affinityGroup = "Individual",
    loginTimestamp = 12345L
  )

  def sessionEntry(key: String, value: String): String = {
    val encryptedValue = URLEncoder.encode(value, "UTF8")
    s"$key=$encryptedValue"
  }

  def expectAnSsoLoginSuccessfulAuditEventFor(user: User) {
    val auditEvent = ArgumentCaptor.forClass(classOf[AuditEvent])
    verify(mockAuditConnector).audit(auditEvent.capture())(Matchers.any())

    auditEvent.getValue.auditType should be("TxSucceeded")
    auditEvent.getValue.tags should contain("transactionName" -> "SSO Login")
    auditEvent.getValue.detail should (
      contain("authId" -> user.userId) and
        contain("name" -> user.name) and
        contain(SessionKeys.affinityGroup -> user.affinityGroup)
      )
    auditEvent.getValue.detail should not contain key("transactionFailureReason")
  }

  def expectAnSsoLoginFailedAuditEventFor(user: User, transactionFailureReason: String) {
    val auditEvent = ArgumentCaptor.forClass(classOf[AuditEvent])
    verify(mockAuditConnector).audit(auditEvent.capture())(Matchers.any())

    auditEvent.getValue.auditType should be("TxFailed")
    auditEvent.getValue.tags should contain("transactionName" -> "SSO Login")
    auditEvent.getValue.detail should contain(SessionKeys.token -> user.encodedToken)
    auditEvent.getValue.detail should contain("transactionFailureReason" -> transactionFailureReason)
  }
}
