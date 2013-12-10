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
import uk.gov.hmrc.microservice.UnauthorizedException
import play.api.libs.ws.Response
import play.api.test.FakeApplication
import play.api.mvc.SimpleResult
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.utils.DateTimeUtils
import org.mockito.{ArgumentCaptor, Matchers}
import controllers.common.actions.HeaderCarrier
import scala.concurrent.Future
import uk.gov.hmrc.common.microservice.audit.{AuditEvent, AuditConnector}

class SsoInControllerSpec extends BaseSpec with MockitoSugar with CookieEncryption with ScalaFutures {

  "The Single Sign-on input page" should {
    "create a new session when the token is valid, the time not expired and no session exists" in new WithSsoControllerInFakeApplication {
      when(mockGovernmentGatewayService.ssoLogin(Matchers.eq(SsoLoginRequest(john.encodedToken.encodeBase64, john.loginTimestamp)))(Matchers.any[HeaderCarrier])).thenReturn(GovernmentGatewayResponse(john.userId, john.name, john.affinityGroup, john.encodedToken))
      when(mockSsoWhiteListService.check(URI.create(redirectUrl).toURL)).thenReturn(true)

      val encryptedPayload = SsoPayloadEncryptor.encrypt( s"""{"gw": "${john.encodedToken.encodeBase64}", "time": ${john.loginTimestamp}, "dest": "$redirectUrl"}""")
      val response = controller.in(FakeRequest("POST", s"www.governmentgateway.com").withFormUrlEncodedBody("payload" -> encryptedPayload))
      whenReady(response) {
        result =>
          val SimpleResult(header, _, _) = result
          header.status shouldBe 303
          header.headers("Location") shouldBe redirectUrl
          header.headers("Set-Cookie") should include(sessionEntry("userId", john.userId))
          header.headers("Set-Cookie") should include(sessionEntry("name", john.name))
          header.headers("Set-Cookie") should include(sessionEntry("affinityGroup", john.affinityGroup))
          header.headers("Set-Cookie") should include(sessionEntry("token", john.encodedToken.encodeBase64))
      }

      expectAnSsoLoginAuditEventFor(john)
    }

    "replace any current session with a new one when the token is valid, and the time not expired" in new WithSsoControllerInFakeApplication {

      val encryptedPayload = SsoPayloadEncryptor.encrypt( s"""{"gw": "${bob.encodedToken.encodeBase64}", "time": ${bob.loginTimestamp}, "dest": "$redirectUrl"}""")
      val request = FakeRequest("POST", s"www.governmentgateway.com")
        .withFormUrlEncodedBody("payload" -> encryptedPayload)
        .withSession("userId" -> encrypt(john.userId), "name" -> john.name, "affinityGroup" -> john.affinityGroup, "token" -> john.encodedToken.encodeBase64)

      when(mockGovernmentGatewayService.ssoLogin(Matchers.eq(SsoLoginRequest(bob.encodedToken.encodeBase64, bob.loginTimestamp)))(Matchers.any[HeaderCarrier])).thenReturn(GovernmentGatewayResponse(bob.userId, bob.name, bob.affinityGroup, bob.encodedToken))
      when(mockSsoWhiteListService.check(URI.create(redirectUrl).toURL)).thenReturn(true)


      val response = controller.in(request)

      whenReady(response) {
        result =>
          val SimpleResult(header, _, _) = result
          header.status shouldBe 303
          header.headers("Location") shouldBe redirectUrl
          header.headers("Set-Cookie") should include(sessionEntry("userId", bob.userId))
          header.headers("Set-Cookie") should include(sessionEntry("name", bob.name))
          header.headers("Set-Cookie") should include(sessionEntry("affinityGroup", bob.affinityGroup))
          header.headers("Set-Cookie") should include(sessionEntry("token", bob.encodedToken.encodeBase64))
      }

      expectAnSsoLoginAuditEventFor(bob)
    }

    "invalidate the session if a session already exists but the login is incorrect" in new WithSsoControllerInFakeApplication {
      when(mockGovernmentGatewayService.ssoLogin(Matchers.eq(SsoLoginRequest(john.invalidEncodedToken, john.loginTimestamp)))(Matchers.any[HeaderCarrier])).thenReturn(Future.failed(new IllegalStateException("error")))
      when(mockSsoWhiteListService.check(URI.create(redirectUrl).toURL)).thenReturn(true)
      val encryptedPayload = SsoPayloadEncryptor.encrypt( s"""{"gw": "${john.invalidEncodedToken}", "time": ${john.loginTimestamp}, "dest": "$redirectUrl"}""")

      val response = controller.in(FakeRequest("POST", s"www.governmentgateway.com")
        .withFormUrlEncodedBody("payload" -> encryptedPayload)
        .withSession("userId" -> encrypt(john.userId), "name" -> john.name, "token" -> john.encodedToken.encodeBase64))

      whenReady(response) {
        result =>
          val SimpleResult(header, _, _) = result
          header.status shouldBe 303
          header.headers("Location") shouldBe "/"
          header.headers("Set-Cookie") should not include "userId"
          header.headers("Set-Cookie") should not include "name"
          header.headers("Set-Cookie") should not include "token"
      }
    }

    "invalidate the session if a session already exists but the login throws an Unauthorised Exception" in new WithSsoControllerInFakeApplication {
      val mockResponse = mock[Response]
      private val request: SsoLoginRequest = SsoLoginRequest(john.encodedToken.encodeBase64, john.invalidLoginTimestamp)
      when(mockGovernmentGatewayService.ssoLogin(Matchers.eq(request))(Matchers.any[HeaderCarrier])).thenReturn(Future.failed(new UnauthorizedException("error", mockResponse)))
      when(mockSsoWhiteListService.check(URI.create(redirectUrl).toURL)).thenReturn(true)

      val encryptedPayload = SsoPayloadEncryptor.encrypt( s"""{"gw": "${john.encodedToken.encodeBase64}", "time": ${john.invalidLoginTimestamp}, "dest": "$redirectUrl"}""")

      val response = controller.in(FakeRequest("POST", s"www.governmentgateway.com")
        .withFormUrlEncodedBody("payload" -> encryptedPayload)
        .withSession("userId" -> encrypt(john.userId), "name" -> john.name, "affinityGroup" -> john.affinityGroup, "token" -> john.encodedToken.encodeBase64))

      whenReady(response) {
        result =>
          val SimpleResult(header, _, _) = result
          header.status shouldBe 303
          header.headers("Location") shouldBe "/"
          header.headers("Set-Cookie") should not include "userId"
          header.headers("Set-Cookie") should not include "name"
          header.headers("Set-Cookie") should not include "affinityGroup"
          header.headers("Set-Cookie") should not include "token"
      }
    }

    "return 400 if the provided dest field is not a valid URL" in new WithSsoControllerInFakeApplication {
      val invalidUrl = "invalid_url"
      val encryptedPayload = SsoPayloadEncryptor.encrypt( s"""{"gw": "${john.encodedToken}", "time": ${john.invalidLoginTimestamp}, "dest": "$invalidUrl"}""")

      val result = controller.in(FakeRequest("POST", s"www.governmentgateway.com")
        .withFormUrlEncodedBody("payload" -> encryptedPayload)
        .withSession("userId" -> encrypt(john.userId), "name" -> john.name, "token" -> john.encodedToken.encodeBase64))

      status(result) shouldBe 400

    }

    "return 400 if the dest field is not allowed by the white list" in new WithSsoControllerInFakeApplication {
      when(mockSsoWhiteListService.check(URI.create(redirectUrl).toURL)).thenReturn(false)

      val encryptedPayload = SsoPayloadEncryptor.encrypt( s"""{"gw": "${john.encodedToken}", "time": ${john.invalidLoginTimestamp}, "dest": "$redirectUrl"}""")

      val result = controller.in(FakeRequest("POST", s"www.governmentgateway.com")
        .withFormUrlEncodedBody("payload" -> encryptedPayload)
        .withSession("userId" -> encrypt(john.userId), "name" -> john.name, "affinityGroup" -> john.affinityGroup, "token" -> john.encodedToken.encodeBase64))

      status(result) shouldBe 400
    }

    "return 400 if the dest field is missing" in new WithSsoControllerInFakeApplication {
      val encryptedPayload = SsoPayloadEncryptor.encrypt( s"""{"gw": "${john.encodedToken}", "time": ${john.invalidLoginTimestamp}}""")

      val result = controller.in(FakeRequest("POST", s"www.governmentgateway.com")
        .withFormUrlEncodedBody("payload" -> encryptedPayload)
        .withSession("userId" -> encrypt(john.userId), "name" -> john.name, "affinityGroup" -> john.affinityGroup, "token" -> john.encodedToken.encodeBase64))

      status(result) shouldBe 400

    }
  }

