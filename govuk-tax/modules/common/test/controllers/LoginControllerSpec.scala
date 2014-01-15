package controllers

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.saml.SamlConnector
import org.mockito.Mockito._
import play.api.test.{ WithApplication, FakeRequest }
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.governmentgateway.GovernmentGatewayConnector
import play.api.http._
import controllers.common._
import uk.gov.hmrc.common.BaseSpec
import controllers.domain.AuthorityUtils._
import uk.gov.hmrc.microservice.saml.domain.AuthRequestFormData
import uk.gov.hmrc.common.microservice.governmentgateway.GovernmentGatewayResponse
import uk.gov.hmrc.common.microservice.UnauthorizedException
import play.api.libs.ws.Response
import uk.gov.hmrc.microservice.saml.domain.AuthResponseValidationResult
import uk.gov.hmrc.common.microservice.governmentgateway.Credentials
import uk.gov.hmrc.common.microservice.ForbiddenException
import play.api.test.FakeApplication
import play.api.templates.Html
import uk.gov.hmrc.utils.DateTimeUtils
import org.mockito.{ArgumentCaptor, Matchers}
import controllers.common.actions.HeaderCarrier
import uk.gov.hmrc.common.microservice.audit.{AuditEvent, AuditConnector}
import uk.gov.hmrc.common.microservice.governmentgateway.GatewayToken
import scala.concurrent.Future
import org.scalatest.concurrent.ScalaFutures

class LoginControllerSpec extends BaseSpec with MockitoSugar {

  import play.api.test.Helpers._

  abstract class WithSetup(additionalConfiguration: Map[String, _] = Map.empty) extends WithApplication(FakeApplication(additionalConfiguration = additionalConfiguration)) {
    lazy val mockSamlConnector = {
      val samlC = mock[SamlConnector]

      when(samlC.create(Matchers.any[HeaderCarrier])).thenReturn(
        AuthRequestFormData("http://www.ida.gov.uk/saml", "0987654321")
      )

      samlC
    }

    lazy val mockAuthConnector = mock[AuthConnector]
    lazy val mockGovernmentGatewayConnector = mock[GovernmentGatewayConnector]
    lazy val mockBusinessTaxPages = mock[BusinessTaxPages]
    lazy val mockAuditConnector = mock[AuditConnector]
    lazy val loginController = new LoginController(mockSamlConnector, mockGovernmentGatewayConnector, mockAuditConnector)(mockAuthConnector){

      override def notOnBusinessTaxWhitelistPage = {
        Html(mockBusinessTaxPages.notOnBusinessTaxWhitelistPage)
      }
    }

    lazy val originalRequestId = "govuk-tax-325-235235-23523"


    def expectALoginFailedAuditEventFor(trasnsationName: String, reason: String) = {
      val captor = ArgumentCaptor.forClass(classOf[AuditEvent])
      verify(mockAuditConnector).audit(captor.capture())(Matchers.any())

      val event = captor.getValue

      event.auditType should be ("TxFailed")
      event.tags should contain ("transactionName" -> trasnsationName)
      event.detail should contain ("transactionFailureReason" -> reason)
    }
  }

  trait BusinessTaxPages {
    def notOnBusinessTaxWhitelistPage: String = ""
  }

  "Login controller GET /login" should {
    "forward to the login page" in new WithSetup {
      val result = loginController.login()(FakeRequest())
      val redirectUrl  =  s"""href="${FrontEndRedirect.payeHome}""""

      status(result) should be(200)
      contentAsString(result) should include(redirectUrl)
    }
  }

  "Login controller GET /samllogin" should {
    "return a form that contains th§e data from the saml service" in new WithSetup {
      val result = loginController.samlLogin()(FakeRequest())

      status(result) should be(200)
      contentAsString(result) should include("action=\"http://www.ida.gov.uk/saml\"")
      contentAsString(result) should include("value=\"0987654321\"")
    }
  }

