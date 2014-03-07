package controllers

import org.mockito.{ArgumentCaptor, Matchers}
import controllers.common.actions.HeaderCarrier
import org.joda.time.{DateTimeZone, DateTime}
import uk.gov.hmrc.common.microservice.auth.domain._
import uk.gov.hmrc.domain.Nino
import controllers.common._
import uk.gov.hmrc.microservice.saml.domain._
import org.mockito.Mockito._
import scala.concurrent.Future
import play.api.test.Helpers._
import uk.gov.hmrc.common.microservice.auth.{AuthConnector, AuthTokenExchangeException}
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.common.microservice.auth.domain.IdaPid
import uk.gov.hmrc.common.microservice.auth.domain.Accounts
import controllers.common.AuthExchangeResponse
import uk.gov.hmrc.common.microservice.auth.domain.Authority
import controllers.common.AuthToken
import scala.Some
import uk.gov.hmrc.microservice.saml.domain.AuthResponseValidationResult
import uk.gov.hmrc.common.microservice.auth.domain.PayeAccount
import controllers.common.SAMLResponse
import play.api.test.{WithApplication, FakeRequest}
import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.saml.SamlConnector
import uk.gov.hmrc.common.microservice.governmentgateway.GovernmentGatewayConnector
import uk.gov.hmrc.common.microservice.audit.{AuditEvent, AuditConnector}
import uk.gov.hmrc.common.microservice.auth.domain.{Credentials => AuthCredentials}

class IdaLoginControllerSpec extends BaseSpec with MockitoSugar {

  private abstract class LoginSetup extends WithApplication {
    lazy val mockSamlConnector = mock[SamlConnector]
    lazy val mockAuthConnector = mock[AuthConnector]
    lazy val mockGovernmentGatewayConnector = mock[GovernmentGatewayConnector]
    lazy val mockAuditConnector = mock[AuditConnector]
    implicit lazy val request = FakeRequest()
    lazy val controller = new IdaLoginController(mockSamlConnector, mockAuditConnector)(mockAuthConnector)
  }

  private trait IdaLoginSetup extends LoginSetup {

    def anyHc = Matchers.any[HeaderCarrier]

    val loginTime = new DateTime(2014, 1, 22, 11, 33, 55, 555, DateTimeZone.UTC)
    val idaAuthority = Authority("/auth/oid/0943809346039", AuthCredentials(idaPids = Set(IdaPid(hashPid, loginTime, loginTime))), Accounts(paye = Some(PayeAccount("/paye/blah", Nino("AB112233C")))), Some(loginTime), None)
    val hashPid = "hash-pid"
    val originalRequestId = "govuk-tax-request-id"
    val authToken = AuthToken("auth-token")
    val samlResponse = SAMLResponse("this_can_be_anything_really")

    def expectALoginFailedAuditEventFor(trasnsationName: String, reason: String) = {
      val captor = ArgumentCaptor.forClass(classOf[AuditEvent])
      verify(mockAuditConnector).audit(captor.capture())(Matchers.any())

      val event = captor.getValue

      event.auditType should be("TxFailed")
      event.tags should contain("transactionName" -> trasnsationName)
      event.detail should contain("transactionFailureReason" -> reason)
    }
  }

  private trait SamlResponseFormSetup extends LoginSetup {
    override lazy val controller = new IdaLoginController()
  }

  "validating the SAML response form and getting its value" should {

    "return the SAML response object if the form is valid" in new SamlResponseFormSetup {
      controller.validateAngGetSamlResponse(request.withFormUrlEncodedBody(("SAMLResponse","some-valid-saml-response"))) shouldBe Some(SAMLResponse("some-valid-saml-response"))
    }

    "return None if the SAML response field is absent" in new SamlResponseFormSetup {
      controller.validateAngGetSamlResponse(request.withFormUrlEncodedBody(("SAMLResponse",""))) shouldBe None
    }
  }

