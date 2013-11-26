package controllers

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.saml.SamlConnector
import org.mockito.Mockito._
import play.api.test.{ WithApplication, FakeRequest }
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.governmentgateway.{GatewayToken, GovernmentGatewayConnector, GovernmentGatewayResponse, Credentials}
import play.api.http._
import controllers.common._
import uk.gov.hmrc.common.BaseSpec
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import uk.gov.hmrc.microservice.saml.domain.AuthRequestFormData
import uk.gov.hmrc.microservice.{ForbiddenException, UnauthorizedException}
import play.api.libs.ws.Response
import scala.Some
import uk.gov.hmrc.microservice.saml.domain.AuthResponseValidationResult
import uk.gov.hmrc.common.microservice.auth.domain.Regimes
import play.api.test.FakeApplication
import play.api.templates.Html
import uk.gov.hmrc.utils.DateTimeUtils
import org.mockito.{ArgumentCaptor, Matchers}
import controllers.common.actions.HeaderCarrier
import scalaz.Alpha.M
import uk.gov.hmrc.common.microservice.audit.{AuditEvent, AuditConnector}

class LoginControllerSpec extends BaseSpec with MockitoSugar with CookieEncryption {

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

  "LoginController " should {
    "encrypt cookie value" in new WithSetup {
      val enc = loginController.encrypt("/auth/oid/9875928746298467209348650298847235")
      enc should not include "/auth/oid/9875928746298467209348650298847235"
    }
  }

  "Login controller POST /ida/login" should {

    val samlResponse = "98ewgiher9t8ho4fh4hfgo48whfkw4h8o"

    val hashPid = "09weu03t8e4gfo8"

    val oid = "0943809346039"
    val id = s"/auth/oid/$oid"

    abstract class TestCase extends WithSetup {
      def setupValidRequest() = {
        when(mockSamlConnector.validate(Matchers.eq(samlResponse))(Matchers.any[HeaderCarrier])).thenReturn(AuthResponseValidationResult(valid = true, Some(hashPid), Some(originalRequestId)))

        when(mockAuthConnector.authorityByPidAndUpdateLoginTime(Matchers.eq(hashPid))(Matchers.any[HeaderCarrier])).thenReturn(Some(UserAuthority(id, Regimes(), None)))
        FakeRequest(POST, "/ida/login").withFormUrlEncodedBody(("SAMLResponse", samlResponse))
      }
    }

    "redirect to the home page if the response is valid and not registering an agent" in new TestCase {
      val result = loginController.idaLogin()(setupValidRequest())

      status(result) shouldBe(303)
      redirectLocation(result).get shouldBe FrontEndRedirect.payeHome

      val sess = session(result)
      decrypt(sess("userId")) shouldBe id

      verify(mockAuditConnector).audit(Matchers.any())(Matchers.any())
    }

    "redirect to the agent contact details if it s registering an agent" in new TestCase {
      val result = loginController.idaLogin()(setupValidRequest.withSession("login_redirect" -> "/agent/home"))

      status(result) shouldBe 303

      session(result).get("login_redirect") shouldBe None
      redirectLocation(result).get shouldBe "/agent/home"
    }

    "generate an audit event if the response is valid" in new TestCase {
      loginController.idaLogin()(setupValidRequest())

      val captor = ArgumentCaptor.forClass(classOf[AuditEvent])
      verify(mockAuditConnector).audit(captor.capture())(Matchers.any())

      val event = captor.getValue

      event.auditSource should be ("frontend")
      event.auditType should be ("TxSucceded")
      event.tags should (
        contain ("transactionName" -> "IDA Login Completion") and
        contain ("X-Request-ID" -> originalRequestId) and
        contain key ("X-Request-ID-Original") and
        contain key ("X-Session-ID")
      )
      event.detail should (
        contain ("hashPid" -> hashPid) and
        contain ("authId" -> id)
      )
    }

    "return Unauthorised if the post does not contain a saml response" in new WithSetup {

      val result = loginController.idaLogin()(FakeRequest(POST, "/ida/login").withFormUrlEncodedBody(("Noddy", "BigEars")))

      status(result) shouldBe(401)
      contentAsString(result) should include("Login error")
    }

    "return Unauthorised if the post contains an empty saml response" in new WithSetup {

      val result = loginController.idaLogin()(FakeRequest(POST, "/ida/login").withFormUrlEncodedBody(("SAMLResponse", "")))

      status(result) shouldBe 401
      contentAsString(result) should include("Login error")
    }

    "return Unauthorised if the saml response fails validation" in new WithSetup {

      when(mockSamlConnector.validate(Matchers.eq(samlResponse))(Matchers.any[HeaderCarrier])).thenReturn(AuthResponseValidationResult(valid = false, None, Some(originalRequestId)))

      val result = loginController.idaLogin()(FakeRequest(POST, "/ida/login").withFormUrlEncodedBody(("SAMLResponse", samlResponse)))

      status(result) shouldBe 401
      contentAsString(result) should include("Login error")
    }

    "return Unauthorised if there is no Authority record matching the hash pid" in new WithSetup {

      when(mockSamlConnector.validate(Matchers.eq(samlResponse))(Matchers.any[HeaderCarrier])).thenReturn(AuthResponseValidationResult(valid = true, Some(hashPid), Some(originalRequestId)))

      when(mockAuthConnector.authorityByPidAndUpdateLoginTime(hashPid)).thenReturn(None)

      val result = loginController.idaLogin()(FakeRequest(POST, "/ida/login").withFormUrlEncodedBody(("SAMLResponse", samlResponse)))

      status(result) shouldBe 401
      contentAsString(result) should include("Login error")
    }
  }

