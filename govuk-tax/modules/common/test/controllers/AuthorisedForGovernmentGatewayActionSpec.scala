package controllers

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import org.mockito.Mockito._
import play.api.test.{FakeRequest, WithApplication}
import play.api.test.Helpers._
import uk.gov.hmrc.common.microservice.sa.domain.SaRegime
import uk.gov.hmrc.common.microservice.sa.SaConnector
import uk.gov.hmrc.common.BaseSpec
import controllers.common._
import org.scalatest.TestData
import java.util.UUID
import uk.gov.hmrc.common.microservice.epaye.EpayeConnector
import uk.gov.hmrc.common.microservice.ct.CtConnector
import uk.gov.hmrc.common.microservice.vat.VatConnector
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import controllers.common.service.Connectors._
import controllers.common.actions.{HeaderCarrier, Actions}
import scala.concurrent.Future
import uk.gov.hmrc.common.microservice.auth.domain.Authority
import uk.gov.hmrc.common.microservice.sa.domain.SaJsonRoot
import scala.Some
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import play.api.test.FakeApplication

class AuthorisedForGovernmentGatewayActionSpec
  extends BaseSpec
  with MockitoSugar {

  import config.DateTimeProvider._
  import controllers.domain.AuthorityUtils._

  private val tokenValue = "someToken"

  val saConnector = mock[SaConnector]
  val epayeConnector = mock[EpayeConnector]
  val ctConnector = mock[CtConnector]
  val vatConnector = mock[VatConnector]
  val authConnector = mock[AuthConnector]

  val testController = new AuthorisedForGovernmentGatewayActionSpecController(saConnector, epayeConnector, ctConnector, vatConnector, null)(authConnector)

  private val lottyRegime_empRef = EmpRef("123", "456")
  private val lottyRegime_ctUtr = CtUtr("aCtUtr")
  private val lottyRegime_vrn = Vrn("someVrn")
  private val lottyRegime_saUtr = SaUtr("aSaUtr")

  override protected def beforeEach(testData: TestData) {
    reset(saConnector, epayeConnector, ctConnector, vatConnector, authConnector)

    when(saConnector.root("/sa/detail/3333333333")).thenReturn(
      SaJsonRoot(Map("link1" -> "http://somelink/1"))
    )

    when(authConnector.authority("/auth/oid/gfisher")).thenReturn(Some(saAuthority("gfisher", "3333333333")))
//
//    when(authConnector.authority("/auth/oid/lottyRegimes")).thenReturn(
//      Some(UserAuthority("/auth/oid/lottyRegimes",
//        regimes = Regimes(sa = Some(URI.create("/saDetailEndpoint")),
//          epaye = Some(URI.create("/epayeDetailEndpoint")),
//          vat = Some(URI.create("/vatDetailEndpoint")),
//          ct = Some(URI.create("/ctDetailEndpoint"))),
//        vrn = Some(lottyRegime_vrn),
//        empRef = Some(lottyRegime_empRef),
//        ctUtr = Some(lottyRegime_ctUtr),
//        saUtr = Some(lottyRegime_saUtr))))
  }

  "basic homepage test" should {
    "contain the users first name in the response" ignore new WithApplication(FakeApplication()) {
      val result = testController.test(FakeRequest().withSession(
        SessionKeys.sessionId -> s"session-${UUID.randomUUID}",
        SessionKeys.lastRequestTimestamp -> now().getMillis.toString,
        SessionKeys.userId -> "/auth/oid/gfisher",
        SessionKeys.token -> tokenValue))

      status(result) should equal(200)
      contentAsString(result) should include("3333333333")
    }
  }

  "AuthorisedForGovernmentGatewayAction" should {

    "return internal server error page if the Action throws an exception" ignore new WithApplication(FakeApplication()) {
      val result = testController.testThrowsException(FakeRequest().withSession(
        SessionKeys.sessionId -> s"session-${UUID.randomUUID}",
        SessionKeys.lastRequestTimestamp -> now().getMillis.toString,
        SessionKeys.userId -> "/auth/oid/gfisher",
        SessionKeys.token -> tokenValue))

      status(result) should equal(500)
      contentAsString(result) should include("java.lang.RuntimeException")
    }

    "return internal server error page if the AuthConnector throws an exception" ignore new WithApplication(FakeApplication()) {
      when(authConnector.authority("/auth/oid/gfisher")).thenThrow(new RuntimeException("TEST"))

      val result = testController.test(FakeRequest().withSession(
        SessionKeys.sessionId -> s"session-${UUID.randomUUID}",
        SessionKeys.lastRequestTimestamp -> now().getMillis.toString,
        SessionKeys.userId -> "/auth/oid/gfisher",
        SessionKeys.token -> tokenValue))

      status(result) should equal(500)
      contentAsString(result) should include("java.lang.RuntimeException")
    }

    "redirect to the Tax Regime landing page if the user is logged in but not authorised for the requested Tax Regime" ignore new WithApplication(FakeApplication()) {
      when(authConnector.authority("/auth/oid/bob")).thenReturn(Some(payeAuthority("bob","12345678")))
      val result = testController.testAuthorisation(FakeRequest().withSession(
        SessionKeys.sessionId -> s"session-${UUID.randomUUID}",
        SessionKeys.lastRequestTimestamp -> now().getMillis.toString,
        SessionKeys.userId -> "/auth/oid/bob",
        SessionKeys.token -> tokenValue)
      )

      status(result) should equal(303)
    }

    "redirect to the login page when the userId is not found in the session " in new WithApplication(FakeApplication()) {
      val result = testController.testAuthorisation(FakeRequest())
      status(result) should equal(303)
      redirectLocation(result) shouldBe Some(routes.LoginController.businessTaxLogin().url)
    }
    // MAT: TODO: fix this
    //    "pass a user with properly constructed regime roots to the wrapped action" in new WithApplication(FakeApplication()) {
    //
    //      def testRegimeRoots = AuthorisedForGovernmentGatewayAction() {
    //        implicit user =>
    //          implicit request =>
    //
    //            when(saConnector.root("/saDetailEndpoint")).thenReturn(SaJsonRoot(Map("link1" -> "http://sa/1")))
    //            when(ctConnector.root("/ctDetailEndpoint")).thenReturn(CtJsonRoot(Map("link1" -> "http://ct/1")))
    //            when(vatConnector.root("/vatDetailEndpoint")).thenReturn(VatJsonRoot(Map("link1" -> "http://vat/1")))
    //            when(epayeConnector.root("/epayeDetailEndpoint")).thenReturn(EpayeJsonRoot(EpayeLinks(Some("http://epaye/1"))))
    //
    //            user.regimes should have(
    //              'sa(Some(SaRoot(lottyRegime_saUtr, Map("link1" -> "http://sa/1")))),
    //              'ct(Some(CtRoot(lottyRegime_ctUtr, Map("link1" -> "http://ct/1")))),
    //              'vat(Some(VatRoot(lottyRegime_vrn, Map("link1" -> "http://vat/1")))),
    //              'epaye(Some(EpayeRoot(lottyRegime_empRef, EpayeLinks(Some("http://epaye/1"))))),
    //              'paye(None))
    //
    //            Ok("dummy argument")
    //      }
    //
    //      val result = testRegimeRoots(FakeRequest().withSession(
    //        SessionKeys.sessionId -> s"session-${UUID.randomUUID}",
    //        SessionKeys.lastRequestTimestamp -> now().getMillis.toString,
    //        SessionKeys.userId -> "/auth/oid/lottyRegimes",
    //        SessionKeys.token -> token
    //      ))
    //
    //      status(result) should equal(200)
    //    }
  }
}

sealed class AuthorisedForGovernmentGatewayActionSpecController(saConnector: SaConnector,
                                                                epayeConnector: EpayeConnector,
                                                                ctConnector: CtConnector,
                                                                vatConnector: VatConnector,
                                                                override val auditConnector: AuditConnector)
                                                               (implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with RegimeRootBase {

  override def regimeRoots(authority: Authority)(implicit hc: HeaderCarrier): Future[RegimeRoots] = {
    for {
      sa <- saRoot(authority)
      vat <- vatRoot(authority)
      epaye <- epayeRoot(authority)
      ct <- ctRoot(authority)
    } yield RegimeRoots(sa = sa, vat = vat, epaye = epaye, ct = ct)
  }

  def test = AuthorisedFor(SaRegime) {
    implicit user =>
      implicit request =>
        val saUtr = user.regimes.sa.get.utr
        Ok(saUtr.utr)
  }

  def testAuthorisation = AuthorisedFor(SaRegime) {
    implicit user =>
      implicit request =>
        val saUtr = user.regimes.sa.get.utr
        Ok(saUtr.utr)
  }


  def testThrowsException = AuthorisedFor(SaRegime) {
    implicit user =>
      implicit request =>
        throw new RuntimeException("ACTION TEST")
  }
}
