package controllers

import org.scalatest.mock.MockitoSugar
import play.api.mvc.Controller
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import org.mockito.Mockito._
import play.api.test.{FakeRequest, WithApplication}
import play.api.test.Helpers._
import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import java.net.URI
import uk.gov.hmrc.common.BaseSpec
import controllers.common._
import org.scalatest.TestData
import java.util.UUID
import uk.gov.hmrc.common.microservice.agent.{AgentConnectorRoot, AgentRegime}
import controllers.common.SessionTimeoutWrapper._
import uk.gov.hmrc.utils.DateTimeUtils.now
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import controllers.common.service.Connectors._
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import scala.Some
import uk.gov.hmrc.common.microservice.agent.AgentRoot
import uk.gov.hmrc.common.microservice.auth.domain.Regimes
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import play.api.test.FakeApplication
import controllers.common.actions.{HeaderCarrier, Actions}

class AuthorisedForActionSpec extends BaseSpec with MockitoSugar with CookieEncryption {

  val mockAuthConnector = mock[AuthConnector]
  val mockPayeConnector = mock[PayeConnector]
  val mockAgentMicroService = mock[AgentConnectorRoot]

  val testController = new TestController(mockPayeConnector, mockAgentMicroService, null)(mockAuthConnector)

  override protected def beforeEach(testData: TestData) {
    reset(mockAuthConnector, mockPayeConnector, mockAgentMicroService)

    //FIXME: mocking expectation should not be done in the before callback EVER!
    // It makes refactoring so much harder later on. Move this out and defined for each that requires it
    when(mockPayeConnector.root("/paye/AB123456C")).thenReturn(
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
    when(mockAuthConnector.authority("/auth/oid/jdensmore")).thenReturn(
      Some(UserAuthority("/auth/oid/jfisher", Regimes(paye = Some(URI.create("/paye/AB123456C"))), None)))
  }

  "basic homepage test" should {
    "contain the users first name in the response" ignore new WithApplication(FakeApplication()) {
      val result = testController.testPayeAuthorisation(FakeRequest().withSession(
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
      when(mockAuthConnector.authority("/auth/oid/jdensmore")).thenReturn(None)

      val result = testController.testPayeAuthorisation(FakeRequest().withSession(
        "sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"),
        lastRequestTimestampKey -> now.getMillis.toString,
        "userId" -> encrypt("/auth/oid/jdensmore"))
      )

      status(result) should equal(401)
    }

    "return internal server error page if the Action throws an exception" ignore new WithApplication(FakeApplication()) {
      val result = testController.testThrowsException(FakeRequest().withSession(
        "sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"),
        lastRequestTimestampKey -> now.getMillis.toString,
        "userId" -> encrypt("/auth/oid/jdensmore"))
      )

      status(result) should equal(500)
      contentAsString(result) should include("java.lang.RuntimeException")
    }

    "return internal server error page if the AuthConnector throws an exception" ignore new WithApplication(FakeApplication()) {
      when(mockAuthConnector.authority("/auth/oid/jdensmore")).thenThrow(new RuntimeException("TEST"))

      val result = testController.testPayeAuthorisation(FakeRequest().withSession(
        "sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"),
        lastRequestTimestampKey -> now.getMillis.toString,
        "userId" -> encrypt("/auth/oid/jdensmore"))
      )

      status(result) should equal(500)
      contentAsString(result) should include("java.lang.RuntimeException")
    }

    "return 200 in case the agent is successfully authorised" ignore new WithApplication(FakeApplication()) {
      when(mockAuthConnector.authority("/auth/oid/goeff")).thenReturn(
        Some(UserAuthority("/auth/oid/goeff", Regimes(agent = Some(URI.create("/agent/uar-for-goeff"))))))

      val agent = AgentRoot("uar-for-goeff", Map.empty, Map.empty)
      val request = FakeRequest().withSession(
        "sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"),
        lastRequestTimestampKey -> now.getMillis.toString,
        "userId" -> encrypt("/auth/oid/goeff"))
      when(mockAgentMicroService.root("/agent/uar-for-goeff")(HeaderCarrier(request))).thenReturn(agent)
      val result = testController.testAgentAuthorisation(request)

      status(result) should equal(200)
      contentAsString(result) shouldBe "uar-for-goeff"
    }

    "redirect to the Tax Regime landing page if the user is logged in but not authorised for the requested Tax Regime" ignore new WithApplication(FakeApplication()) {
      when(mockAuthConnector.authority("/auth/oid/john")).thenReturn(
        Some(UserAuthority("/auth/oid/john", Regimes(paye = None, sa = Some(URI.create("/sa/individual/12345678"))))))

      val result = testController.testPayeAuthorisation(FakeRequest().withSession(
        "sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"),
        lastRequestTimestampKey -> now.getMillis.toString,
        "userId" -> encrypt("/auth/oid/john"))
      )

      status(result) should equal(303)
      redirectLocation(result).get shouldBe "/login"
    }

    "redirect to the Tax Regime landing page if the agent is logged in but not authorised for the requested Tax Regime" ignore new WithApplication(FakeApplication()) {
      when(mockAuthConnector.authority("/auth/oid/john")).thenReturn(
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
      val result = testController.testPayeAuthorisation(FakeRequest().withSession(
        lastRequestTimestampKey -> now.getMillis.toString
      ))

      status(result) should equal(303)
      redirectLocation(result).get shouldBe "/samllogin"
    }

    "redirect to the login page when the userId is found but a gateway token is present" in new WithApplication(FakeApplication()) {
      val result = testController.testPayeAuthorisation(FakeRequest().withSession(
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


sealed class TestController(payeConnector: PayeConnector,
                            agentConnectorRoot: AgentConnectorRoot,
                            override val auditConnector: AuditConnector)
                           (implicit override val authConnector: AuthConnector)
  extends Controller
  with Actions
  with HeaderNames {


  override def regimeRoots(authority: UserAuthority)(implicit hc: HeaderCarrier): RegimeRoots = {
    val regimes = authority.regimes
    RegimeRoots(
      paye = regimes.paye map {
        uri => payeConnector.root(uri.toString)
      },
      agent = regimes.agent.map {
        uri => agentConnectorRoot.root(uri.toString)
      }
    )
  }

  def testPayeAuthorisation = AuthorisedFor(PayeRegime) {
    implicit user =>
      implicit request =>
        val userPayeRegimeRoot = user.regimes.paye.get
        val userName = userPayeRegimeRoot.name
        Ok(userName)
  }

  def testAgentAuthorisation = AuthorisedFor(AgentRegime) {
    implicit user =>
      implicit request =>
        val userAgentRegimeRoot = user.regimes.agent.get
        Ok(userAgentRegimeRoot.uar)
  }

  def testAuthorisationWithRedirectCommand = AuthenticatedBy(authenticationProvider = Ida, redirectToOrigin = true) {
    implicit user =>
      implicit request =>
        val userPayeRegimeRoot = user.regimes.paye.get
        val userName = userPayeRegimeRoot.name
        Ok(userName)
  }

  def testThrowsException = AuthorisedFor(PayeRegime) {
    implicit user =>
      implicit request =>
        throw new RuntimeException("ACTION TEST")
  }
}