  "Attempting to log in to SA via Government Gateway Geoff Fisher" should {

    object geoff {
      val governmentGatewayUserId = "805933359724"
      val password = "passw0rd"
      val nameFromGovernmentGateway = "Geoff G.G.W. Nott-Fisher"
      val userId = "/auth/oid/notGeoff"
      val affinityGroup = "Organisation"
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

      session(result).get("userId") shouldBe None
      verifyZeroInteractions(mockGovernmentGatewayConnector)
    }

    "not be able to log in and should return to the login form with an error message if he submits an empty Government Gateway password" in new WithSetup {

      val result = loginController.governmentGatewayLogin(FakeRequest().withFormUrlEncodedBody("userId" -> geoff.governmentGatewayUserId, "password" -> ""))

      status(result) shouldBe OK
      contentAsString(result) should include("Password")
      contentAsString(result) should include("Invalid Password: This field is required")
      contentAsString(result) should not include "Invalid User ID"

      session(result).get("userId") shouldBe None
      verifyZeroInteractions(mockGovernmentGatewayConnector)

    }

    "not be able to log in and should return to the login form with an error message on submitting invalid Government Gateway credentials" in new WithSetup {

      val mockResponse = mock[Response]
      when(mockGovernmentGatewayConnector.login(Matchers.eq(Credentials(geoff.governmentGatewayUserId, geoff.password)))(Matchers.any[HeaderCarrier])).thenThrow(UnauthorizedException("Unauthenticated request", mockResponse))

      val result = loginController.governmentGatewayLogin(FakeRequest().withFormUrlEncodedBody("userId" -> geoff.governmentGatewayUserId, "password" -> geoff.password))

      status(result) shouldBe 401
      contentAsString(result) should include("form")
      contentAsString(result) should include("Invalid username or password. Try again.")

      session(result).get("userId") shouldBe None
    }

    "not be able to log in and should return to the login form with an error message on submitting valid Government Gateway credentials but not on the whitelist" in new WithSetup {

      val mockResponse = mock[Response]
      when(mockGovernmentGatewayConnector.login(Matchers.eq(Credentials(geoff.governmentGatewayUserId, geoff.password)))(Matchers.any[HeaderCarrier])).thenThrow(ForbiddenException("Not authorised to make this request", mockResponse))
      when(mockBusinessTaxPages.notOnBusinessTaxWhitelistPage).thenReturn("<html>NOT IN WHITELIST</html>")

      val result = loginController.governmentGatewayLogin(FakeRequest().withFormUrlEncodedBody("userId" -> geoff.governmentGatewayUserId, "password" -> geoff.password))

      status(result) shouldBe 403
      session(result).get("userId") shouldBe None
      contentAsString(result) should include("NOT IN WHITELIST")
    }

    "be redirected to his SA homepage on submitting valid Government Gateway credentials with a cookie set containing his Government Gateway name" in new WithSetup {

      when(mockGovernmentGatewayConnector.login(Matchers.eq(Credentials(geoff.governmentGatewayUserId, geoff.password)))(Matchers.any[HeaderCarrier])).thenReturn(GovernmentGatewayResponse(geoff.userId, geoff.nameFromGovernmentGateway, geoff.affinityGroup, geoff.encodedGovernmentGatewayToken))

      val result = loginController.governmentGatewayLogin(FakeRequest().withFormUrlEncodedBody("userId" -> geoff.governmentGatewayUserId, "password" -> geoff.password))

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).get shouldBe FrontEndRedirect.businessTaxHome

      val sess = session(result)
      decrypt(sess("name")) shouldBe geoff.nameFromGovernmentGateway
      decrypt(sess("userId")) shouldBe geoff.userId
      decrypt(sess("token")) shouldBe geoff.encodedGovernmentGatewayToken.encodeBase64
      decrypt(sess("affinityGroup")) shouldBe geoff.affinityGroup

    }
  }

  "Calling logout" should {

    "remove your existing session cookie and redirect you to the portal logout page" in new WithSetup(additionalConfiguration = Map("application.secret" -> "secret")) {

      val result = loginController.logout(FakeRequest().withSession("someKey" -> "someValue"))

      status(result) shouldBe Status.SEE_OTHER

      redirectLocation(result).get shouldBe "http://localhost:8080/ssoin/logout"

      val playSessionCookie = cookies(result).get("PLAY_SESSION")

      playSessionCookie shouldBe None
    }

    "just redirect you to the portal logout page if you do not have a session cookie" in new WithSetup(additionalConfiguration = Map("application.secret" -> "secret")) {

      val result = loginController.logout(FakeRequest())

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).get shouldBe "http://localhost:8080/ssoin/logout"

      val playSessionCookie = cookies(result).get("PLAY_SESSION")

      playSessionCookie shouldBe None
    }
  }

  "Calling logged out" should {
    "return the logged out view and clear any session data" in new WithSetup(additionalConfiguration = Map("application.secret" -> "secret")) {
      val result = loginController.loggedout(FakeRequest().withSession("someKey" -> "someValue"))

      status(result) shouldBe(200)
      contentAsString(result) should include("signed out")

      val sess = session(result)
      sess.get("someKey") shouldBe None
    }
  }
}

