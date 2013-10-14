package controllers

import org.scalatest.mock.MockitoSugar
import play.api.mvc.Controller
import uk.gov.hmrc.common.microservice.auth.AuthMicroService
import uk.gov.hmrc.common.microservice.paye.PayeMicroService
import org.mockito.Mockito.when
import org.mockito.Mockito.reset
import uk.gov.hmrc.common.microservice.auth.domain.{ Regimes, UserAuthority }
import play.api.test.{ FakeRequest, FakeApplication, WithApplication }
import play.api.test.Helpers._
import uk.gov.hmrc.common.microservice.paye.domain.{ PayeRegime, PayeRoot }
import java.net.URI
import org.slf4j.MDC
import uk.gov.hmrc.common.BaseSpec
import uk.gov.hmrc.common.microservice.MockMicroServicesForTests
import controllers.common._
import org.scalatest.TestData
import java.util.UUID
import uk.gov.hmrc.common.microservice.agent.{AgentRoot, AgentMicroService, AgentRegime}
import uk.gov.hmrc.domain.Uar

class AuthorisedForActionSpec extends BaseSpec with MockitoSugar with CookieEncryption {

  private lazy val mockAuthMicroService = mock[AuthMicroService]
  private lazy val mockPayeMicroService = mock[PayeMicroService]
  private lazy val mockAgentMicroService = mock[AgentMicroService]

