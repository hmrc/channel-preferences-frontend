package controllers

import test.BaseSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import play.api.test.{ FakeRequest, WithApplication, FakeApplication }
import microservice.{ UnauthorizedException, MockMicroServicesForTests }
import microservice.governmentgateway.{ GovernmentGatewayResponse, ValidateTokenRequest, GovernmentGatewayMicroService }
import org.mockito.Mockito._
import play.api.mvc.{ SimpleResult, Result }
import java.net.URLEncoder
import play.api.libs.ws.Response

class SsoInControllerSpec extends BaseSpec with ShouldMatchers with MockitoSugar with CookieEncryption {

  private val mockGovernmentGatewayService = mock[GovernmentGatewayMicroService]
  private val redirectUrl = "www.redirect-url.co.uk"

  private object bob {
    val name = "Bob Jones"
    val userId = "authId/ROBERT"
    val encodedToken = "bobsToken"
    val loginTimestamp = 123456L
  }

  private object john {
    val name = "John Smith"
    val userId = "authId/JOHNNY"
    val encodedToken = "johnsToken"
    val invalidEncodedToken = "invalidToken"
    val loginTimestamp = 12345L
    val invalidLoginTimestamp = 2222L
  }

  private def controller = new SsoInController with MockMicroServicesForTests {
    override val governmentGatewayMicroService = mockGovernmentGatewayService
  }

  "The Single Sign-on input page" should {
    "create a new session when the token is valid, the time not expired and no session exists" in new WithApplication(FakeApplication()) {
      when(mockGovernmentGatewayService.validateToken(ValidateTokenRequest(john.encodedToken, john.loginTimestamp))).thenReturn(GovernmentGatewayResponse(john.userId, john.name, john.encodedToken))

      val encryptedPayload = SsoPayloadEncryptor.encrypt(s"""{"gw": "${john.encodedToken}", "time": ${john.loginTimestamp}, "dest": "${redirectUrl}"}""")
      val result: Result = controller.in(FakeRequest("POST", s"www.governmentgateway.com").withFormUrlEncodedBody("payload" -> encryptedPayload))
      result match {
        case SimpleResult(header, _) => {
          header.status shouldBe 303
          header.headers("Location") shouldBe redirectUrl
          header.headers("Set-Cookie") should include(sessionEntry("userId", john.userId))
          header.headers("Set-Cookie") should include(sessionEntry("name", john.name))
          header.headers("Set-Cookie") should include(sessionEntry("token", john.encodedToken))
        }
        case _ => fail("the response from the SsoIn controller was not of the expected format")
      }

    }

    "replace the session details if a session already exists and the login is correct" in new WithApplication(FakeApplication()) {

      when(mockGovernmentGatewayService.validateToken(ValidateTokenRequest(bob.encodedToken, bob.loginTimestamp))).thenReturn(GovernmentGatewayResponse(bob.userId, bob.name, bob.encodedToken))

      val encryptedPayload = SsoPayloadEncryptor.encrypt(s"""{"gw": "${bob.encodedToken}", "time": ${bob.loginTimestamp}, "dest": "${redirectUrl}"}""")

      val result: Result = controller.in(FakeRequest("POST", s"www.governmentgateway.com")
        .withFormUrlEncodedBody("payload" -> encryptedPayload)
        .withSession("userId" -> john.userId, "name" -> john.name, "token" -> john.encodedToken))

      result match {
        case SimpleResult(header, _) => {
          header.status shouldBe 303
          header.headers("Location") shouldBe redirectUrl
          header.headers("Set-Cookie") should include(sessionEntry("userId", bob.userId))
          header.headers("Set-Cookie") should include(sessionEntry("name", bob.name))
          header.headers("Set-Cookie") should include(sessionEntry("token", bob.encodedToken))
        }
        case _ => fail("the response from the SsoIn controller was not of the expected format")
      }

    }

    "invalidate the session if a session already exists but the login is incorrect" in new WithApplication(FakeApplication()) {
      when(mockGovernmentGatewayService.validateToken(ValidateTokenRequest(john.invalidEncodedToken, john.loginTimestamp))).thenThrow(new IllegalStateException("error"))

      val encryptedPayload = SsoPayloadEncryptor.encrypt(s"""{"gw": "${john.invalidEncodedToken}", "time": ${john.loginTimestamp}, "dest": "${redirectUrl}"}""")

      val result: Result = controller.in(FakeRequest("POST", s"www.governmentgateway.com")
        .withFormUrlEncodedBody("payload" -> encryptedPayload)
        .withSession("userId" -> john.userId, "name" -> john.name, "token" -> john.encodedToken))

      result match {
        case SimpleResult(header, _) => {
          header.status shouldBe 303
          header.headers("Location") shouldBe "/"
          header.headers("Set-Cookie") should not include "userId"
          header.headers("Set-Cookie") should not include "name"
          header.headers("Set-Cookie") should not include "token"
        }
        case _ => fail("the response from the SsoIn controller was not of the expected format")
      }
    }

    "invalidate the session if a session already exists but the login throws an Unauthorised Exception" in new WithApplication(FakeApplication()) {
      val mockResponse = mock[Response]
      when(mockGovernmentGatewayService.validateToken(ValidateTokenRequest(john.encodedToken, john.invalidLoginTimestamp))).thenThrow(new UnauthorizedException("error", mockResponse))

      val encryptedPayload = SsoPayloadEncryptor.encrypt(s"""{"gw": "${john.encodedToken}", "time": ${john.invalidLoginTimestamp}, "dest": "${redirectUrl}"}""")

      val result: Result = controller.in(FakeRequest("POST", s"www.governmentgateway.com")
        .withFormUrlEncodedBody("payload" -> encryptedPayload)
        .withSession("userId" -> john.userId, "name" -> john.name, "token" -> john.encodedToken))

      result match {
        case SimpleResult(header, _) => {
          header.status shouldBe 303
          header.headers("Location") shouldBe "/"
          header.headers("Set-Cookie") should not include "userId"
          header.headers("Set-Cookie") should not include "name"
          header.headers("Set-Cookie") should not include "token"
        }
        case _ => fail("the response from the SsoIn controller was not of the expected format")
      }
    }

  }

  private def sessionEntry(key: String, unencryptedValue: String): String = {
    val encryptedValue = URLEncoder.encode(encrypt(unencryptedValue), "UTF8")
    s"$key=$encryptedValue"
  }

}
