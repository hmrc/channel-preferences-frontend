package controllers

import org.scalatest.mock.MockitoSugar
import play.api.mvc.Controller
import uk.gov.hmrc.common.microservice.auth.AuthMicroService
import uk.gov.hmrc.common.microservice.paye.PayeMicroService
import org.mockito.Mockito.when
import play.api.test.{FakeRequest, WithApplication}
import play.api.test.Helpers._
import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import java.net.URI
import org.slf4j.MDC
import uk.gov.hmrc.common.{MockUtils, BaseSpec}
import controllers.common._
import org.scalatest.TestData
import java.util.UUID
import uk.gov.hmrc.common.microservice.agent.{AgentMicroServiceRoot, AgentRegime}
import controllers.common.SessionTimeoutWrapper._
import uk.gov.hmrc.utils.DateTimeUtils.now
import uk.gov.hmrc.common.microservice.audit.AuditMicroService
import controllers.common.service.MicroServices._
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import scala.Some
import uk.gov.hmrc.common.microservice.agent.AgentRoot
import uk.gov.hmrc.common.microservice.auth.domain.Regimes
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import play.api.test.FakeApplication

class AuthorisedForActionSpec extends BaseSpec with MockitoSugar with CookieEncryption {

  val mockAuthMicroService = mock[AuthMicroService]
  val mockPayeMicroService = mock[PayeMicroService]
  val mockAgentMicroService = mock[AgentMicroServiceRoot]

  val testController = new TestController(mockPayeMicroService, mockAgentMicroService, null)(mockAuthMicroService)

  override protected def beforeEach(testData: TestData) {
    MockUtils.resetAll(mockAuthMicroService, mockPayeMicroService, mockAgentMicroService)

    //FIXME: mocking expectation should not be done in the before callback EVER!
    // It makes refactoring so much harder later on. Move this out and defined for each that requires it
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
        actions = Map("calculateBenefitValue" -> "/calculation/paye/benefit/new/value-calculation")
      )
    )
    when(mockAuthMicroService.authority("/auth/oid/jdensmore")).thenReturn(
      Some(UserAuthority("/auth/oid/jfisher", Regimes(paye = Some(URI.create("/paye/AB123456C"))), None)))
  }

  "basic homepage test" should {
    "contain the user's first name in the response" in new WithApplication(FakeApplication()) {
      val result = testController.test(FakeRequest().withSession(
        "sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"),
        lastRequestTimestampKey -> now.getMillis.toString,
        "userId" -> encrypt("/auth/oid/jdensmore"))
      )

      status(result) should equal(200)
      contentAsString(result) should include("John Densmore")
    }
  }

  "AuthorisedForIdaAction" should {
    "return Unauthorised if no Authority is returned from the Auth service" in new WithApplication(FakeApplication()) {
      when(mockAuthMicroService.authority("/auth/oid/jdensmore")).thenReturn(None)

      val result = testController.test(FakeRequest().withSession(
        "sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"),
        lastRequestTimestampKey -> now.getMillis.toString,
        "userId" -> encrypt("/auth/oid/jdensmore"))
      )

      status(result) should equal(401)
    }

    "return internal server error page if the Action throws an exception" in new WithApplication(FakeApplication()) {
      val result = testController.testThrowsException(FakeRequest().withSession(
        "sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"),
        lastRequestTimestampKey -> now.getMillis.toString,
        "userId" -> encrypt("/auth/oid/jdensmore"))
      )

      status(result) should equal(500)
      contentAsString(result) should include("java.lang.RuntimeException")
    }

    "return internal server error page if the AuthMicroService throws an exception" in new WithApplication(FakeApplication()) {
      when(mockAuthMicroService.authority("/auth/oid/jdensmore")).thenThrow(new RuntimeException("TEST"))

      val result = testController.test(FakeRequest().withSession(
        "sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"),
        lastRequestTimestampKey -> now.getMillis.toString,
        "userId" -> encrypt("/auth/oid/jdensmore"))
      )

      status(result) should equal(500)
      contentAsString(result) should include("java.lang.RuntimeException")
    }

    "include the authorisation and request ids in the MDC" in new WithApplication(FakeApplication()) {
      val result = testController.testMdc(FakeRequest().withSession(
        "sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"),
        lastRequestTimestampKey -> now.getMillis.toString,
        "userId" -> encrypt("/auth/oid/jdensmore"))
      )

      status(result) should equal(200)
      val strings = contentAsString(result).split(" ")
      strings(0) should equal("/auth/oid/jdensmore")
      strings(1) should startWith("govuk-tax-")
    }


    "return 200 in case the agent is successfully authorised" in new WithApplication(FakeApplication()) {

      when(mockAuthMicroService.authority("/auth/oid/goeff")).thenReturn(
        Some(UserAuthority("/auth/oid/goeff", Regimes(agent = Some(URI.create("/agent/uar-for-goeff"))), None)))

      val agent = AgentRoot("uar-for-goeff", Map.empty, Map.empty)
      when(mockAgentMicroService.root("/agent/uar-for-goeff")).thenReturn(agent)

      val result = testController.testAgentAuthorisation(FakeRequest().withSession(
        "sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"),
        lastRequestTimestampKey -> now.getMillis.toString,
        "userId" -> encrypt("/auth/oid/goeff"))
      )

      status(result) should equal(200)
      contentAsString(result) shouldBe "uar-for-goeff"
    }

    "redirect to the Tax Regime landing page if the user is logged in but not authorised for the requested Tax Regime" in new WithApplication(FakeApplication()) {
      when(mockAuthMicroService.authority("/auth/oid/john")).thenReturn(
        Some(UserAuthority("/auth/oid/john", Regimes(paye = None, sa = Some(URI.create("/sa/individual/12345678"))), None)))
      val result = testController.testAuthorisation(FakeRequest().withSession(
        "sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"),
        lastRequestTimestampKey -> now.getMillis.toString,
        "userId" -> encrypt("/auth/oid/john"))
      )

      status(result) should equal(303)
      redirectLocation(result).get shouldBe "/login"
    }

    "redirect to the Tax Regime landing page if the agent is logged in but not authorised for the requested Tax Regime" in new WithApplication(FakeApplication()) {
      when(mockAuthMicroService.authority("/auth/oid/john")).thenReturn(
        Some(UserAuthority("/auth/oid/john", Regimes(paye = None, sa = Some(URI.create("/sa/individual/12345678"))), None)))
      val result = testController.testAgentAuthorisation(FakeRequest().withSession(
        "sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"),
        lastRequestTimestampKey -> now.getMillis.toString,
        "userId" -> encrypt("/auth/oid/john"))
      )

      status(result) should equal(303)
      redirectLocation(result).get shouldBe "/login"
    }

    "redirect to the login page when the userId is not found in the session " in new WithApplication(FakeApplication()) {
      val result = testController.testAuthorisation(FakeRequest().withSession(
        lastRequestTimestampKey -> now.getMillis.toString
      ))

      status(result) should equal(303)
      redirectLocation(result).get shouldBe "/samllogin"
    }

    "redirect to the login page when the userId is found but a gateway token is present" in new WithApplication(FakeApplication()) {
      val result = testController.testAuthorisation(FakeRequest().withSession(
        "sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"),
        "userId" -> encrypt("/auth/oid/john"),
        lastRequestTimestampKey -> now.getMillis.toString,
        "token" -> encrypt("a-government-gateway-token")))


      status(result) should equal(303)
      redirectLocation(result).get shouldBe "/samllogin"
    }

    "add redirect information to the session when required" in new WithApplication(FakeApplication()) {
      val result = testController.testAuthorisationWithRedirectCommand(
        FakeRequest("GET", "/some/path").withSession(
          "sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"),
          "userId" -> encrypt("/auth/oid/john"),
          lastRequestTimestampKey -> now.getMillis.toString,
          "token" -> encrypt("a-government-gateway-token")
        )
      )
      status(result) should equal(303)
      session(result).get(FrontEndRedirect.redirectSessionKey) shouldBe Some("/some/path")
    }

  }

}


