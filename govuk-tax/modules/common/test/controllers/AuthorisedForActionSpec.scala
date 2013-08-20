package controllers

import org.scalatest.mock.MockitoSugar
import org.scalatest.matchers.ShouldMatchers
import play.api.mvc.Controller
import uk.gov.hmrc.microservice.auth.AuthMicroService
import uk.gov.hmrc.microservice.paye.PayeMicroService
import org.mockito.Mockito.when
import org.mockito.Mockito.reset
import uk.gov.hmrc.microservice.auth.domain.{ Regimes, UserAuthority }
import play.api.test.{ FakeRequest, FakeApplication, WithApplication }
import play.api.test.Helpers._
import uk.gov.hmrc.microservice.paye.domain.{ PayeRegime, PayeRoot }
import java.net.URI
import org.slf4j.MDC
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.common.BaseSpec
import uk.gov.hmrc.microservice.MockMicroServicesForTests
import controllers.common._

class AuthorisedForActionSpec extends BaseSpec with ShouldMatchers with MockitoSugar with CookieEncryption with BeforeAndAfterEach {

  private val mockAuthMicroService = mock[AuthMicroService]
  private val mockPayeMicroService = mock[PayeMicroService]

  override def beforeEach() {
    reset(mockAuthMicroService)
    reset(mockPayeMicroService)
    when(mockPayeMicroService.root("/personal/paye/AB123456C")).thenReturn(
      PayeRoot(
        name = "John Densmore",
        firstName = "John",
        secondName = Some("Drummer"),
        surname = "Densmore",
        version = 22,
        title = "Mr",
        nino = "AB123456C",
        dateOfBirth = "1976-01-02",
        links = Map.empty,
        transactionLinks = Map.empty
      )
    )
    when(mockAuthMicroService.authority("/auth/oid/jdensmore")).thenReturn(
      Some(UserAuthority("/auth/oid/jfisher", Regimes(paye = Some(URI.create("/personal/paye/AB123456C"))), None)))
  }

  object TestController extends Controller with ActionWrappers with MockMicroServicesForTests with HeaderNames {

    override val authMicroService = mockAuthMicroService
    override val payeMicroService = mockPayeMicroService

    def test = AuthorisedForIdaAction(Some(PayeRegime)) {
      implicit user =>
        implicit request =>
          val userPayeRegimeRoot = user.regimes.paye.get
          val userName = userPayeRegimeRoot.name
          Ok(userName)
    }

    def testAuthorisation = AuthorisedForIdaAction(Some(PayeRegime)) {
      implicit user =>
        implicit request =>
          val userPayeRegimeRoot = user.regimes.paye.get
          val userName = userPayeRegimeRoot.name
          Ok(userName)
    }

    def testThrowsException = AuthorisedForIdaAction(Some(PayeRegime)) {
      implicit user =>
        implicit request =>
          throw new RuntimeException("ACTION TEST")
    }

    def testMdc = AuthorisedForIdaAction(Some(PayeRegime)) {
      implicit user =>
        implicit request =>
          Ok(s"${MDC.get(authorisation)} ${MDC.get(requestId)}")
    }
  }

  "basic homepage test" should {
    "contain the user's first name in the response" in new WithApplication(FakeApplication()) {
      val result = TestController.test(FakeRequest().withSession(("userId", encrypt("/auth/oid/jdensmore"))))

      status(result) should equal(200)
      contentAsString(result) should include("John Densmore")
    }
  }

  "AuthorisedForIdaAction" should {
    "return Unauthorised if no Authority is returned from the Auth service" in new WithApplication(FakeApplication()) {
      when(mockAuthMicroService.authority("/auth/oid/jdensmore")).thenReturn(None)

      val result = TestController.test(FakeRequest().withSession(("userId", encrypt("/auth/oid/jdensmore"))))
      status(result) should equal(401)
    }

    "return internal server error page if the Action throws an exception" in new WithApplication(FakeApplication()) {
      val result = TestController.testThrowsException(FakeRequest().withSession(("userId", encrypt("/auth/oid/jdensmore"))))
      status(result) should equal(500)
      contentAsString(result) should include("java.lang.RuntimeException")
    }

    "return internal server error page if the AuthMicroService throws an exception" in new WithApplication(FakeApplication()) {
      when(mockAuthMicroService.authority("/auth/oid/jdensmore")).thenThrow(new RuntimeException("TEST"))

      val result = TestController.test(FakeRequest().withSession(("userId", encrypt("/auth/oid/jdensmore"))))
      status(result) should equal(500)
      contentAsString(result) should include("java.lang.RuntimeException")
    }

    "include the authorisation and request ids in the MDC" in new WithApplication(FakeApplication()) {
      val result = TestController.testMdc(FakeRequest().withSession(("userId", encrypt("/auth/oid/jdensmore"))))
      status(result) should equal(200)
      val strings = contentAsString(result).split(" ")
      strings(0) should equal("/auth/oid/jdensmore")
      strings(1) should startWith("frontend-")
    }

    "redirect to the Tax Regime landing page if the user is logged in but not authorised for the requested Tax Regime" in new WithApplication(FakeApplication()) {
      when(mockAuthMicroService.authority("/auth/oid/john")).thenReturn(
        Some(UserAuthority("/auth/oid/john", Regimes(paye = None, sa = Some(URI.create("/personal/sa/12345678"))), None)))
      val result = TestController.testAuthorisation(FakeRequest().withSession("userId" -> encrypt("/auth/oid/john")))
      status(result) should equal(303)
      redirectLocation(result).get mustBe "/login"
    }

    "redirect to the login page when the userId is not found in the session " in new WithApplication(FakeApplication()) {
      val result = TestController.testAuthorisation(FakeRequest())
      status(result) should equal(303)
      redirectLocation(result).get mustBe "/samllogin"
    }

    "redirect to the login page when the userId is found but a gateway token is present" in new WithApplication(FakeApplication()) {
      val result = TestController.testAuthorisation(FakeRequest().withSession("userId" -> encrypt("/auth/oid/john"), "token" -> encrypt("a-government-gateway-token")))
      status(result) should equal(303)
      redirectLocation(result).get mustBe "/samllogin"
    }

  }

}