  "The Single Sign-on logout page" should {
    " logout a logged-in user and redirect to the Portal loggedout page" in new WithSsoControllerInFakeApplication {

      val result = controller.out(FakeRequest("POST", s"www.governmentgateway.com").withSession("userId" -> encrypt(john.userId), "name" -> john.name, "token" -> john.encodedToken.encodeBase64))

      whenReady(result) {
        case SimpleResult(header, _, _) => {
          header.status shouldBe 303
          header.headers("Location") shouldBe "http://localhost:8080/portal/loggedout"
          header.headers("Set-Cookie") should not include "userId"
          header.headers("Set-Cookie") should not include "name"
          header.headers("Set-Cookie") should not include "affinityGroup"
          header.headers("Set-Cookie") should not include "token"
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
          header.headers("Set-Cookie") should not include "userId"
          header.headers("Set-Cookie") should not include "name"
          header.headers("Set-Cookie") should not include "affinityGroup"
          header.headers("Set-Cookie") should not include "token"
        }
        case _ => fail("the response from the SsoIn (logout) controller was not of the expected format")
      }

    }
  }

}


abstract class WithSsoControllerInFakeApplication extends WithApplication(FakeApplication())
with MockitoSugar with CookieEncryption with org.scalatest.Matchers {

  lazy val mockGovernmentGatewayService = mock[GovernmentGatewayConnector]
  val mockSsoWhiteListService = mock[SsoWhiteListService]
  val mockAuditConnector = mock[AuditConnector]
  val redirectUrl = "http://www.redirect-url.co.uk"

  def controller = new SsoInController(mockSsoWhiteListService, mockGovernmentGatewayService, mockAuditConnector)(null)

  trait User {
    val name: String
    val userId: String
    val affinityGroup: String
  }

  object bob extends User {
    val name = "Bob Jones"
    val userId = "authId/ROBERT"
    val encodedToken = GatewayToken("bobsToken", DateTimeUtils.now, DateTimeUtils.now)
    val affinityGroup = "Partnership"
    val loginTimestamp = 123456L
  }

  object john extends User {
    val name = "John Smith"
    val userId = "authId/JOHNNY"
    val encodedToken = GatewayToken("johnsToken", DateTimeUtils.now, DateTimeUtils.now)
    val affinityGroup = "Individual"
    val invalidEncodedToken = "invalidToken"
    val loginTimestamp = 12345L
    val invalidLoginTimestamp = 2222L
  }

  def sessionEntry(key: String, unencryptedValue: String): String = {
    val encryptedValue = URLEncoder.encode(encrypt(unencryptedValue), "UTF8")
    s"$key=$encryptedValue"
  }

  def expectAnSsoLoginAuditEventFor(user: User) {
    val auditEvent = ArgumentCaptor.forClass(classOf[AuditEvent])
    verify(mockAuditConnector).audit(auditEvent.capture())(Matchers.any())

    auditEvent.getValue.auditType should be("TxSucceded")
    auditEvent.getValue.tags should contain("transactionName" -> "SSO Login Completion")
    auditEvent.getValue.detail should (
      contain("authId" -> user.userId) and
        contain("name" -> user.name) and
        contain("affinityGroup" -> user.affinityGroup)
      )
  }
}
