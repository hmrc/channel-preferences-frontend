package controllers

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import org.mockito.Mockito._
import play.api.test.{FakeRequest, WithApplication}
import play.api.test.Helpers._
import java.net.URI
import org.slf4j.MDC
import uk.gov.hmrc.common.microservice.sa.domain.{SaRoot, SaJsonRoot, SaRegime}
import uk.gov.hmrc.common.microservice.sa.SaConnector
import uk.gov.hmrc.common.BaseSpec
import controllers.common._
import org.scalatest.TestData
import java.util.UUID
import uk.gov.hmrc.common.microservice.epaye.EpayeConnector
import uk.gov.hmrc.common.microservice.ct.CtConnector
import uk.gov.hmrc.common.microservice.vat.VatConnector
import uk.gov.hmrc.domain.EmpRef
import controllers.common.SessionTimeoutWrapper._
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import controllers.common.service.Connectors._
import uk.gov.hmrc.common.microservice.vat.domain.VatRoot
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeRoot
import uk.gov.hmrc.common.microservice.ct.domain.CtRoot
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import scala.Some
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.common.microservice.auth.domain.Regimes
import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import play.api.test.FakeApplication
import controllers.common.actions.Actions
import play.api.mvc.SimpleResult

class AuthorisedForGovernmentGatewayActionSpec
  extends BaseSpec
  with MockitoSugar
  with CookieEncryption
  with HeaderNames {

  import config.DateTimeProvider._

  private val token = "someToken"

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

    when(authConnector.authority("/auth/oid/gfisher")).thenReturn(
      Some(
        UserAuthority(
          id = "/auth/oid/gfisher",
          saUtr = Some(SaUtr("3333333333")),
          regimes = Regimes(
            sa = Some(URI.create("/sa/detail/3333333333")
            )
          )
        )
      )
    )

    when(authConnector.authority("/auth/oid/lottyRegimes")).thenReturn(
      Some(UserAuthority("/auth/oid/lottyRegimes",
        regimes = Regimes(sa = Some(URI.create("/saDetailEndpoint")),
          epaye = Some(URI.create("/epayeDetailEndpoint")),
          vat = Some(URI.create("/vatDetailEndpoint")),
          ct = Some(URI.create("/ctDetailEndpoint"))),
        vrn = Some(lottyRegime_vrn),
        empRef = Some(lottyRegime_empRef),
        ctUtr = Some(lottyRegime_ctUtr),
        saUtr = Some(lottyRegime_saUtr))))
  }

  "basic homepage test" should {
    "contain the user's first name in the response" in new WithApplication(FakeApplication()) {
      val result = testController.test(FakeRequest().withSession(
        "sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"),
        lastRequestTimestampKey -> now().getMillis.toString,
        "userId" -> encrypt("/auth/oid/gfisher"),
        "token" -> encrypt(token)))

      status(result) should equal(200)
      contentAsString(result) should include("3333333333")
    }
  }

  "AuthorisedForGovernmentGatewayAction" should {
    "return Unauthorised if no Authority is returned from the Auth service" in new WithApplication(FakeApplication()) {
      when(authConnector.authority("/auth/oid/gfisher")).thenReturn(None)

      val result = testController.test(FakeRequest().withSession(
        "sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"),
        lastRequestTimestampKey -> now().getMillis.toString,
        "userId" -> encrypt("/auth/oid/gfisher"),
        "token" -> encrypt(token)))

      status(result) should equal(401)
    }

    "return internal server error page if the Action throws an exception" ignore new WithApplication(FakeApplication()) {
      val result = testController.testThrowsException(FakeRequest().withSession(
        "sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"),
        lastRequestTimestampKey -> now().getMillis.toString,
        "userId" -> encrypt("/auth/oid/gfisher"),
        "token" -> encrypt(token)))

      status(result) should equal(500)
      contentAsString(result) should include("java.lang.RuntimeException")
    }

    "return internal server error page if the AuthConnector throws an exception" in new WithApplication(FakeApplication()) {
      when(authConnector.authority("/auth/oid/gfisher")).thenThrow(new RuntimeException("TEST"))

      val result = testController.test(FakeRequest().withSession(
        "sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"),
        lastRequestTimestampKey -> now().getMillis.toString,
        "userId" -> encrypt("/auth/oid/gfisher"),
        "token" -> encrypt(token)))

      status(result) should equal(500)
      contentAsString(result) should include("java.lang.RuntimeException")
    }

    "include the authorisation and request ids in the MDC" in new WithApplication(FakeApplication()) {
      val result = testController.testMdc(FakeRequest().withSession(
        "sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"),
        lastRequestTimestampKey -> now().getMillis.toString,
        "userId" -> encrypt("/auth/oid/gfisher"),
        "token" -> encrypt(token)))

      status(result) should equal(200)
      val strings = contentAsString(result).split(" ")
      strings(0) should equal("/auth/oid/gfisher")
      strings(1) should startWith("govuk-tax-")
    }

    "redirect to the Tax Regime landing page if the user is logged in but not authorised for the requested Tax Regime" in new WithApplication(FakeApplication()) {
      when(authConnector.authority("/auth/oid/bob")).thenReturn(
        Some(UserAuthority("bob", Regimes(sa = None, paye = Some(URI.create("/personal/paye/12345678"))), None)))
      val result = testController.testAuthorisation(FakeRequest().withSession(
        "sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"),
        lastRequestTimestampKey -> now().getMillis.toString,
        "userId" -> encrypt("/auth/oid/bob"),
        "token" -> encrypt(token))
      )

      status(result) should equal(303)
    }

    "redirect to the login page when the userId is not found in the session " in new WithApplication(FakeApplication()) {
      val result = testController.testAuthorisation(FakeRequest())
      status(result) should equal(303)
      redirectLocation(result).get shouldBe "/"
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
    //        "sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"),
    //        lastRequestTimestampKey -> now().getMillis.toString,
    //        "userId" -> encrypt("/auth/oid/lottyRegimes"),
    //        "token" -> encrypt(token)
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
  with Actions {

  override def regimeRoots(authority: UserAuthority): RegimeRoots = {
    val regimes = authority.regimes
    RegimeRoots(
      sa = regimes.sa map {
        uri => SaRoot(authority.saUtr.get, saConnector.root(uri.toString))
      },
      vat = regimes.vat map {
        uri => VatRoot(authority.vrn.get, vatConnector.root(uri.toString))
      },
      epaye = regimes.epaye.map {
        uri => EpayeRoot(authority.empRef.get, epayeConnector.root(uri.toString))
      },
      ct = regimes.ct.map {
        uri => CtRoot(authority.ctUtr.get, ctConnector.root(uri.toString))
      }
    )
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

  def testMdc = AuthorisedFor(SaRegime) {
    implicit user =>
      implicit request =>
        Ok(s"${MDC.get(authorisation)} ${MDC.get(requestId)}")
  }
}