sealed class TestController(payeMicroService : PayeMicroService,
                            agentMicroServiceRoot : AgentMicroServiceRoot,
                            override val auditMicroService: AuditMicroService)
                           (implicit override val authMicroService: AuthMicroService)
  extends Controller
  with Actions
  with HeaderNames {


  override def regimeRoots(authority: UserAuthority): RegimeRoots = {
    val regimes = authority.regimes
    RegimeRoots(
      paye = regimes.paye map {
        uri => payeMicroService.root(uri.toString)
      },
      agent = regimes.agent.map {
        uri => agentMicroServiceRoot.root(uri.toString)
      }
    )
  }

  def test = ActionAuthorisedBy(Ida)(Some(PayeRegime)) {
    implicit user =>
      implicit request =>
        val userPayeRegimeRoot = user.regimes.paye.get
        val userName = userPayeRegimeRoot.name
        Ok(userName)
  }

  def testAuthorisation = ActionAuthorisedBy(Ida)(Some(PayeRegime)) {
    implicit user =>
      implicit request =>
        val userPayeRegimeRoot = user.regimes.paye.get
        val userName = userPayeRegimeRoot.name
        Ok(userName)
  }

  def testAgentAuthorisation = ActionAuthorisedBy(Ida)(Some(AgentRegime)) {
    implicit user =>
      implicit request =>
        val userAgentRegimeRoot = user.regimes.agent.get
        Ok(userAgentRegimeRoot.uar)
  }

  def testAuthorisationWithRedirectCommand = ActionAuthorisedBy(Ida)(redirectToOrigin = true) {
    implicit user =>
      implicit request =>
        val userPayeRegimeRoot = user.regimes.paye.get
        val userName = userPayeRegimeRoot.name
        Ok(userName)
  }

  def testThrowsException = ActionAuthorisedBy(Ida)(Some(PayeRegime)) {
    implicit user =>
      implicit request =>
        throw new RuntimeException("ACTION TEST")
  }

  def testMdc = ActionAuthorisedBy(Ida)(Some(PayeRegime)) {
    implicit user =>
      implicit request =>
        Ok(s"${MDC.get(authorisation)} ${MDC.get(requestId)}")
  }
}