  override protected def beforeEach(testData: TestData) {
    reset(mockAuthMicroService)
    reset(mockPayeMicroService)
    reset(mockAgentMicroService)
    when(mockPayeMicroService.root("/paye/AB123456C")).thenReturn(
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
        transactionLinks = Map.empty,
        actions = Map("addBenefit" -> "/paye/AB123456C/benefits/{year}/{employment}/add")
      )
    )
    when(mockAuthMicroService.authority("/auth/oid/jdensmore")).thenReturn(
      Some(UserAuthority("/auth/oid/jfisher", Regimes(paye = Some(URI.create("/paye/AB123456C"))), None)))
  }

  object TestController extends Controller with ActionWrappers with MockMicroServicesForTests with HeaderNames {

    override lazy val authMicroService = mockAuthMicroService
    override lazy val payeMicroService = mockPayeMicroService
    override lazy val agentMicroService = mockAgentMicroService

    def test = AuthorisedForIdaAction(Some(PayeRegime)) {
      implicit user =>
        implicit request =>
          val userPayeRegimeRoot = user.regimes.paye.get.get
          val userName = userPayeRegimeRoot.name
          Ok(userName)
    }

    def testAuthorisation = AuthorisedForIdaAction(Some(PayeRegime)) {
      implicit user =>
        implicit request =>
          val userPayeRegimeRoot = user.regimes.paye.get.get
          val userName = userPayeRegimeRoot.name
          Ok(userName)
    }

    def testAgentAuthorisation = AuthorisedForIdaAction(Some(AgentRegime)) {
      implicit user =>
        implicit request =>
          val userAgentRegimeRoot = user.regimes.agent.get.get
          Ok(userAgentRegimeRoot.uar.uar)
    }

    def testAuthorisationWithRedirectCommand = AuthorisedForIdaAction(redirectToOrigin = true) {
      implicit user =>
        implicit request =>
          val userPayeRegimeRoot = user.regimes.paye.get.get
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
      val result = TestController.test(FakeRequest().withSession(("sessionId", encrypt(s"session-${UUID.randomUUID().toString}")),("userId", encrypt("/auth/oid/jdensmore"))))

      status(result) should equal(200)
      contentAsString(result) should include("John Densmore")
    }
  }

  "AuthorisedForIdaAction" should {
    "return Unauthorised if no Authority is returned from the Auth service" in new WithApplication(FakeApplication()) {
      when(mockAuthMicroService.authority("/auth/oid/jdensmore")).thenReturn(None)

      val result = TestController.test(FakeRequest().withSession(("sessionId", encrypt(s"session-${UUID.randomUUID().toString}")), ("userId", encrypt("/auth/oid/jdensmore"))))
      status(result) should equal(401)
    }

    "return internal server error page if the Action throws an exception" in new WithApplication(FakeApplication()) {
      val result = TestController.testThrowsException(FakeRequest().withSession(("sessionId", encrypt(s"session-${UUID.randomUUID().toString}")), ("userId", encrypt("/auth/oid/jdensmore"))))
      status(result) should equal(500)
      contentAsString(result) should include("java.lang.RuntimeException")
    }

    "return internal server error page if the AuthMicroService throws an exception" in new WithApplication(FakeApplication()) {
      when(mockAuthMicroService.authority("/auth/oid/jdensmore")).thenThrow(new RuntimeException("TEST"))

      val result = TestController.test(FakeRequest().withSession(("sessionId", encrypt(s"session-${UUID.randomUUID().toString}")), ("userId", encrypt("/auth/oid/jdensmore"))))
      status(result) should equal(500)
      contentAsString(result) should include("java.lang.RuntimeException")
    }

    "include the authorisation and request ids in the MDC" in new WithApplication(FakeApplication()) {
      val result = TestController.testMdc(FakeRequest().withSession(("sessionId", encrypt(s"session-${UUID.randomUUID().toString}")), ("userId", encrypt("/auth/oid/jdensmore"))))
      status(result) should equal(200)
      val strings = contentAsString(result).split(" ")
      strings(0) should equal("/auth/oid/jdensmore")
      strings(1) should startWith("govuk-tax-")
    }


    "return 200 in case the agent is successfuly authorised" in new WithApplication(FakeApplication()) {


      when(mockAuthMicroService.authority("/auth/oid/goeff")).thenReturn(
        Some(UserAuthority("/auth/oid/goeff", Regimes(agent = Some(URI.create("/agent/uar-for-goeff"))), None)))

      val agent = mock[AgentRoot]
      when(agent.uar).thenReturn(Uar("uar-for-goeff"))
      when(mockAgentMicroService.root("/agent/uar-for-goeff")).thenReturn(agent)
      val result = TestController.testAgentAuthorisation(FakeRequest().withSession(("sessionId", encrypt(s"session-${UUID.randomUUID().toString}")), ("userId", encrypt("/auth/oid/goeff"))))
      status(result) should equal(200)
      contentAsString(result) shouldBe "uar-for-goeff"
    }

    "redirect to the Tax Regime landing page if the user is logged in but not authorised for the requested Tax Regime" in new WithApplication(FakeApplication()) {
      when(mockAuthMicroService.authority("/auth/oid/john")).thenReturn(
        Some(UserAuthority("/auth/oid/john", Regimes(paye = None, sa = Some(URI.create("/sa/individual/12345678"))), None)))
      val result = TestController.testAuthorisation(FakeRequest().withSession(("sessionId", encrypt(s"session-${UUID.randomUUID().toString}")), "userId" -> encrypt("/auth/oid/john")))
      status(result) should equal(303)
      redirectLocation(result).get shouldBe "/login"
    }

    "redirect to the Tax Regime landing page if the agent is logged in but not authorised for the requested Tax Regime" in new WithApplication(FakeApplication()) {
      when(mockAuthMicroService.authority("/auth/oid/john")).thenReturn(
        Some(UserAuthority("/auth/oid/john", Regimes(paye = None, sa = Some(URI.create("/sa/individual/12345678"))), None)))
      val result = TestController.testAgentAuthorisation(FakeRequest().withSession(("sessionId", encrypt(s"session-${UUID.randomUUID().toString}")), "userId" -> encrypt("/auth/oid/john")))
      status(result) should equal(303)
      redirectLocation(result).get shouldBe "/login"
    }

    "redirect to the login page when the userId is not found in the session " in new WithApplication(FakeApplication()) {
      val result = TestController.testAuthorisation(FakeRequest())
      status(result) should equal(303)
      redirectLocation(result).get shouldBe "/samllogin"
    }

    "redirect to the login page when the userId is found but a gateway token is present" in new WithApplication(FakeApplication()) {
      val result = TestController.testAuthorisation(FakeRequest().withSession("sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"), "userId" -> encrypt("/auth/oid/john"), "token" -> encrypt("a-government-gateway-token")))
      status(result) should equal(303)
      redirectLocation(result).get shouldBe "/samllogin"
    }

    "add redirect information to the session when required" in new WithApplication(FakeApplication()) {
      val result = TestController.testAuthorisationWithRedirectCommand(
        FakeRequest("GET", "/some/path")
          .withSession("sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"), "userId" -> encrypt("/auth/oid/john"), "token" -> encrypt("a-government-gateway-token"))
      )
      status(result) should equal(303)
      session(result).get(FrontEndRedirect.redirectSessionKey) shouldBe Some("/some/path")
    }

  }

}
