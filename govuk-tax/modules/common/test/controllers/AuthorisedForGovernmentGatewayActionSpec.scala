package controllers

import org.scalatest.mock.MockitoSugar
import play.api.mvc.Controller
import uk.gov.hmrc.common.microservice.auth.AuthMicroService
import org.mockito.Mockito.when
import org.mockito.Mockito.reset
import uk.gov.hmrc.common.microservice.auth.domain.{ Regimes, UserAuthority }
import play.api.test.{ FakeRequest, FakeApplication, WithApplication }
import play.api.test.Helpers._
import java.net.URI
import org.slf4j.MDC
import uk.gov.hmrc.common.microservice.sa.domain.{ SaRoot, SaRegime }
import uk.gov.hmrc.common.microservice.sa.SaMicroService
import uk.gov.hmrc.common.BaseSpec
import uk.gov.hmrc.common.microservice.MockMicroServicesForTests
import controllers.common._
import org.scalatest.TestData
import java.util.UUID

class AuthorisedForGovernmentGatewayActionSpec extends BaseSpec with MockitoSugar with CookieEncryption {

  private lazy val mockAuthMicroService = mock[AuthMicroService]
  private lazy val mockSaMicroService = mock[SaMicroService]
  private val token = "someToken"

  override protected def beforeEach(testData: TestData) {
    reset(mockAuthMicroService)
    reset(mockSaMicroService)
    when(mockSaMicroService.root("/sa/detail/AB123456C")).thenReturn(
      SaRoot("someUtr", Map("link1" -> "http://somelink/1"))
    )
    when(mockAuthMicroService.authority("/auth/oid/gfisher")).thenReturn(
      Some(UserAuthority("/auth/oid/gfisher", Regimes(sa = Some(URI.create("/sa/detail/AB123456C"))), None)))
  }

  object TestController extends Controller with ActionWrappers with MockMicroServicesForTests with HeaderNames {

    override lazy val authMicroService = mockAuthMicroService
    override lazy val saMicroService = mockSaMicroService

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
    "contain the user's first name in the response" in new WithApplication(FakeApplication()) {
      val result = TestController.test(FakeRequest().withSession("sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"), "userId" -> encrypt("/auth/oid/gfisher"), "token" -> encrypt(token)))

      status(result) should equal(200)
      contentAsString(result) should include("someUtr")
    }
  }

  "AuthorisedForIdaAction" should {
    "return Unauthorised if no Authority is returned from the Auth service" in new WithApplication(FakeApplication()) {
      when(mockAuthMicroService.authority("/auth/oid/gfisher")).thenReturn(None)

      val result = TestController.test(FakeRequest().withSession("sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"), "userId" -> encrypt("/auth/oid/gfisher"), "token" -> encrypt(token)))
      status(result) should equal(401)
    }

    "return internal server error page if the Action throws an exception" in new WithApplication(FakeApplication()) {
      val result = TestController.testThrowsException(FakeRequest().withSession("sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"), "userId" -> encrypt("/auth/oid/gfisher"), "token" -> encrypt(token)))
      status(result) should equal(500)
      contentAsString(result) should include("java.lang.RuntimeException")
    }

    "return internal server error page if the AuthMicroService throws an exception" in new WithApplication(FakeApplication()) {
      when(mockAuthMicroService.authority("/auth/oid/gfisher")).thenThrow(new RuntimeException("TEST"))

      val result = TestController.test(FakeRequest().withSession("sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"), "userId" -> encrypt("/auth/oid/gfisher"), "token" -> encrypt(token)))
      status(result) should equal(500)
      contentAsString(result) should include("java.lang.RuntimeException")
    }

    "include the authorisation and request ids in the MDC" in new WithApplication(FakeApplication()) {
      val result = TestController.testMdc(FakeRequest().withSession("sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"), "userId" -> encrypt("/auth/oid/gfisher"), "token" -> encrypt(token)))
      status(result) should equal(200)
      val strings = contentAsString(result).split(" ")
      strings(0) should equal("/auth/oid/gfisher")
      strings(1) should startWith("govuk-tax-")
    }

    "redirect to the Tax Regime landing page if the user is logged in but not authorised for the requested Tax Regime" in new WithApplication(FakeApplication()) {
      when(mockAuthMicroService.authority("/auth/oid/bob")).thenReturn(
        Some(UserAuthority("bob", Regimes(sa = None, paye = Some(URI.create("/personal/paye/12345678"))), None)))
      val result = TestController.testAuthorisation(FakeRequest().withSession("sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"), "userId" -> encrypt("/auth/oid/bob"), "token" -> encrypt(token)))
      status(result) should equal(303)
    }

    "redirect to the login page when the userId is not found in the session " in new WithApplication(FakeApplication()) {
      val result = TestController.testAuthorisation(FakeRequest())
      status(result) should equal(303)
      redirectLocation(result).get shouldBe "/"
    }
  }
}