  "processing Ida login" should {

    "handle a successful login if the Ida response is a match" in new IdaLoginSetup {
      val idaResponse = AuthResponseValidationResult(Match, Some(hashPid), Some(originalRequestId))

      when(mockSamlConnector.validate(Matchers.eq(samlResponse.response))(anyHc)).thenReturn(Future.successful(idaResponse))
      when(mockAuthConnector.exchangePidForBearerToken(Matchers.eq(hashPid))(anyHc)).thenReturn(Future.successful(AuthExchangeResponse(authToken, idaAuthority)))
      val result = controller.processIdaLogin(samlResponse)

      status(result) shouldBe 303
      val locationHeader = header("Location", result)
      locationHeader shouldBe defined
      locationHeader.get shouldBe FrontEndRedirect.carBenefit(None)
    }

    "handle a failure login if the Ida response is no-match" in new IdaLoginSetup {
      val idaResponse = AuthResponseValidationResult(NoMatch, Some(hashPid), Some(originalRequestId))

      when(mockSamlConnector.validate(Matchers.eq(samlResponse.response))(anyHc)).thenReturn(Future.successful(idaResponse))
      val result = controller.processIdaLogin(samlResponse)
      verifyZeroInteractions(mockAuthConnector)

      status(result) shouldBe 401
    }

    "handle a failure login if the Ida response is match but auth does not find any record with the given pid" in new IdaLoginSetup {
      val idaResponse = AuthResponseValidationResult(NoMatch, Some(hashPid), Some(originalRequestId))

      when(mockSamlConnector.validate(Matchers.eq(samlResponse.response))(anyHc)).thenReturn(Future.successful(idaResponse))
      when(mockAuthConnector.exchangePidForBearerToken(Matchers.eq(hashPid))(anyHc)).thenReturn(Future.failed(AuthTokenExchangeException("idType")))
      val result = controller.processIdaLogin(samlResponse)

      status(result) shouldBe 401
    }

    "redirect to the signed out page if the Ida response is cancel" in new IdaLoginSetup {
      val idaResponse = AuthResponseValidationResult(Cancel, None, Some(originalRequestId))

      when(mockSamlConnector.validate(Matchers.eq(samlResponse.response))(anyHc)).thenReturn(Future.successful(idaResponse))
      val result = controller.processIdaLogin(samlResponse)
      verifyZeroInteractions(mockAuthConnector)

      status(result) shouldBe 303
      val locationHeader = header("Location", result)
      locationHeader shouldBe defined
      locationHeader.get shouldBe routes.LoginController.signedOut.url
    }

    "throw an exception if SAML got whatever error when validating Ida response" in new IdaLoginSetup with ScalaFutures {
      val idaResponse = AuthResponseValidationResult(Error, None, Some(originalRequestId))

      when(mockSamlConnector.validate(Matchers.eq(samlResponse.response))(anyHc)).thenReturn(Future.successful(idaResponse))
      val result = controller.processIdaLogin(samlResponse)
      verifyZeroInteractions(mockAuthConnector)

      result.failed.futureValue shouldBe a[RuntimeException]
    }
  }

  "Ida login" should {

    "successfully login user if Ida response is a match" in new IdaLoginSetup {
      val idaResponse = AuthResponseValidationResult(Match, Some(hashPid), Some(originalRequestId))

      when(mockSamlConnector.validate(Matchers.eq(samlResponse.response))(anyHc)).thenReturn(Future.successful(idaResponse))
      when(mockAuthConnector.exchangePidForBearerToken(Matchers.eq(hashPid))(anyHc)).thenReturn(Future.successful(AuthExchangeResponse(authToken, idaAuthority)))
      val result = controller.idaLogin(request.withFormUrlEncodedBody(("SAMLResponse",samlResponse.response)))

      status(result) shouldBe 303
      val locationHeader = header("Location", result)
      locationHeader shouldBe defined
      locationHeader.get shouldBe FrontEndRedirect.carBenefit(None)
    }
  }

  "Ida login controller" should {

      "audit the login failed event if the saml response is empty" in new IdaLoginSetup with ScalaFutures {

        val result = controller.idaLogin()(FakeRequest(POST, "/ida/login").withFormUrlEncodedBody(("SAMLResponse", "")))

        status(result) shouldBe 401

        whenReady (result) { _=>
          expectALoginFailedAuditEventFor("IDA Login", "SAML authentication response received without SAMLResponse data")
        }
      }

      "audit the login failed event if the saml response fails validation" in new IdaLoginSetup with ScalaFutures {

        when(mockSamlConnector.validate(Matchers.eq(samlResponse.response))(Matchers.any[HeaderCarrier])).thenReturn(AuthResponseValidationResult(NoMatch, None, Some(originalRequestId)))

        val result = controller.idaLogin()(FakeRequest(POST, "/ida/login").withFormUrlEncodedBody(("SAMLResponse", samlResponse.response)))

        status(result) shouldBe 401

        whenReady (result) { _=>
          expectALoginFailedAuditEventFor("IDA Login", "SAMLResponse failed validation")
        }
      }

      "audit the login failed event if there is no Authority record matching the hash pid" in new IdaLoginSetup with ScalaFutures {

        when(mockSamlConnector.validate(Matchers.eq(samlResponse.response))(Matchers.any[HeaderCarrier])).thenReturn(Future.successful(AuthResponseValidationResult(Match.toString, Some(hashPid), Some(originalRequestId))))

        when(mockAuthConnector.exchangePidForBearerToken(Matchers.eq(hashPid))(Matchers.any[HeaderCarrier])).thenReturn(Future.failed(AuthTokenExchangeException("pid")))

        val result = controller.idaLogin()(FakeRequest(POST, "/ida/login").withFormUrlEncodedBody(("SAMLResponse", samlResponse.response)))

        status(result) shouldBe 401

        whenReady(result) {
          _ =>
            expectALoginFailedAuditEventFor("IDA Login", "No record found in Auth for the PID")
        }
      }
    }



    }
