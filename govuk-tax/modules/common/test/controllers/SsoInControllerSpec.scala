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

class SsoInControllerSpec extends BaseSpec with MockitoSugar with CookieEncryption with ScalaFutures {

  private lazy val mockGovernmentGatewayService = mock[GovernmentGatewayConnector]
  private val mockSsoWhiteListService = mock[SsoWhiteListService]
  private val redirectUrl = "http://www.redirect-url.co.uk"

  private def controller = new SsoInController(mockSsoWhiteListService, mockGovernmentGatewayService, null)(null)

  private object bob {
    val name = "Bob Jones"
    val userId = "authId/ROBERT"
    val encodedToken = GatewayToken("bobsToken", DateTimeUtils.now, DateTimeUtils.now)
    val affinityGroup = "Partnership"
    val loginTimestamp = 123456L
  }

  private object john {
    val name = "John Smith"
    val userId = "authId/JOHNNY"
    val encodedToken = GatewayToken("johnsToken", DateTimeUtils.now, DateTimeUtils.now)
    val affinityGroup = "Individual"
    val invalidEncodedToken = "invalidToken"
    val loginTimestamp = 12345L
    val invalidLoginTimestamp = 2222L
  }

  override protected def before(fun: => Any): Unit = {
    reset(mockGovernmentGatewayService, mockSsoWhiteListService)
  }

  "The Single Sign-on input page" should {
    "create a new session when the token is valid, the time not expired and no session exists" in new WithApplication(FakeApplication()) {
      when(mockGovernmentGatewayService.ssoLogin(SsoLoginRequest(john.encodedToken.encodeBase64, john.loginTimestamp))).thenReturn(GovernmentGatewayResponse(john.userId, john.name, john.affinityGroup, john.encodedToken))
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

    }

    "replace the session details if a session already exists and the login is correct" in new WithApplication(FakeApplication()) {

      when(mockGovernmentGatewayService.ssoLogin(SsoLoginRequest(bob.encodedToken.encodeBase64, bob.loginTimestamp))).thenReturn(GovernmentGatewayResponse(bob.userId, bob.name, bob.affinityGroup, bob.encodedToken))
      when(mockSsoWhiteListService.check(URI.create(redirectUrl).toURL)).thenReturn(true)

      val encryptedPayload = SsoPayloadEncryptor.encrypt( s"""{"gw": "${bob.encodedToken.encodeBase64}", "time": ${bob.loginTimestamp}, "dest": "$redirectUrl"}""")

      val response = controller.in(FakeRequest("POST", s"www.governmentgateway.com")
        .withFormUrlEncodedBody("payload" -> encryptedPayload)
        .withSession("userId" -> encrypt(john.userId), "name" -> john.name, "affinityGroup" -> john.affinityGroup, "token" -> john.encodedToken.encodeBase64))

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

    }

    "invalidate the session if a session already exists but the login is incorrect" in new WithApplication(FakeApplication()) {
      when(mockGovernmentGatewayService.ssoLogin(SsoLoginRequest(john.invalidEncodedToken, john.loginTimestamp))).thenThrow(new IllegalStateException("error"))
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

    "invalidate the session if a session already exists but the login throws an Unauthorised Exception" in new WithApplication(FakeApplication()) {
      val mockResponse = mock[Response]
      when(mockGovernmentGatewayService.ssoLogin(SsoLoginRequest(john.encodedToken.encodeBase64, john.invalidLoginTimestamp))).thenThrow(new UnauthorizedException("error", mockResponse))
      when(mockSsoWhiteListService.check(URI.create(redirectUrl).toURL)).thenReturn(true)

      val encryptedPayload = SsoPayloadEncryptor.encrypt( s"""{"gw": "${john.encodedToken}", "time": ${john.invalidLoginTimestamp}, "dest": "$redirectUrl"}""")

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

    "return 400 if the provided dest field is not a valid URL" in new WithApplication(FakeApplication()) {
      val invalidUrl = "invalid_url"
      val encryptedPayload = SsoPayloadEncryptor.encrypt( s"""{"gw": "${john.encodedToken}", "time": ${john.invalidLoginTimestamp}, "dest": "$invalidUrl"}""")

      val result = controller.in(FakeRequest("POST", s"www.governmentgateway.com")
        .withFormUrlEncodedBody("payload" -> encryptedPayload)
        .withSession("userId" -> encrypt(john.userId), "name" -> john.name, "token" -> john.encodedToken.encodeBase64))

      status(result) shouldBe 400

    }

    "return 400 if the dest field is not allowed by the white list" in new WithApplication(FakeApplication()) {
      when(mockSsoWhiteListService.check(URI.create(redirectUrl).toURL)).thenReturn(false)

      val encryptedPayload = SsoPayloadEncryptor.encrypt( s"""{"gw": "${john.encodedToken}", "time": ${john.invalidLoginTimestamp}, "dest": "$redirectUrl"}""")

      val result = controller.in(FakeRequest("POST", s"www.governmentgateway.com")
        .withFormUrlEncodedBody("payload" -> encryptedPayload)
        .withSession("userId" -> encrypt(john.userId), "name" -> john.name, "affinityGroup" -> john.affinityGroup, "token" -> john.encodedToken.encodeBase64))

      status(result) shouldBe 400
    }

    "return 400 if the dest field is missing" in new WithApplication(FakeApplication()) {
      val encryptedPayload = SsoPayloadEncryptor.encrypt( s"""{"gw": "${john.encodedToken}", "time": ${john.invalidLoginTimestamp}}""")

      val result = controller.in(FakeRequest("POST", s"www.governmentgateway.com")
        .withFormUrlEncodedBody("payload" -> encryptedPayload)
        .withSession("userId" -> encrypt(john.userId), "name" -> john.name, "affinityGroup" -> john.affinityGroup, "token" -> john.encodedToken.encodeBase64))

      status(result) shouldBe 400

    }

    "The Single Sign-on logout page" should {
      " logout a logged-in user and redirect to the Portal loggedout page" in new WithApplication(FakeApplication()) {

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

      " logout a not logged-in user and redirect to the Portal loggedout page" in new WithApplication(FakeApplication()) {

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

  private def sessionEntry(key: String, unencryptedValue: String): String = {
    val encryptedValue = URLEncoder.encode(encrypt(unencryptedValue), "UTF8")
    s"$key=$encryptedValue"
  }

}
