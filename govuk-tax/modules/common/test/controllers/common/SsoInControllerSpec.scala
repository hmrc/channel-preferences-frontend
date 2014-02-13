package controllers.common

import org.scalatest.mock.MockitoSugar
import play.api.test.{FakeRequest, WithApplication}
import uk.gov.hmrc.common.microservice.governmentgateway.{GovernmentGatewayConnector, GovernmentGatewayLoginResponse, SsoLoginRequest}
import org.mockito.Mockito._
import java.net.{URI, URLEncoder}
import uk.gov.hmrc.common.BaseSpec
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
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import controllers.domain.AuthorityUtils._
import controllers.common.service.SsoWhiteListService
import play.libs.Json
import controllers.common.preferences.service.SsoPayloadCrypto

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
      val payload = """us3r4vSj%2Fx9Es%2BEC7LqvLWcmWpTTphiKR%2B3VFP7oJQRlmm7m2lTBNpfxpfmUwLxMDtR6nU9xsKzBc%2FWZJfjnHz4beunnjwr6pKfQjaRU1c5FoQukpywRe7roxxcLVytc%2BiBR2JVNNg67GA72YTUeQShtbZdmgdrs3JXjyIc6JxIKiYuOeosGQLc5eSJX7FzYlP9OIWH9ENGKNxqikYnDDvWnmEMiEgfHbz0oSHJyaQ4gV1EWul%2BuwhPaXMwm8XL2MLpmucyaRt7n%2BSQxzgHPzFTcF98IwkkvQarMMoRh%2Fz5UWpBQbYCzqeZUhHn7uq4p%2B%2BVsfS5sc5VBK5RCOwK4t1CH8GU2DSOub3ixWhD9DOEqdL%2FwUJVx04Fo84OQ3crUZmAcVBbzEJrRxJmCaLHX8%2BSJpP12rwCYwO8AwGEPcwDFA45dvJ2wfFkl5GwynQRYAlHxyTLEzizzK%2FxKGj6ioP%2Fp1m5xhOadactozjwGRUkq9IoO6AFNONIinKSiruxnCKwI3Ffc7bY4fatShDC%2BVir0ig7oAU040iKcpKKu7GfI9tDAqwqHnpSaNZ%2B1CkOWNHkxTb%2FUedHyP4gxqUNtmuFA%2FjlN1oyLJzZtJxIoB7uF8jwfpk%2BaBTN0ge3RdzsYwN%2BVHNmOIs1XNNTZb%2F2tuUnYynhGYjHZOy0e2Nr1yIjQZ0geyvB7WtLvxyyLW5P8Tw0pDErlehOGX3SPcCiFevqqQTpG0L%2B6q66LRQoYcg8A4or0ibJYao8nUKIgQdw1lG7gVhkZe39gpy27vker0f8Umi59%2BL2lQqFSWCt6XJjGBF26PREJ8HX5qVG0Ohkx4VbV78RpQbuirr5FQ0cs7nZHC6WTZLHQ%2B8EKzwQfKcNpgsHStjKcTYkHan6TEwjxq6OM%2FXd7MRtzowm8x%2BNnzaMF1DWqgToDIMGk%2F%2FgJmdRoukVCHeho8rF9S2LJdXrbydMfui3BTpWkL1m162lVYkK7cfIHQgfZbpGuzhwNK3hk%2Bd9ppyiQfPI2aCZRImO6j5a8X15SDvxdPkYDeLVsBw%3D%3D"""
      Json.parse(controller.decryptEncodedPayload(payload))
    }

    "Is able to handle not-encoded payload" in new WithSsoControllerInFakeApplication {
      val payload = """us3r4vSj/x9Es+EC7LqvLWcmWpTTphiKR+3VFP7oJQRlmm7m2lTBNpfxpfmUwLxMDtR6nU9xsKzBc/WZJfjnHz4beunnjwr6pKfQjaRU1c5FoQukpywRe7roxxcLVytc+iBR2JVNNg67GA72YTUeQShtbZdmgdrs3JXjyIc6JxIKiYuOeosGQLc5eSJX7FzYlP9OIWH9ENGKNxqikYnDDvWnmEMiEgfHbz0oSHJyaQ4gV1EWul+uwhPaXMwm8XL2MLpmucyaRt7n+SQxzgHPzFTcF98IwkkvQarMMoRh/z5UWpBQbYCzqeZUhHn7uq4p++VsfS5sc5VBK5RCOwK4t1CH8GU2DSOub3ixWhD9DOEqdL/wUJVx04Fo84OQ3crUZmAcVBbzEJrRxJmCaLHX8+SJpP12rwCYwO8AwGEPcwDFA45dvJ2wfFkl5GwynQRYAlHxyTLEzizzK/xKGj6ioP/p1m5xhOadactozjwGRUkq9IoO6AFNONIinKSiruxnCKwI3Ffc7bY4fatShDC+Vir0ig7oAU040iKcpKKu7GfI9tDAqwqHnpSaNZ+1CkOWNHkxTb/UedHyP4gxqUNtmuFA/jlN1oyLJzZtJxIoB7uF8jwfpk+aBTN0ge3RdzsYwN+VHNmOIs1XNNTZb/2tuUnYynhGYjHZOy0e2Nr1yIjQZ0geyvB7WtLvxyyLW5P8Tw0pDErlehOGX3SPcCiFevqqQTpG0L+6q66LRQoYcg8A4or0ibJYao8nUKIgQdw1lG7gVhkZe39gpy27vker0f8Umi59+L2lQqFSWCt6XJjGBF26PREJ8HX5qVG0Ohkx4VbV78RpQbuirr5FQ0cs7nZHC6WTZLHQ+8EKzwQfKcNpgsHStjKcTYkHan6TEwjxq6OM/Xd7MRtzowm8x+NnzaMF1DWqgToDIMGk//gJmdRoukVCHeho8rF9S2LJdXrbydMfui3BTpWkL1m162lVYkK7cfIHQgfZbpGuzhwNK3hk+d9ppyiQfPI2aCZRImO6j5a8X15SDvxdPkYDeLVsBw=="""
      Json.parse(controller.decryptEncodedPayload(payload))
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


abstract class WithSsoControllerInFakeApplication extends WithApplication(FakeApplication())
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
