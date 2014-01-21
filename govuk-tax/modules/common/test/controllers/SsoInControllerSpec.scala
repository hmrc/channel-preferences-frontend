package controllers

import common.service.SsoWhiteListService
import org.scalatest.mock.MockitoSugar
import play.api.test.{FakeRequest, WithApplication}
import uk.gov.hmrc.common.microservice.governmentgateway.{GatewayToken, GovernmentGatewayConnector, GovernmentGatewayResponse, SsoLoginRequest}
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
import uk.gov.hmrc.utils.DateTimeUtils
import org.mockito.{ArgumentCaptor, Matchers}
import controllers.common.actions.HeaderCarrier
import scala.concurrent.Future
import uk.gov.hmrc.common.microservice.audit.{AuditEvent, AuditConnector}
import uk.gov.hmrc.common.crypto.ApplicationCrypto.SsoPayloadCrypto

class SsoInControllerSpec extends BaseSpec with MockitoSugar with ScalaFutures with IntegrationPatience {

  val loginPage = routes.LoginController.businessTaxLogin().url

  "The Single Sign-on input page" should {
    "create a new session when the token is valid, the time not expired and no session exists - POST" in new WithSsoControllerInFakeApplication {
      when(mockGovernmentGatewayService.ssoLogin(Matchers.eq(SsoLoginRequest(john.encodedToken.encodeBase64, john.loginTimestamp)))(Matchers.any[HeaderCarrier])).thenReturn(GovernmentGatewayResponse(john.userId, john.name, john.affinityGroup, john.encodedToken))
      when(mockSsoWhiteListService.check(URI.create(redirectUrl).toURL)).thenReturn(true)

      val encryptedPayload = SsoPayloadCrypto.encrypt( s"""{"gw": "${john.encodedToken.encodeBase64}", "time": ${john.loginTimestamp}, "dest": "$redirectUrl"}""")
      val response = controller.postIn(FakeRequest("POST", s"www.governmentgateway.com").withFormUrlEncodedBody("payload" -> encryptedPayload))
      whenReady(response) {
        result =>
          val SimpleResult(header, _, _) = result
          header.status shouldBe 303
          header.headers("Location") shouldBe redirectUrl
          header.headers("Set-Cookie") should include(sessionEntry(SessionKeys.userId, john.userId))
          header.headers("Set-Cookie") should include(sessionEntry(SessionKeys.name, john.name))
          header.headers("Set-Cookie") should include(sessionEntry(SessionKeys.affinityGroup, john.affinityGroup))
          header.headers("Set-Cookie") should include(sessionEntry(SessionKeys.token, john.encodedToken.encodeBase64))
      }

      expectAnSsoLoginSuccessfulAuditEventFor(john)
    }

    "create a new session when the token is valid, the time not expired and no session exists - GET" in new WithSsoControllerInFakeApplication {
      when(mockGovernmentGatewayService.ssoLogin(Matchers.eq(SsoLoginRequest(john.encodedToken.encodeBase64, john.loginTimestamp)))(Matchers.any[HeaderCarrier])).thenReturn(GovernmentGatewayResponse(john.userId, john.name, john.affinityGroup, john.encodedToken))
      when(mockSsoWhiteListService.check(URI.create(redirectUrl).toURL)).thenReturn(true)

      val encryptedPayload = URLEncoder.encode(SsoPayloadCrypto.encrypt( s"""{"gw": "${john.encodedToken.encodeBase64}", "time": ${john.loginTimestamp}, "dest": "$redirectUrl"}"""), "UTF-8")
      val response = controller.getIn(encryptedPayload)(FakeRequest())
      whenReady(response) {
        result =>
          val SimpleResult(header, _, _) = result
          header.status shouldBe 303
          header.headers("Location") shouldBe redirectUrl
          header.headers("Set-Cookie") should include(sessionEntry(SessionKeys.userId, john.userId))
          header.headers("Set-Cookie") should include(sessionEntry(SessionKeys.name, john.name))
          header.headers("Set-Cookie") should include(sessionEntry(SessionKeys.affinityGroup, john.affinityGroup))
          header.headers("Set-Cookie") should include(sessionEntry(SessionKeys.token, john.encodedToken.encodeBase64))
      }

      expectAnSsoLoginSuccessfulAuditEventFor(john)
    }

    "replace any current session with a new one when the token is valid, and the time not expired - POST" in new WithSsoControllerInFakeApplication {

      val encryptedPayload = SsoPayloadCrypto.encrypt( s"""{"gw": "${bob.encodedToken.encodeBase64}", "time": ${bob.loginTimestamp}, "dest": "$redirectUrl"}""")
      val request = FakeRequest("POST", s"www.governmentgateway.com")
        .withFormUrlEncodedBody("payload" -> encryptedPayload)
        .withSession(SessionKeys.userId -> john.userId, SessionKeys.name -> john.name, SessionKeys.affinityGroup -> john.affinityGroup, SessionKeys.token -> john.encodedToken.encodeBase64)

      when(mockGovernmentGatewayService.ssoLogin(Matchers.eq(SsoLoginRequest(bob.encodedToken.encodeBase64, bob.loginTimestamp)))(Matchers.any[HeaderCarrier])).thenReturn(GovernmentGatewayResponse(bob.userId, bob.name, bob.affinityGroup, bob.encodedToken))
      when(mockSsoWhiteListService.check(URI.create(redirectUrl).toURL)).thenReturn(true)


      val response = controller.postIn(request)

      whenReady(response) {
        result =>
          val SimpleResult(header, _, _) = result
          header.status shouldBe 303
          header.headers("Location") shouldBe redirectUrl
          header.headers("Set-Cookie") should include(sessionEntry(SessionKeys.userId, bob.userId))
          header.headers("Set-Cookie") should include(sessionEntry(SessionKeys.name, bob.name))
          header.headers("Set-Cookie") should include(sessionEntry(SessionKeys.affinityGroup, bob.affinityGroup))
          header.headers("Set-Cookie") should include(sessionEntry(SessionKeys.token, bob.encodedToken.encodeBase64))
      }

      expectAnSsoLoginSuccessfulAuditEventFor(bob)
    }

     "replace any current session with a new one when the token is valid, and the time not expired - GET" in new WithSsoControllerInFakeApplication {

      val encryptedPayload = URLEncoder.encode(SsoPayloadCrypto.encrypt( s"""{"gw": "${bob.encodedToken.encodeBase64}", "time": ${bob.loginTimestamp}, "dest": "$redirectUrl"}"""), "UTF-8")
      val request = FakeRequest()
        .withSession(SessionKeys.userId -> john.userId, SessionKeys.name -> john.name, SessionKeys.affinityGroup -> john.affinityGroup, SessionKeys.token -> john.encodedToken.encodeBase64)

      when(mockGovernmentGatewayService.ssoLogin(Matchers.eq(SsoLoginRequest(bob.encodedToken.encodeBase64, bob.loginTimestamp)))(Matchers.any[HeaderCarrier])).thenReturn(GovernmentGatewayResponse(bob.userId, bob.name, bob.affinityGroup, bob.encodedToken))
      when(mockSsoWhiteListService.check(URI.create(redirectUrl).toURL)).thenReturn(true)


      val response = controller.getIn(encryptedPayload)(request)

      whenReady(response) {
        result =>
          val SimpleResult(header, _, _) = result
          header.status shouldBe 303
          header.headers("Location") shouldBe redirectUrl
          header.headers("Set-Cookie") should include(sessionEntry(SessionKeys.userId, bob.userId))
          header.headers("Set-Cookie") should include(sessionEntry(SessionKeys.name, bob.name))
          header.headers("Set-Cookie") should include(sessionEntry(SessionKeys.affinityGroup, bob.affinityGroup))
          header.headers("Set-Cookie") should include(sessionEntry(SessionKeys.token, bob.encodedToken.encodeBase64))
      }

      expectAnSsoLoginSuccessfulAuditEventFor(bob)
    }

    "invalidate the session if a session already exists but the login is incorrect" in new WithSsoControllerInFakeApplication {
      val johnInvalidCredentials = john.copy(encodedToken = john.encodedToken.copy(encodeBase64 = "invalidToken"))
      when(mockGovernmentGatewayService.ssoLogin(Matchers.eq(SsoLoginRequest(johnInvalidCredentials.encodedToken.encodeBase64, johnInvalidCredentials.loginTimestamp)))(Matchers.any[HeaderCarrier])).thenReturn(Future.failed(new IllegalStateException("error")))
      when(mockSsoWhiteListService.check(URI.create(redirectUrl).toURL)).thenReturn(true)
      val encryptedPayload = SsoPayloadCrypto.encrypt( s"""{"gw": "${johnInvalidCredentials.encodedToken.encodeBase64}", "time": ${johnInvalidCredentials.loginTimestamp}, "dest": "$redirectUrl"}""")

      val response = controller.postIn(FakeRequest("POST", s"www.governmentgateway.com")
        .withFormUrlEncodedBody("payload" -> encryptedPayload)
        .withSession(SessionKeys.userId -> johnInvalidCredentials.userId, SessionKeys.name -> johnInvalidCredentials.name, SessionKeys.token -> johnInvalidCredentials.encodedToken.encodeBase64))

      whenReady(response) {
        result =>
          val SimpleResult(header, _, _) = result
          header.status shouldBe 303
          header.headers("Location") shouldBe loginPage
          header.headers("Set-Cookie") should not include SessionKeys.userId
          header.headers("Set-Cookie") should not include SessionKeys.name
          header.headers("Set-Cookie") should not include SessionKeys.token
      }

      expectAnSsoLoginFailedAuditEventFor(johnInvalidCredentials, transactionFailureReason = "Invalid Token")
    }

    "invalidate the session if a session already exists but the login throws an Unauthorised Exception" in new WithSsoControllerInFakeApplication {
      val johnWithInvalidTimestamp = john.copy(loginTimestamp = 2222L)
      val mockResponse = mock[Response]
      private val request: SsoLoginRequest = SsoLoginRequest(johnWithInvalidTimestamp.encodedToken.encodeBase64, johnWithInvalidTimestamp.loginTimestamp)
      when(mockGovernmentGatewayService.ssoLogin(Matchers.eq(request))(Matchers.any[HeaderCarrier])).thenReturn(Future.failed(new UnauthorizedException("error", mockResponse)))
      when(mockSsoWhiteListService.check(URI.create(redirectUrl).toURL)).thenReturn(true)

      val encryptedPayload = SsoPayloadCrypto.encrypt( s"""{"gw": "${johnWithInvalidTimestamp.encodedToken.encodeBase64}", "time": ${johnWithInvalidTimestamp.loginTimestamp}, "dest": "$redirectUrl"}""")

      val response = controller.postIn(FakeRequest("POST", s"www.governmentgateway.com")
        .withFormUrlEncodedBody("payload" -> encryptedPayload)
        .withSession(SessionKeys.userId -> johnWithInvalidTimestamp.userId, SessionKeys.name -> johnWithInvalidTimestamp.name, SessionKeys.affinityGroup -> johnWithInvalidTimestamp.affinityGroup, SessionKeys.token -> john.encodedToken.encodeBase64))

      whenReady(response) {
        result =>
          val SimpleResult(header, _, _) = result
          header.status shouldBe 303
          header.headers("Location") shouldBe loginPage
          header.headers("Set-Cookie") should not include SessionKeys.userId
          header.headers("Set-Cookie") should not include SessionKeys.name
          header.headers("Set-Cookie") should not include SessionKeys.affinityGroup
          header.headers("Set-Cookie") should not include SessionKeys.token
      }

      expectAnSsoLoginFailedAuditEventFor(johnWithInvalidTimestamp, transactionFailureReason = "Unauthorized")
    }
    "invalidate the session if a session already exists but the login throws some other exception" in new WithSsoControllerInFakeApplication {
      val mockResponse = mock[Response]
      val request = SsoLoginRequest(john.encodedToken.encodeBase64, john.loginTimestamp)
      val errorMessage = "something went wrong"
      when(mockGovernmentGatewayService.ssoLogin(Matchers.eq(request))(Matchers.any[HeaderCarrier])).thenReturn(Future.failed(new RuntimeException(errorMessage)))
      when(mockSsoWhiteListService.check(URI.create(redirectUrl).toURL)).thenReturn(true)

      val encryptedPayload = SsoPayloadCrypto.encrypt( s"""{"gw": "${john.encodedToken.encodeBase64}", "time": ${john.loginTimestamp}, "dest": "$redirectUrl"}""")

      val response = controller.postIn(FakeRequest("POST", s"www.governmentgateway.com")
        .withFormUrlEncodedBody("payload" -> encryptedPayload)
        .withSession(SessionKeys.userId -> john.userId, SessionKeys.name -> john.name, SessionKeys.affinityGroup -> john.affinityGroup, SessionKeys.token -> john.encodedToken.encodeBase64))

      whenReady(response) {
        result =>
          val SimpleResult(header, _, _) = result
          header.status shouldBe 303
          header.headers("Location") shouldBe loginPage
          header.headers("Set-Cookie") should not include SessionKeys.userId
          header.headers("Set-Cookie") should not include SessionKeys.name
          header.headers("Set-Cookie") should not include SessionKeys.affinityGroup
          header.headers("Set-Cookie") should not include SessionKeys.token
      }

      expectAnSsoLoginFailedAuditEventFor(john, transactionFailureReason = s"Unknown - $errorMessage")
    }

    "return 400 if the provided dest field is not a valid URL" in new WithSsoControllerInFakeApplication {
      val invalidUrl = "invalid_url"
      val encryptedPayload = SsoPayloadCrypto.encrypt( s"""{"gw": "${john.encodedToken.encodeBase64}", "time": ${john.loginTimestamp}, "dest": "$invalidUrl"}""")

      val result = controller.postIn(FakeRequest("POST", s"www.governmentgateway.com")
        .withFormUrlEncodedBody("payload" -> encryptedPayload)
        .withSession(SessionKeys.userId -> john.userId, SessionKeys.name -> john.name, SessionKeys.token -> john.encodedToken.encodeBase64))

      status(result) shouldBe 400

      expectAnSsoLoginFailedAuditEventFor(john, transactionFailureReason = "Invalid destination")
    }

    "return 400 if the dest field is not allowed by the white list" in new WithSsoControllerInFakeApplication {
      when(mockSsoWhiteListService.check(URI.create(redirectUrl).toURL)).thenReturn(false)

      val encryptedPayload = SsoPayloadCrypto.encrypt( s"""{"gw": "${john.encodedToken.encodeBase64}", "time": ${john.loginTimestamp}, "dest": "$redirectUrl"}""")

      val result = controller.postIn(FakeRequest("POST", s"www.governmentgateway.com")
        .withFormUrlEncodedBody("payload" -> encryptedPayload)
        .withSession(SessionKeys.userId -> john.userId, SessionKeys.name -> john.name, SessionKeys.affinityGroup -> john.affinityGroup, SessionKeys.token -> john.encodedToken.encodeBase64))

      status(result) shouldBe 400

      expectAnSsoLoginFailedAuditEventFor(john, transactionFailureReason = "Invalid destination")
    }

    "return 400 if the dest field is missing" in new WithSsoControllerInFakeApplication {
      val encryptedPayload = SsoPayloadCrypto.encrypt( s"""{"gw": "${john.encodedToken.encodeBase64}", "time": ${john.loginTimestamp}}""")

      val result = controller.postIn(FakeRequest("POST", s"www.governmentgateway.com")
        .withFormUrlEncodedBody("payload" -> encryptedPayload)
        .withSession(SessionKeys.userId -> john.userId, SessionKeys.name -> john.name, SessionKeys.affinityGroup -> john.affinityGroup, SessionKeys.token -> john.encodedToken.encodeBase64))

      status(result) shouldBe 400

      expectAnSsoLoginFailedAuditEventFor(john, transactionFailureReason = "Invalid destination")
    }
  }
  "The Single Sign-on logout page" should {
    " logout a logged-in user and redirect to the Portal loggedout page" in new WithSsoControllerInFakeApplication {

      val result = controller.out(FakeRequest("POST", s"www.governmentgateway.com").withSession(SessionKeys.userId -> john.userId, SessionKeys.name -> john.name, SessionKeys.token -> john.encodedToken.encodeBase64))

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
  val redirectUrl = "http://www.redirect-url.co.uk"

  def controller = new SsoInController(mockSsoWhiteListService, mockGovernmentGatewayService, mockAuditConnector)(null)

  case class User(name: String,
                  userId: String,
                  affinityGroup: String,
                  encodedToken: GatewayToken,
                  loginTimestamp: Long
                   )

  val bob = User(
    name = "Bob Jones",
    userId = "authId/ROBERT",
    encodedToken = GatewayToken("bobsToken", DateTimeUtils.now, DateTimeUtils.now),
    affinityGroup = "Partnership",
    loginTimestamp = 123456L
  )

  val john = User(
    name = "John Smith",
    userId = "authId/JOHNNY",
    encodedToken = GatewayToken("johnsToken", DateTimeUtils.now, DateTimeUtils.now),
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
    auditEvent.getValue.detail should contain(SessionKeys.token -> user.encodedToken.encodeBase64)
    auditEvent.getValue.detail should contain("transactionFailureReason" -> transactionFailureReason)
  }
}
