package controllers

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.saml.SamlMicroService
import org.mockito.Mockito._
import play.api.test.{ WithApplication, FakeRequest }
import uk.gov.hmrc.common.microservice.auth.AuthMicroService
import uk.gov.hmrc.common.microservice.governmentgateway.GovernmentGatewayMicroService
import play.api.http._
import uk.gov.hmrc.common.microservice.MockMicroServicesForTests
import controllers.common._
import uk.gov.hmrc.common.BaseSpec
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import uk.gov.hmrc.microservice.saml.domain.AuthRequestFormData
import uk.gov.hmrc.common.microservice.governmentgateway.GovernmentGatewayResponse
import uk.gov.hmrc.microservice.{ForbiddenException, UnauthorizedException}
import play.api.libs.ws.Response
import scala.Some
import uk.gov.hmrc.microservice.saml.domain.AuthResponseValidationResult
import uk.gov.hmrc.common.microservice.auth.domain.Regimes
import uk.gov.hmrc.common.microservice.governmentgateway.Credentials
import play.api.test.FakeApplication
import play.api.templates.Html

class LoginControllerSpec extends BaseSpec with MockitoSugar with CookieEncryption {

  import play.api.test.Helpers._

  abstract class WithSetup(additionalConfiguration: Map[String, _] = Map.empty) extends WithApplication(FakeApplication(additionalConfiguration = additionalConfiguration)) {
    lazy val mockSamlMicroService = mock[SamlMicroService]
    lazy val mockAuthMicroService = mock[AuthMicroService]
    lazy val mockGovernmentGatewayMicroService = mock[GovernmentGatewayMicroService]
    lazy val mockBusinessTaxPages = mock[BusinessTaxPages]

    when(mockSamlMicroService.create).thenReturn(
      AuthRequestFormData("http://www.ida.gov.uk/saml", "0987654321")
    )

    lazy val loginController = new LoginController with MockMicroServicesForTests {
      override lazy val samlMicroService = mockSamlMicroService
      override lazy val authMicroService = mockAuthMicroService
      override lazy val governmentGatewayMicroService = mockGovernmentGatewayMicroService

      override def notOnBusinessTaxWhitelistPage = {
        Html(mockBusinessTaxPages.notOnBusinessTaxWhitelistPage)
      }
    }

    val originalRequestId = "govuk-tax-325-235235-23523"
  }

  trait BusinessTaxPages {
    def notOnBusinessTaxWhitelistPage: String
  }

  "Login controller GET /login" should {
    "forward to the login page" in new WithSetup {
      val result = loginController.login()(FakeRequest())

      status(result) should be(200)
      contentAsString(result) should include("href=\"/samllogin\"")
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
      println("Encrypted cookie:" + enc)
    }
  }

