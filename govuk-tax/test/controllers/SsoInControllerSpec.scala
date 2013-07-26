package controllers

import test.BaseSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import play.api.test.{ FakeRequest, WithApplication, FakeApplication }
import microservice.MockMicroServicesForTests
import microservice.governmentgateway.{ GovernmentGatewayResponse, ValidateTokenRequest, GovernmentGatewayMicroService }
import org.mockito.Mockito._
import play.api.mvc.{ SimpleResult, Result }

class SsoInControllerSpec extends BaseSpec with ShouldMatchers with MockitoSugar with CookieEncryption {

  private val mockGovernmentGatewayService = mock[GovernmentGatewayMicroService]
  val redirectUrl = "www.redirect-url.co.uk"

  private def controller = new SsoInController with MockMicroServicesForTests {
    override val governmentGatewayMicroService = mockGovernmentGatewayService
  }

  "The Single Sign-on input page" should {
    "create a new session when the token is valid, the time not expired and no session exists" in new WithApplication(FakeApplication()) {
      val token: String = "token1"
      val userName = "john"
      when(mockGovernmentGatewayService.validateToken(ValidateTokenRequest(token, "2013-07-12"))).thenReturn(GovernmentGatewayResponse("http://authId", userName))

      val result: Result = controller.in(FakeRequest("POST", s"www.governmentgateway.com?dest=$redirectUrl").withFormUrlEncodedBody("gw" -> token, "time" -> "2013-07-12"))
      result match {
        case SimpleResult(header, _) => {
          header.status shouldBe 303
          header.headers("Location") shouldBe redirectUrl
          header.headers("Set-Cookie") should include("userId")
          header.headers("Set-Cookie") should include regex "nameFromGovernmentGateway.*john".r
        }
        case _ => fail("the response from the SsoIn controller was not of the expected format")
      }

    }

    "leave the session if one exists and the login is correct" in new WithApplication(FakeApplication()) {
      val userName = "johnny"
      val token: String = "token2"
      when(mockGovernmentGatewayService.validateToken(ValidateTokenRequest(token, "2013-07-12"))).thenReturn(GovernmentGatewayResponse("http://authId", userName))

      val result: Result = controller.in(FakeRequest("POST", s"www.governmentgateway.com?dest=$redirectUrl").withFormUrlEncodedBody("gw" -> token, "time" -> "2013-07-12").withSession("userId" -> "john", "nameFromGovernmentGateway" -> "john"))
      result match {
        case SimpleResult(header, _) => {
          header.status shouldBe 303
          header.headers("Location") shouldBe redirectUrl
          header.headers("Set-Cookie") should include("userId")
          header.headers("Set-Cookie") should include regex "nameFromGovernmentGateway.*john".r
        }
        case _ => fail("the response from the SsoIn controller was not of the expected format")
      }

    }

    "invalidate the session if a session exists but the login is incorrect" in new WithApplication(FakeApplication()) {
      val userName = "johnboy"
      val token: String = "token3"
      when(mockGovernmentGatewayService.validateToken(ValidateTokenRequest(token, "2013-07-12"))).thenThrow(new IllegalStateException("error"))

      val result: Result = controller.in(FakeRequest("POST", s"www.governmentgateway.com?dest=$redirectUrl").withFormUrlEncodedBody("gw" -> token, "time" -> "2013-07-12").withSession("userId" -> "john", "nameFromGovernmentGateway" -> "john"))
      result match {
        case SimpleResult(header, _) => {
          header.status shouldBe 303
          header.headers("Location") shouldBe redirectUrl
          header.headers("Set-Cookie") should not include "userId"
          header.headers("Set-Cookie") should not include "john"
        }
        case _ => fail("the response from the SsoIn controller was not of the expected format")
      }

    }

  }

}
