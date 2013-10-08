package controllers

import org.scalatest.mock.MockitoSugar
import play.api.mvc.Controller
import uk.gov.hmrc.common.microservice.auth.AuthMicroService
import org.mockito.Mockito.when
import org.mockito.Mockito.reset
import uk.gov.hmrc.common.microservice.auth.domain.{Regimes, UserAuthority}
import play.api.test.{FakeRequest, FakeApplication, WithApplication}
import play.api.test.Helpers._
import java.net.URI
import org.slf4j.MDC
import uk.gov.hmrc.common.microservice.sa.domain.{SaRoot, SaRegime}
import uk.gov.hmrc.common.microservice.sa.SaConnector
import uk.gov.hmrc.common.BaseSpec
import uk.gov.hmrc.common.microservice.MockMicroServicesForTests
import controllers.common._
import org.scalatest.TestData
import java.util.UUID
import scala.util.Success
import uk.gov.hmrc.common.microservice.epaye.EpayeConnector
import uk.gov.hmrc.common.microservice.ct.CtConnector
import uk.gov.hmrc.common.microservice.vat.VatConnector
import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.VatRoot
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeDomain.{EpayeRoot, EpayeLinks, EpayeJsonRoot}
import uk.gov.hmrc.common.microservice.ct.domain.CtDomain.{CtRoot, CtJsonRoot}
import uk.gov.hmrc.domain.{CtUtr, EmpRef, Vrn}

class AuthorisedForGovernmentGatewayActionSpec extends BaseSpec with MockitoSugar with CookieEncryption with Controller with ActionWrappers with MockMicroServicesForTests with HeaderNames {

  private val token = "someToken"
  override lazy val authMicroService = mock[AuthMicroService]
  override lazy val saConnector = mock[SaConnector]
  override lazy val epayeConnector = mock[EpayeConnector]
  override lazy val ctConnector = mock[CtConnector]
  override lazy val vatConnector = mock[VatConnector]
  val lottyRegime_empref: EmpRef = EmpRef("123", "456")
  val lottyRegime_ctUtr: CtUtr = CtUtr("aCtUtr")

  override protected def beforeEach(testData: TestData) {
    reset(authMicroService)
    reset(saConnector)
    when(saConnector.root("/sa/detail/AB123456C")).thenReturn(
      SaRoot("someUtr", Map("link1" -> "http://somelink/1"))
    )
    when(authMicroService.authority("/auth/oid/gfisher")).thenReturn(
      Some(UserAuthority("/auth/oid/gfisher", Regimes(sa = Some(URI.create("/sa/detail/AB123456C"))), None)))
    when(authMicroService.authority("/auth/oid/lottyRegimes")).thenReturn(
      Some(UserAuthority("/auth/oid/lottyRegimes",
        Regimes(sa = Some(URI.create("/saDetailEndpoint")),
          epaye = Some(URI.create("/epayeDetailEndpoint")),
          vat = Some(URI.create("/vatDetailEndpoint")),
          ct = Some(URI.create("/ctDetailEndpoint"))), None, empRef = Some(lottyRegime_empref), ctUtr = Some(lottyRegime_ctUtr))))
  }


  def test = AuthorisedForGovernmentGatewayAction(Some(SaRegime)) {
    implicit user =>
      implicit request =>
        val saUtr = user.regimes.sa.get.get.utr
        Ok(saUtr)
  }

  def testAuthorisation = AuthorisedForGovernmentGatewayAction(Some(SaRegime)) {
    implicit user =>
      implicit request =>
        val saUtr = user.regimes.sa.get.get.utr
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

  "basic homepage test" should {
    "contain the user's first name in the response" in new WithApplication(FakeApplication()) {
      val result = test(FakeRequest().withSession("sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"), "userId" -> encrypt("/auth/oid/gfisher"), "token" -> encrypt(token)))

      status(result) should equal(200)
      contentAsString(result) should include("someUtr")
    }
  }

  "AuthorisedForGovernmentGatewayAction" should {
    "return Unauthorised if no Authority is returned from the Auth service" in new WithApplication(FakeApplication()) {
      when(authMicroService.authority("/auth/oid/gfisher")).thenReturn(None)

      val result = test(FakeRequest().withSession("sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"), "userId" -> encrypt("/auth/oid/gfisher"), "token" -> encrypt(token)))
      status(result) should equal(401)
    }