  "Login controller POST /ida/login" should {

    val samlResponse = "98ewgiher9t8ho4fh4hfgo48whfkw4h8o"

    val hashPid = "09weu03t8e4gfo8"

    val oid = "0943809346039"
    val id = s"/auth/oid/$oid"

    abstract class TestCase extends WithSetup with ScalaFutures {
      def setupValidRequest() = {
        when(mockSamlConnector.validate(Matchers.eq(samlResponse))(Matchers.any[HeaderCarrier])).thenReturn(AuthResponseValidationResult(valid = true, Some(hashPid), Some(originalRequestId)))

        when(mockAuthConnector.authorityByPidAndUpdateLoginTime(Matchers.eq(hashPid))(Matchers.any[HeaderCarrier])).thenReturn(Some(emptyAuthority(oid)))
        FakeRequest(POST, "/ida/login").withFormUrlEncodedBody(("SAMLResponse", samlResponse))
      }
    }

    "redirect to the home page if the response is valid" in new TestCase {
      val result = loginController.idaLogin()(setupValidRequest())

      status(result) shouldBe 303
      redirectLocation(result).get shouldBe FrontEndRedirect.payeHome

      val sess = session(result)
      sess(SessionKeys.userId) shouldBe id

      verify(mockAuditConnector).audit(Matchers.any())(Matchers.any())
    }

    "generate an audit event for successful login if the response is valid" in new TestCase with ScalaFutures{
      whenReady( loginController.idaLogin()(setupValidRequest()) ) { _=>

        val captor = ArgumentCaptor.forClass(classOf[AuditEvent])
        verify(mockAuditConnector).audit(captor.capture())(Matchers.any())

        val event = captor.getValue

        event.auditSource should be ("frontend")
        event.auditType should be ("TxSucceeded")
        event.tags should (
          contain ("transactionName" -> "IDA Login") and
          contain ("X-Request-ID" -> originalRequestId) and
          contain key "X-Request-ID-Original" and
          contain key "X-Session-ID"
        )
        event.detail should (
          contain ("hashPid" -> hashPid) and
          contain ("authId" -> id)
        )
      }
    }

    "generate an audit event for failed login if the AuthResponseValidationResult is not valid" in new TestCase {
        when(mockSamlConnector.validate(Matchers.eq(samlResponse))(Matchers.any[HeaderCarrier])).thenReturn(AuthResponseValidationResult(valid = false, Some(hashPid), Some(originalRequestId)))

        val request = FakeRequest(POST, "/ida/login").withFormUrlEncodedBody(("SAMLResponse", samlResponse))

       whenReady( loginController.idaLogin()(request)) { _=>

        val captor = ArgumentCaptor.forClass(classOf[AuditEvent])
        verify(mockAuditConnector).audit(captor.capture())(Matchers.any())

        val event = captor.getValue

        event.auditSource should be ("frontend")
        event.auditType should be ("TxFailed")
        event.tags should (
          contain ("transactionName" -> "IDA Login") and
            contain ("X-Request-ID" -> originalRequestId) and
            contain key "X-Request-ID-Original"
          )
        event.detail should contain ("transactionFailureReason" -> "SAMLResponse failed validation")
      }
    }

    "return Unauthorised if the post does not contain a saml response" in new TestCase {

      val result = loginController.idaLogin()(FakeRequest(POST, "/ida/login").withFormUrlEncodedBody(("Noddy", "BigEars")))

      status(result) shouldBe 401
      contentAsString(result) should include("There was a problem signing you in")

      whenReady (result) { _=>
        expectALoginFailedAuditEventFor("IDA Login", "SAML authentication response received without SAMLResponse data")
      }
    }


    "return Unauthorised if the post contains an empty saml response" in new TestCase {

      val result = loginController.idaLogin()(FakeRequest(POST, "/ida/login").withFormUrlEncodedBody(("SAMLResponse", "")))

      status(result) shouldBe 401
      contentAsString(result) should include("There was a problem signing you in")

      whenReady (result) { _=>
        expectALoginFailedAuditEventFor("IDA Login", "SAML authentication response received without SAMLResponse data")
      }
    }

    "return Unauthorised if the saml response fails validation" in new TestCase {

      when(mockSamlConnector.validate(Matchers.eq(samlResponse))(Matchers.any[HeaderCarrier])).thenReturn(AuthResponseValidationResult(valid = false, None, Some(originalRequestId)))

      val result = loginController.idaLogin()(FakeRequest(POST, "/ida/login").withFormUrlEncodedBody(("SAMLResponse", samlResponse)))

      status(result) shouldBe 401
      contentAsString(result) should include("There was a problem signing you in")

      whenReady (result) { _=>
        expectALoginFailedAuditEventFor("IDA Login", "SAMLResponse failed validation")
      }
    }

    "return Unauthorised if there is no Authority record matching the hash pid" in new TestCase {

      when(mockSamlConnector.validate(Matchers.eq(samlResponse))(Matchers.any[HeaderCarrier])).thenReturn(Future.successful(AuthResponseValidationResult(valid = true, Some(hashPid), Some(originalRequestId))))

      when(mockAuthConnector.authorityByPidAndUpdateLoginTime(Matchers.eq(hashPid))(Matchers.any[HeaderCarrier])).thenReturn(Future.successful(None))

      val result = loginController.idaLogin()(FakeRequest(POST, "/ida/login").withFormUrlEncodedBody(("SAMLResponse", samlResponse)))

      status(result) shouldBe 401
      contentAsString(result) should include("There was a problem signing you in")

      whenReady (result) { _=>
        expectALoginFailedAuditEventFor("IDA Login", "No record found in Auth for the PID")
      }
    }
  }