  "Login controller POST /ida/login" should {

    val samlResponse = "98ewgiher9t8ho4fh4hfgo48whfkw4h8o"

    val hashPid = "09weu03t8e4gfo8"

    val oid = "0943809346039"
    val id = s"/auth/oid/$oid"

    "redirect to the home page if the response is valid and not registering an agent" in new WithSetup {

      when(mockSamlMicroService.validate(samlResponse)).thenReturn(AuthResponseValidationResult(valid = true, Some(hashPid), Some(originalRequestId)))

      when(mockAuthMicroService.authorityByPidAndUpdateLoginTime(hashPid)).thenReturn(Some(UserAuthority(id, Regimes(), None)))

      val result = loginController.idaLogin()(FakeRequest(POST, "/ida/login").withFormUrlEncodedBody(("SAMLResponse", samlResponse)))

      verify(mockAuthMicroService).authorityByPidAndUpdateLoginTime(hashPid)

      status(result) should be(303)
      redirectLocation(result).get should be("/paye/home")

      val sess = session(result)
      decrypt(sess("userId")) should be(id)
    }

    "redirect to the agent contact details if it s registering an agent" in new WithSetup {

      when(mockSamlMicroService.validate(samlResponse)).thenReturn(AuthResponseValidationResult(valid = true, Some(hashPid), Some(originalRequestId)))

      when(mockAuthMicroService.authorityByPidAndUpdateLoginTime(hashPid)).thenReturn(Some(UserAuthority(id, Regimes(), None)))

      val result = loginController.idaLogin()(FakeRequest(POST, "/ida/login").withFormUrlEncodedBody(("SAMLResponse", samlResponse)).withSession("login_redirect" -> "/agent/home"))

      verify(mockAuthMicroService).authorityByPidAndUpdateLoginTime(hashPid)

      status(result) should be(303)

      session(result).get("login_redirect") shouldBe None
      redirectLocation(result).get should be("/agent/home")

    }

    "return Unauthorised if the post does not contain a saml response" in new WithSetup {

      val result = loginController.idaLogin()(FakeRequest(POST, "/ida/login").withFormUrlEncodedBody(("Noddy", "BigEars")))

      status(result) should be(401)
      contentAsString(result) should include("Login error")
    }

    "return Unauthorised if the post contains an empty saml response" in new WithSetup {

      val result = loginController.idaLogin()(FakeRequest(POST, "/ida/login").withFormUrlEncodedBody(("SAMLResponse", "")))

      status(result) should be(401)
      contentAsString(result) should include("Login error")
    }

    "return Unauthorised if the saml response fails validation" in new WithSetup {

      when(mockSamlMicroService.validate(samlResponse)).thenReturn(AuthResponseValidationResult(valid = false, None, Some(originalRequestId)))

      val result = loginController.idaLogin()(FakeRequest(POST, "/ida/login").withFormUrlEncodedBody(("SAMLResponse", samlResponse)))

      status(result) should be(401)
      contentAsString(result) should include("Login error")
    }

    "return Unauthorised if there is no Authority record matching the hash pid" in new WithSetup {

      when(mockSamlMicroService.validate(samlResponse)).thenReturn(AuthResponseValidationResult(valid = true, Some(hashPid), Some(originalRequestId)))

      when(mockAuthMicroService.authorityByPidAndUpdateLoginTime(hashPid)).thenReturn(None)

      val result = loginController.idaLogin()(FakeRequest(POST, "/ida/login").withFormUrlEncodedBody(("SAMLResponse", samlResponse)))

      verify(mockAuthMicroService).authorityByPidAndUpdateLoginTime(hashPid)

      status(result) should be(401)
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
      val encodedGovernmentGatewayToken = "someencodedtoken"
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
      verifyZeroInteractions(mockGovernmentGatewayMicroService)
    }

    "not be able to log in and should return to the login form with an error message if he submits an empty Government Gateway password" in new WithSetup {

      val result = loginController.governmentGatewayLogin(FakeRequest().withFormUrlEncodedBody("userId" -> geoff.governmentGatewayUserId, "password" -> ""))

      status(result) shouldBe OK
      contentAsString(result) should include("Password")
      contentAsString(result) should include("Invalid Password: This field is required")
      contentAsString(result) should not include "Invalid User ID"

      session(result).get("userId") shouldBe None
      verifyZeroInteractions(mockGovernmentGatewayMicroService)

    }

    "not be able to log in and should return to the login form with an error message on submitting invalid Government Gateway credentials" in new WithSetup {

      val mockResponse = mock[Response]
      when(mockGovernmentGatewayMicroService.login(Credentials(geoff.governmentGatewayUserId, geoff.password))).thenThrow(UnauthorizedException("Unauthenticated request", mockResponse))

      val result = loginController.governmentGatewayLogin(FakeRequest().withFormUrlEncodedBody("userId" -> geoff.governmentGatewayUserId, "password" -> geoff.password))

      status(result) shouldBe 401
      contentAsString(result) should include("form")
      contentAsString(result) should include("Invalid User ID or Password")

      session(result).get("userId") shouldBe None
    }

    "not be able to log in and should return to the login form with an error message on submitting valid Government Gateway credentials but not on the whitelist" in new WithSetup {

      val mockResponse = mock[Response]
      when(mockGovernmentGatewayMicroService.login(Credentials(geoff.governmentGatewayUserId, geoff.password))).thenThrow(ForbiddenException("Not authorised to make this request", mockResponse))
      when(mockBusinessTaxPages.notOnBusinessTaxWhitelistPage).thenReturn("<html>NOT IN WHITELIST</html>")

      val result = loginController.governmentGatewayLogin(FakeRequest().withFormUrlEncodedBody("userId" -> geoff.governmentGatewayUserId, "password" -> geoff.password))

      status(result) shouldBe 403
      session(result).get("userId") shouldBe None
      contentAsString(result) should include("NOT IN WHITELIST")
    }

    "be redirected to his SA homepage on submitting valid Government Gateway credentials with a cookie set containing his Government Gateway name" in new WithSetup {

      when(mockGovernmentGatewayMicroService.login(Credentials(geoff.governmentGatewayUserId, geoff.password))).thenReturn(GovernmentGatewayResponse(geoff.userId, geoff.nameFromGovernmentGateway, geoff.affinityGroup, geoff.encodedGovernmentGatewayToken))

      val result = loginController.governmentGatewayLogin(FakeRequest().withFormUrlEncodedBody("userId" -> geoff.governmentGatewayUserId, "password" -> geoff.password))

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).get shouldBe FrontEndRedirect.businessTaxHome.toString

      val sess = session(result)
      decrypt(sess("name")) shouldBe geoff.nameFromGovernmentGateway
      decrypt(sess("userId")) shouldBe geoff.userId
      decrypt(sess("token")) shouldBe geoff.encodedGovernmentGatewayToken
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

      status(result) should be(200)
      contentAsString(result) should include("logged out")

      val sess = session(result)
      sess.get("someKey") shouldBe None
    }
  }
}

