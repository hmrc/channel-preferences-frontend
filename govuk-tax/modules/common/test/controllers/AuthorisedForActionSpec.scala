package controllers

import org.scalatest.mock.MockitoSugar
import play.api.mvc.Controller
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import org.mockito.Mockito._
import play.api.test.{FakeRequest, WithApplication}
import play.api.test.Helpers._
import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import uk.gov.hmrc.common.{MdcLoggingExecutionContext, BaseSpec}
import controllers.common._
import org.scalatest.TestData
import org.mockito.Matchers
import java.util.UUID
import uk.gov.hmrc.utils.DateTimeUtils.now
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import controllers.common.service.Connectors._
import controllers.common.actions.{HeaderCarrier, Actions}
import scala.concurrent.Future
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.common.microservice.auth.domain.Authority
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import play.api.test.FakeApplication

class AuthorisedForActionSpec extends BaseSpec with MockitoSugar {

  import controllers.domain.AuthorityUtils._

  val mockAuthConnector = mock[AuthConnector]
  val mockPayeConnector = mock[PayeConnector]

  val testController = new TestController(mockPayeConnector, null)(mockAuthConnector)

  override protected def beforeEach(testData: TestData) {
    reset(mockAuthConnector, mockPayeConnector)

    //FIXME: mocking expectation should not be done in the before callback EVER!
    // It makes refactoring so much harder later on. Move this out and defined for each that requires it
    when(mockPayeConnector.root("/paye/AB123456C")).thenReturn(
      PayeRoot(
        name = "John Densmore",
        firstName = "John",
        secondName = Some("Drummer"),
        surname = "Densmore",
        title = "Mr",
        nino = "AB123456C",
        dateOfBirth = "1976-01-02",
        links = Map.empty,
        transactionLinks = Map.empty,
        actions = Map("calculateBenefitValue" -> "/calculation/paye/benefit/new/value-calculation")
      )
    )
    when(mockAuthConnector.authority("/auth/oid/jdensmore")).thenReturn(Some(payeAuthority("jdensmore", "AB123456C")))
  }

  "basic homepage test" should {
    "contain the users first name in the response" ignore new WithApplication(FakeApplication()) {
      val result = testController.testPayeAuthorisation(FakeRequest().withSession(
        SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
        SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
        SessionKeys.userId -> "/auth/oid/jdensmore")
      )

      status(result) should equal(200)
      contentAsString(result) should include("John Densmore")
    }
  }

  "AuthorisedForIdaAction" should {
    "return redirect to login if no Authority is returned from the Auth service" in new WithApplication(FakeApplication()) {
      when(mockAuthConnector.authority(Matchers.eq("/auth/oid/jdensmore"))(Matchers.any[HeaderCarrier])).thenReturn(None)

      val result = testController.testPayeAuthorisation(FakeRequest().withSession(
        SessionKeys.sessionId -> s"session-${UUID.randomUUID().toString}",
        SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
        SessionKeys.userId -> "/auth/oid/jdensmore")
      )

      status(result) should equal(303)
      redirectLocation(result) shouldBe Some(routes.LoginController.businessTaxLogin().url)
    }

    "return internal server error page if the Action throws an exception" ignore new WithApplication(FakeApplication()) {
      val result = testController.testThrowsException(FakeRequest().withSession(
        SessionKeys.sessionId -> s"session-${UUID.randomUUID().toString}",
        SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
        SessionKeys.userId -> "/auth/oid/jdensmore")
      )

      status(result) should equal(500)
      contentAsString(result) should include("java.lang.RuntimeException")
    }

    "return internal server error page if the AuthConnector throws an exception" ignore new WithApplication(FakeApplication()) {
      when(mockAuthConnector.authority("/auth/oid/jdensmore")).thenThrow(new RuntimeException("TEST"))

      val result = testController.testPayeAuthorisation(FakeRequest().withSession(
        SessionKeys.sessionId -> s"session-${UUID.randomUUID().toString}",
        SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
        SessionKeys.userId -> "/auth/oid/jdensmore")
      )

      status(result) should equal(500)
      contentAsString(result) should include("java.lang.RuntimeException")
    }

    "redirect to the Tax Regime landing page if the user is logged in but not authorised for the requested Tax Regime" ignore new WithApplication(FakeApplication()) {
      when(mockAuthConnector.authority("/auth/oid/john")).thenReturn(Some(saAuthority("john", "12345678")))

      val result = testController.testPayeAuthorisation(FakeRequest().withSession(
        SessionKeys.sessionId -> s"session-${UUID.randomUUID}",
        SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
        SessionKeys.userId -> "/auth/oid/john")
      )

      status(result) should equal(303)
      redirectLocation(result).get shouldBe "/login"
    }

    "redirect to the login page when the userId is not found in the session " in new WithApplication(FakeApplication()) {
      val result = testController.testPayeAuthorisation(FakeRequest().withSession(
        SessionKeys.lastRequestTimestamp -> now.getMillis.toString
      ))

      status(result) should equal(303)
      redirectLocation(result).get shouldBe "/samllogin"
    }

    "redirect to the login page when the userId is found but a gateway token is present" in new WithApplication(FakeApplication()) {
      val result = testController.testPayeAuthorisation(FakeRequest().withSession(
        SessionKeys.sessionId -> s"session-${UUID.randomUUID}",
        SessionKeys.userId -> "/auth/oid/john",
        SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
        SessionKeys.token -> "a-government-gateway-token"))

      status(result) should equal(303)
      redirectLocation(result).get shouldBe "/samllogin"
    }

    "add redirect information to the session when required" in new WithApplication(FakeApplication()) {
      val result = testController.testAuthorisationWithRedirectCommand(
        FakeRequest("GET", "/some/path").withSession(
          SessionKeys.sessionId -> s"session-${UUID.randomUUID}",
          SessionKeys.userId -> "/auth/oid/john",
          SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
          SessionKeys.token -> "a-government-gateway-token"
        )
      )
      status(result) should equal(303)
      session(result).get(SessionKeys.redirect) shouldBe Some("/some/path")
    }

  }

}

sealed class TestController(payeConnector: PayeConnector,
                            override val auditConnector: AuditConnector)
                           (implicit override val authConnector: AuthConnector)
  extends Controller
  with Actions
  with RegimeRootBase {

  import MdcLoggingExecutionContext._

  override def regimeRoots(authority: Authority)(implicit hc: HeaderCarrier): Future[RegimeRoots] = {
    for {
      paye <- payeRoot(authority)
    } yield RegimeRoots(paye = paye)
  }

  def testPayeAuthorisation = AuthorisedFor(PayeRegime) {
    implicit user =>
      implicit request =>
        val userPayeRegimeRoot = user.regimes.paye.get
        val userName = userPayeRegimeRoot.name
        Ok(userName)
  }

  def testAuthorisationWithRedirectCommand = AuthenticatedBy(authenticationProvider = IdaWithTokenCheckForBeta, redirectToOrigin = true) {
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