    "return internal server error page if the Action throws an exception" in new WithApplication(FakeApplication()) {
      val result = testThrowsException(FakeRequest().withSession("sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"), "userId" -> encrypt("/auth/oid/gfisher"), "token" -> encrypt(token)))
      status(result) should equal(500)
      contentAsString(result) should include("java.lang.RuntimeException")
    }

    "return internal server error page if the AuthMicroService throws an exception" in new WithApplication(FakeApplication()) {
      when(authMicroService.authority("/auth/oid/gfisher")).thenThrow(new RuntimeException("TEST"))

      val result = test(FakeRequest().withSession("sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"), "userId" -> encrypt("/auth/oid/gfisher"), "token" -> encrypt(token)))
      status(result) should equal(500)
      contentAsString(result) should include("java.lang.RuntimeException")
    }

    "include the authorisation and request ids in the MDC" in new WithApplication(FakeApplication()) {
      val result = testMdc(FakeRequest().withSession("sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"), "userId" -> encrypt("/auth/oid/gfisher"), "token" -> encrypt(token)))
      status(result) should equal(200)
      val strings = contentAsString(result).split(" ")
      strings(0) should equal("/auth/oid/gfisher")
      strings(1) should startWith("govuk-tax-")
    }

    "redirect to the Tax Regime landing page if the user is logged in but not authorised for the requested Tax Regime" in new WithApplication(FakeApplication()) {
      when(authMicroService.authority("/auth/oid/bob")).thenReturn(
        Some(UserAuthority("bob", Regimes(sa = None, paye = Some(URI.create("/personal/paye/12345678"))), None)))
      val result = testAuthorisation(FakeRequest().withSession("sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"), "userId" -> encrypt("/auth/oid/bob"), "token" -> encrypt(token)))
      status(result) should equal(303)
    }

    "redirect to the login page when the userId is not found in the session " in new WithApplication(FakeApplication()) {
      val result = testAuthorisation(FakeRequest())
      status(result) should equal(303)
      redirectLocation(result).get shouldBe "/"
    }

    "pass a user with properly constructed regime roots to the wrapped action" in new WithApplication(FakeApplication()) {

      def testRegimeRoots = AuthorisedForGovernmentGatewayAction() {
        implicit user =>
          implicit request =>
            when(saConnector.root("/saDetailEndpoint")).thenReturn(
              SaRoot("someUtr", Map("link1" -> "http://sa/1"))
            )
            when(ctConnector.root("/ctDetailEndpoint")).thenReturn(
              CtJsonRoot(Map("link1" -> "http://ct/1"))
            )
            when(vatConnector.root("/vatDetailEndpoint")).thenReturn(
              VatRoot(Vrn("someVrn"), Map("link1" -> "http://vat/1"))
            )
            when(epayeConnector.root("/epayeDetailEndpoint")).thenReturn(
              EpayeJsonRoot(EpayeLinks(Some("http://epaye/1")))
            )
            user.regimes should have(
              'sa(Some(Success(SaRoot("someUtr", Map("link1" -> "http://sa/1"))))),
              'ct(Some(Success(CtRoot(Map("link1" -> "http://ct/1"), lottyRegime_ctUtr)))),
              'vat(Some(Success(VatRoot(Vrn("someVrn"), Map("link1" -> "http://vat/1"))))),
              'epaye(Some(Success(EpayeRoot(EpayeLinks(Some("http://epaye/1")), lottyRegime_empref)))),
              'paye(None))
            Ok("dummy argument")
      }
      val result = testRegimeRoots(FakeRequest().withSession("sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"),
        "userId" -> encrypt("/auth/oid/lottyRegimes"), "token" -> encrypt(token)))
      status(result) should equal(200)
    }
    "evaluate root endpoints lazily" in new WithApplication(FakeApplication()) {

      def testRegimeRoots = AuthorisedForGovernmentGatewayAction() {
        implicit user =>
          implicit request =>
            when(saConnector.root("/saDetailEndpoint")).thenReturn(
              SaRoot("someUtr", Map("link1" -> "http://sa/1"))
            )
            when(ctConnector.root("/ctDetailEndpoint")).thenReturn(
              CtJsonRoot(Map("link1" -> "http://ct/1"))
            )
            when(vatConnector.root("/vatDetailEndpoint")).thenReturn(
              VatRoot(Vrn("someVrn"), Map("link1" -> "http://vat/1"))
            )
            when(epayeConnector.root("/epayeDetailEndpoint")).thenReturn(
              EpayeJsonRoot(EpayeLinks(Some("http://epaye/1")))
            )
            user.regimes should have(
              'sa(Some(Success(SaRoot("someUtr", Map("link1" -> "http://sa/1"))))),
              'ct(Some(Success(CtRoot(Map("link1" -> "http://ct/1"), lottyRegime_ctUtr)))),
              'vat(Some(Success(VatRoot(Vrn("someVrn"), Map("link1" -> "http://vat/1"))))),
              'epaye(Some(Success(EpayeRoot(EpayeLinks(Some("http://epaye/1")), lottyRegime_empref)))),
              'paye(None))
            Ok("dummy argument")
      }

      when(saConnector.root("/saDetailEndpoint")).thenReturn(
        SaRoot("someUtr", Map("link1" -> "http://sa/2"))
      )
      when(ctConnector.root("/ctDetailEndpoint")).thenReturn(
        CtJsonRoot(Map("link1" -> "http://ct/2"))
      )
      when(vatConnector.root("/vatDetailEndpoint")).thenReturn(
        VatRoot(Vrn("someVrn"), Map("link1" -> "http://vat/2"))
      )
      when(epayeConnector.root("/epayeDetailEndpoint")).thenReturn(
        EpayeJsonRoot(EpayeLinks(Some("http://epaye/2")))
      )

      val result = testRegimeRoots(FakeRequest().withSession("sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"),
        "userId" -> encrypt("/auth/oid/lottyRegimes"), "token" -> encrypt(token)))
      status(result) should equal(200)
    }
  }
}
