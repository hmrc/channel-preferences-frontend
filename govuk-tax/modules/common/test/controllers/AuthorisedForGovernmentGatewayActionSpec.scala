package controllers

import org.scalatest.mock.MockitoSugar
import play.api.mvc.Controller
import uk.gov.hmrc.common.microservice.auth.AuthMicroService
import org.mockito.Mockito.when
import org.mockito.Mockito.reset
import play.api.test.{FakeRequest, WithApplication}
import play.api.test.Helpers._
import java.net.URI
import org.slf4j.MDC
import uk.gov.hmrc.common.microservice.sa.domain.SaDomain.SaRoot
import uk.gov.hmrc.common.microservice.sa.domain.SaRegime
import uk.gov.hmrc.common.microservice.sa.SaConnector
import uk.gov.hmrc.common.BaseSpec
import uk.gov.hmrc.common.microservice.MockMicroServicesForTests
import controllers.common._
import org.scalatest.TestData
import java.util.UUID
import uk.gov.hmrc.common.microservice.epaye.EpayeConnector
import uk.gov.hmrc.common.microservice.ct.CtConnector
import uk.gov.hmrc.common.microservice.vat.VatConnector
import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.VatRoot
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeDomain.EpayeRoot
import uk.gov.hmrc.common.microservice.ct.domain.CtDomain.CtRoot
import uk.gov.hmrc.domain.EmpRef
import controllers.common.SessionTimeoutWrapper._
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeDomain.EpayeLinks
import uk.gov.hmrc.common.microservice.sa.domain.SaDomain.SaJsonRoot
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.common.microservice.auth.domain.Regimes
import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.common.microservice.ct.domain.CtDomain.CtJsonRoot
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeDomain.EpayeJsonRoot
import play.api.test.FakeApplication
import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.VatJsonRoot

class AuthorisedForGovernmentGatewayActionSpec
  extends BaseSpec
  with MockitoSugar
  with CookieEncryption
  with Controller
  with ActionWrappers
  with MockMicroServicesForTests
  with HeaderNames {

  private val token = "someToken"

  override lazy val authMicroService = mock[AuthMicroService]
  override lazy val saConnector = mock[SaConnector]
  override lazy val epayeConnector = mock[EpayeConnector]
  override lazy val ctConnector = mock[CtConnector]
  override lazy val vatConnector = mock[VatConnector]

  private val lottyRegime_empRef = EmpRef("123", "456")
  private val lottyRegime_ctUtr = CtUtr("aCtUtr")
  private val lottyRegime_vrn = Vrn("someVrn")
  private val lottyRegime_saUtr = SaUtr("aSaUtr")

  override protected def beforeEach(testData: TestData) {
    reset(authMicroService)
    reset(saConnector)

    when(saConnector.root("/sa/detail/3333333333")).thenReturn(
      SaJsonRoot(Map("link1" -> "http://somelink/1"))
    )

    when(authMicroService.authority("/auth/oid/gfisher")).thenReturn(
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

    when(authMicroService.authority("/auth/oid/lottyRegimes")).thenReturn(
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


  def test = AuthorisedForGovernmentGatewayAction(Some(SaRegime)) {
    implicit user =>
      implicit request =>
        val saUtr = user.regimes.sa.get.utr
        Ok(saUtr.utr)
  }

  def testAuthorisation = AuthorisedForGovernmentGatewayAction(Some(SaRegime)) {
    implicit user =>
      implicit request =>
        val saUtr = user.regimes.sa.get.utr
        Ok(saUtr.utr)
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

  "basic homepage test" should {
    "contain the user's first name in the response" in new WithApplication(FakeApplication()) {
      val result = test(FakeRequest().withSession(
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
      when(authMicroService.authority("/auth/oid/gfisher")).thenReturn(None)

      val result = test(FakeRequest().withSession(
        "sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"),
        lastRequestTimestampKey -> now().getMillis.toString,
        "userId" -> encrypt("/auth/oid/gfisher"),
        "token" -> encrypt(token)))

      status(result) should equal(401)
    }

    "return internal server error page if the Action throws an exception" in new WithApplication(FakeApplication()) {
      val result = testThrowsException(FakeRequest().withSession(
        "sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"),
        lastRequestTimestampKey -> now().getMillis.toString,
        "userId" -> encrypt("/auth/oid/gfisher"),
        "token" -> encrypt(token)))

      status(result) should equal(500)
      contentAsString(result) should include("java.lang.RuntimeException")
    }

    "return internal server error page if the AuthMicroService throws an exception" in new WithApplication(FakeApplication()) {
      when(authMicroService.authority("/auth/oid/gfisher")).thenThrow(new RuntimeException("TEST"))

      val result = test(FakeRequest().withSession(
        "sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"),
        lastRequestTimestampKey -> now().getMillis.toString,
        "userId" -> encrypt("/auth/oid/gfisher"),
        "token" -> encrypt(token)))

      status(result) should equal(500)
      contentAsString(result) should include("java.lang.RuntimeException")
    }

    "include the authorisation and request ids in the MDC" in new WithApplication(FakeApplication()) {
      val result = testMdc(FakeRequest().withSession(
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
      when(authMicroService.authority("/auth/oid/bob")).thenReturn(
        Some(UserAuthority("bob", Regimes(sa = None, paye = Some(URI.create("/personal/paye/12345678"))), None)))
      val result = testAuthorisation(FakeRequest().withSession(
        "sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"),
        lastRequestTimestampKey -> now().getMillis.toString,
        "userId" -> encrypt("/auth/oid/bob"),
        "token" -> encrypt(token))
      )

      status(result) should equal(303)
    }

    "redirect to the login page when the userId is not found in the session " in new WithApplication(FakeApplication()) {
      val result = testAuthorisation(FakeRequest())
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