  "Attempting to log in to SA via Government Gateway Geoff Fisher" should {

    object geoff {
      val governmentGatewayUserId = "805933359724"
      val password = "passw0rd"
      val nameFromGovernmentGateway = "Geoff G.G.W. Nott-Fisher"
      val userId = "/auth/oid/notGeoff"
      val encodedGovernmentGatewayToken = GatewayToken("someencodedtoken", DateTimeUtils.now, DateTimeUtils.now)
    }

    "see the login form asking for his Government Gateway user id and password" in new WithSetup {

      val result = loginController.businessTaxLogin(FakeRequest())

      status(result) shouldBe OK
      contentType(result).get shouldBe "text/html"
      charset(result).get shouldBe "utf-8"
      contentAsString(result) should include("form")
      contentAsString(result) should include("Username")
      contentAsString(result) should include("Password")
      contentAsString(result) should include("Sign in")
      contentAsString(result) should not include "Invalid"
    }

    "not be able to log in and should return to the login form with an error message if he submits an empty Government Gateway user id" in new WithSetup {
      val result = loginController.governmentGatewayLogin(FakeRequest().withFormUrlEncodedBody("userId" -> "", "password" -> geoff.password))

      status(result) shouldBe OK
      contentAsString(result) should include("form")
      contentAsString(result) should include("Username")
      contentAsString(result) should include("Invalid Username: This field is required")
      contentAsString(result) should not include "Invalid Password"

      session(result).get(SessionKeys.userId) shouldBe None
      verifyZeroInteractions(mockGovernmentGatewayConnector)
    }

    "not be able to log in and should return to the login form with an error message if he submits an empty Government Gateway password" in new WithSetup {

      val result = loginController.governmentGatewayLogin(FakeRequest().withFormUrlEncodedBody("userId" -> geoff.governmentGatewayUserId, "password" -> ""))

      status(result) shouldBe OK
      contentAsString(result) should include("Password")
      contentAsString(result) should include("Invalid Password: This field is required")
      contentAsString(result) should not include "Invalid User ID"

      session(result).get(SessionKeys.userId) shouldBe None
      verifyZeroInteractions(mockGovernmentGatewayConnector)
    }

    "not be able to log in and should return to the login form with an error message on submitting invalid Government Gateway credentials" in new WithSetup {

      val mockResponse = mock[Response]
      when(mockGovernmentGatewayConnector.login(Matchers.eq(Credentials(geoff.governmentGatewayUserId, geoff.password)))(Matchers.any[HeaderCarrier])).thenReturn(Future.failed(UnauthorizedException("Unauthenticated request", mockResponse)))

      val result = loginController.governmentGatewayLogin(FakeRequest().withFormUrlEncodedBody("userId" -> geoff.governmentGatewayUserId, "password" -> geoff.password))

      status(result) shouldBe 401
      contentAsString(result) should include("form")
      contentAsString(result) should include("Invalid username or password. Try again.")

      session(result).get(SessionKeys.userId) shouldBe None

      expectALoginFailedAuditEventFor("GG Login", "Invalid Credentials")

    }

    "not be able to log in and should return to the login form with an error message on submitting valid Government Gateway credentials but not on the whitelist" in new WithSetup {

      val mockResponse = mock[Response]
      when(mockGovernmentGatewayConnector.login(Matchers.eq(Credentials(geoff.governmentGatewayUserId, geoff.password)))(Matchers.any[HeaderCarrier])).thenReturn(Future.failed(ForbiddenException("Not authorised to make this request", mockResponse)))
      when(mockBusinessTaxPages.notOnBusinessTaxWhitelistPage).thenReturn("<html>NOT IN WHITELIST</html>")

      val result = loginController.governmentGatewayLogin(FakeRequest().withFormUrlEncodedBody("userId" -> geoff.governmentGatewayUserId, "password" -> geoff.password))

      status(result) shouldBe 403
      session(result).get(SessionKeys.userId) shouldBe None
      contentAsString(result) should include("NOT IN WHITELIST")

      expectALoginFailedAuditEventFor("GG Login", "Not on the whitelist")
    }

    "be redirected to his SA homepage on submitting valid Government Gateway credentials with a cookie set containing his Government Gateway name and generate an audit event" in new WithSetup {

      when(mockGovernmentGatewayConnector.login(Matchers.eq(Credentials(geoff.governmentGatewayUserId, geoff.password)))(Matchers.any[HeaderCarrier])).thenReturn(GovernmentGatewayResponse(geoff.userId, geoff.nameFromGovernmentGateway, "affinityGroup", geoff.encodedGovernmentGatewayToken))

      val result = loginController.governmentGatewayLogin(FakeRequest().withFormUrlEncodedBody("userId" -> geoff.governmentGatewayUserId, "password" -> geoff.password))

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).get shouldBe FrontEndRedirect.businessTaxHome

      val sess = session(result)
      sess(SessionKeys.name) shouldBe geoff.nameFromGovernmentGateway
      sess(SessionKeys.userId) shouldBe geoff.userId
      sess(SessionKeys.token) shouldBe geoff.encodedGovernmentGatewayToken.encodeBase64

      val captor = ArgumentCaptor.forClass(classOf[AuditEvent])
      verify(mockAuditConnector).audit(captor.capture())(Matchers.any())

      val event = captor.getValue

      event.auditSource should be ("frontend")
      event.auditType should be ("TxSucceeded")
      event.tags should (
        contain ("transactionName" -> "GG Login") and
        contain key "X-Request-ID" and
        contain key "X-Session-ID"
      )
      event.detail should contain ("authId" -> geoff.userId)
    }
  }

  "Calling logout" should {

    "remove your existing session cookie and redirect you to the portal logout page" in new WithSetup(additionalConfiguration = Map("application.secret" -> "secret")) {

      val result = loginController.logout(FakeRequest().withSession("someKey" -> "someValue"))

      status(result) shouldBe Status.SEE_OTHER

      redirectLocation(result).get shouldBe "http://localhost:8080/ssoin/logout"

      val playSessionCookie = cookies(result).get("mdtp")

      playSessionCookie shouldBe None
    }

    "just redirect you to the portal logout page if you do not have a session cookie" in new WithSetup(additionalConfiguration = Map("application.secret" -> "secret")) {

      val result = loginController.logout(FakeRequest())

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).get shouldBe "http://localhost:8080/ssoin/logout"

      val playSessionCookie = cookies(result).get("mdtp")

      playSessionCookie shouldBe None
    }
  }

  "Calling logged out" should {
    "return the logged out view and clear any session data" in new WithSetup(additionalConfiguration = Map("application.secret" -> "secret")) {
      val result = loginController.loggedout(FakeRequest().withSession("someKey" -> "someValue"))

      status(result) shouldBe 200
      contentAsString(result) should include("signed out")

      val sess = session(result)
      sess.get("someKey") shouldBe None
    }
  }
}

