package controllers

import test.BaseSpec
import org.scalatest.mock.MockitoSugar
import org.scalatest.matchers.ShouldMatchers
import play.api.mvc.Controller
import microservice.auth.AuthMicroService
import org.mockito.Mockito.when
import org.mockito.Mockito.reset
import microservice.auth.domain.{ Regimes, UserAuthority }
import play.api.test.{ FakeRequest, FakeApplication, WithApplication }
import play.api.test.Helpers._
import microservice.MockMicroServicesForTests
import java.net.URI
import org.slf4j.MDC
import org.scalatest.BeforeAndAfterEach
import microservice.sa.domain.{ SaRoot, SaRegime }
import microservice.sa.SaMicroService
import config.CookieSupport
import java.security.GeneralSecurityException

class AuthorisedForGovernmentGatewayActionSpec extends BaseSpec with ShouldMatchers with MockitoSugar with CookieSupport with BeforeAndAfterEach {

  private val mockAuthMicroService = mock[AuthMicroService]
  private val mockSaMicroService = mock[SaMicroService]
  private val token = "someToken"

  override def beforeEach() {
    reset(mockAuthMicroService)
    reset(mockSaMicroService)
    when(mockSaMicroService.root("/sa/detail/AB123456C")).thenReturn(
      SaRoot("someUtr", Map("link1" -> "http://somelink/1"))
    )
    when(mockAuthMicroService.authority("/auth/oid/gfisher")).thenReturn(
      Some(UserAuthority("/auth/oid/gfisher", Regimes(sa = Some(URI.create("/sa/detail/AB123456C"))), None)))
  }

  object TestController extends Controller with ActionWrappers with MockMicroServicesForTests with HeaderNames {

    override val authMicroService = mockAuthMicroService
    override val saMicroService = mockSaMicroService

    def test = AuthorisedForGovernmentGatewayAction(Some(SaRegime)) {
      implicit user =>
        implicit request =>
          val saUtr = user.regimes.sa.get.utr
          Ok(saUtr)
    }

    def testAuthorisation = AuthorisedForGovernmentGatewayAction(Some(SaRegime)) {
      implicit user =>
        implicit request =>
          val saUtr = user.regimes.sa.get.utr
          Ok(saUtr)
    }

    def testThrowsException = AuthorisedForGovernmentGatewayAction(Some(SaRegime)) {
      implicit user =>
        implicit request =>
          throw new RuntimeException("ACTION TEST")
    }

    def testMdc = AuthorisedForGovernmentGatewayAction(Some(SaRegime)) {
      implicit user =>
        implicit request =>
          Ok(s"${MDC.get(authorisation)} ${MDC.get(requestId)}")
    }
  }

  "basic homepage test" should {
    "contain the first name of the user in the response" in new WithApplication(FakeApplication()) {
      val result = TestController.test(FakeRequest().withSession("userId" -> encrypt("/auth/oid/gfisher"), "token" -> encrypt(token)).withCookies(validTimestampCookie))

      status(result) should equal(200)
      contentAsString(result) should include("someUtr")
    }
  }

  "AuthorisedForIdaAction" should {
    "return Unauthorised if no Authority is returned from the Auth service" in new WithApplication(FakeApplication()) {
      when(mockAuthMicroService.authority("/auth/oid/gfisher")).thenReturn(None)

      val result = TestController.test(FakeRequest().withSession("userId" -> encrypt("/auth/oid/gfisher"), "token" -> encrypt(token)).withCookies(validTimestampCookie))
      status(result) should equal(401)
    }

    "return internal server error page if the Action throws an exception" in new WithApplication(FakeApplication()) {
      val result = TestController.testThrowsException(FakeRequest().withSession("userId" -> encrypt("/auth/oid/gfisher"), "token" -> encrypt(token)).withCookies(validTimestampCookie))
      status(result) should equal(500)
      contentAsString(result) should include("java.lang.RuntimeException")
    }

    "return internal server error page if the AuthMicroService throws an exception" in new WithApplication(FakeApplication()) {
      when(mockAuthMicroService.authority("/auth/oid/gfisher")).thenThrow(new RuntimeException("TEST"))

      val result = TestController.test(FakeRequest().withSession("userId" -> encrypt("/auth/oid/gfisher"), "token" -> encrypt(token)).withCookies(validTimestampCookie))
      status(result) should equal(500)
      contentAsString(result) should include("java.lang.RuntimeException")
    }

    "include the authorisation and request ids in the MDC" in new WithApplication(FakeApplication()) {
      val result = TestController.testMdc(FakeRequest().withSession("userId" -> encrypt("/auth/oid/gfisher"), "token" -> encrypt(token)).withCookies(validTimestampCookie))
      status(result) should equal(200)
      val strings = contentAsString(result).split(" ")
      strings(0) should equal("/auth/oid/gfisher")
      strings(1) should startWith("frontend-")
    }

    "redirect to the Tax Regime landing page if the user is logged in but not authorised for the requested Tax Regime" in new WithApplication(FakeApplication()) {
      when(mockAuthMicroService.authority("/auth/oid/bob")).thenReturn(
        Some(UserAuthority("/auth/oid/bob", Regimes(sa = None, paye = Some(URI.create("/personal/paye/12345678"))), None)))
      val result = TestController.testAuthorisation(FakeRequest().withSession("userId" -> encrypt("/auth/oid/bob"), "token" -> encrypt(token)).withCookies(validTimestampCookie))
      status(result) should equal(303)
    }

    "redirect to the login page when the userId is not found in the session " in new WithApplication(FakeApplication()) {
      val result = TestController.testAuthorisation(FakeRequest().withCookies(validTimestampCookie))
      status(result) should equal(303)
      redirectLocation(result).get mustBe "/"
    }

    "redirect to the login page when the last request timestamp cookie is not present" in new WithApplication(FakeApplication()) {
      val result = TestController.test(FakeRequest().withSession("userId" -> encrypt("/auth/oid/gfisher"), "token" -> encrypt(token)))
      status(result) should equal(303)
      redirectLocation(result).get mustBe "/"
    }

    "redirect to the login page when the last request timestamp cookie is present, but has expired" in new WithApplication(FakeApplication()) {
      val result = TestController.test(FakeRequest().withSession("userId" -> encrypt("/auth/oid/gfisher"), "token" -> encrypt(token)).withCookies(expiredTimestampCookie))
      status(result) should equal(303)
      redirectLocation(result).get mustBe "/"
    }

    "throw a security exception if the last timestamp cookie is present, but can't be decrypted" in new WithApplication(FakeApplication()) {
      intercept[GeneralSecurityException] {
        TestController.test(FakeRequest().withSession("userId" -> encrypt("/auth/oid/gfisher"), "token" -> encrypt(token)).withCookies(brokenTimestampCookie))
      }
    }
  }
}
