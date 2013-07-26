package controllers

import test.BaseSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import microservice.saml.SamlMicroService
import org.mockito.Mockito._
import microservice.MockMicroServicesForTests
import play.api.test.{ WithApplication, FakeRequest }
import microservice.auth.AuthMicroService
import microservice.governmentgateway.{ GovernmentGatewayResponse, GovernmentGatewayMicroService, Credentials }
import play.api.http._
import org.scalatest.BeforeAndAfterEach
import microservice.auth.domain.{ Regimes, UserAuthority }
import microservice.saml.domain.AuthRequestFormData
import microservice.UnauthorizedException
import scala.Some
import microservice.saml.domain.AuthResponseValidationResult
import play.api.test.FakeApplication

class LoginControllerSpec extends BaseSpec with ShouldMatchers with MockitoSugar with CookieEncryption with BeforeAndAfterEach {

  import play.api.test.Helpers._

  private val mockSamlMicroService = mock[SamlMicroService]

  private val mockAuthMicroService = mock[AuthMicroService]
  private var mockGovernmentGatewayMicroService = mock[GovernmentGatewayMicroService]

  when(mockSamlMicroService.create).thenReturn(
    AuthRequestFormData("http://www.ida.gov.uk/saml", "0987654321")
  )

  lazy val loginController = new LoginController with MockMicroServicesForTests {
    override val samlMicroService = mockSamlMicroService
    override val authMicroService = mockAuthMicroService
    override val governmentGatewayMicroService = mockGovernmentGatewayMicroService
  }

  override def beforeEach() {
    reset(mockGovernmentGatewayMicroService) //todo instead of resetting mocks it would be better to set a new one up before each test
  }

  "Login controller GET /login" should {
    "forward to the login page" in new WithApplication(FakeApplication()) {
      val result = loginController.login()(FakeRequest())

      status(result) should be(200)
      contentAsString(result) should include("<a href=\"/samllogin\">here</a>")
    }
  }

  "Login controller GET /samllogin" should {
    "return a form that contains th§e data from the saml service" in new WithApplication(FakeApplication()) {
      val result = loginController.samlLogin()(FakeRequest())

      status(result) should be(200)
      contentAsString(result) should include("action=\"http://www.ida.gov.uk/saml\"")
      contentAsString(result) should include("value=\"0987654321\"")
    }
  }

  "LoginController " should {
    "encrypt cooke value" in new WithApplication(FakeApplication()) {
      val enc = loginController.encrypt("/auth/oid/9875928746298467209348650298847235")
      println("Encrypted cookie:" + enc)
    }
  }

  "Login controller POST /ida/login" should {

    val samlResponse = "98ewgiher9t8ho4fh4hfgo48whfkw4h8o"

    val hashPid = "09weu03t8e4gfo8"

    val id = "/auth/oid/0943809346039"

    "redirect to the home page if the response is valid" in new WithApplication(FakeApplication()) {

      when(mockSamlMicroService.validate(samlResponse)).thenReturn(AuthResponseValidationResult(true, Some(hashPid)))

      when(mockAuthMicroService.authority(s"/auth/pid/$hashPid")).thenReturn(Some(UserAuthority(id, Regimes(), None)))

      val result = loginController.idaLogin()(FakeRequest(POST, "/ida/login").withFormUrlEncodedBody(("SAMLResponse", samlResponse)))

      status(result) should be(303)
      redirectLocation(result).get should be("/home")

      val sess = session(result)
      decrypt(sess("userId")) should be(id)
    }

    "return Unauthorised if the post does not contain a saml response" in new WithApplication(FakeApplication()) {

      val result = loginController.idaLogin()(FakeRequest(POST, "/ida/login").withFormUrlEncodedBody(("Noddy", "BigEars")))

      status(result) should be(401)
    }

    "return Unauthorised if the post contains an empty saml response" in new WithApplication(FakeApplication()) {

      val result = loginController.idaLogin()(FakeRequest(POST, "/ida/login").withFormUrlEncodedBody(("SAMLResponse", "")))

      status(result) should be(401)
    }

    "return Unauthorised if the saml response fails validation" in new WithApplication(FakeApplication()) {

      when(mockSamlMicroService.validate(samlResponse)).thenReturn(AuthResponseValidationResult(false, None))

      val result = loginController.idaLogin()(FakeRequest(POST, "/ida/login").withFormUrlEncodedBody(("SAMLResponse", samlResponse)))

      status(result) should be(401)
    }

    "return Unauthorised if there is no Authority record matching the hash pid" in new WithApplication(FakeApplication()) {

      when(mockSamlMicroService.validate(samlResponse)).thenReturn(AuthResponseValidationResult(true, Some(hashPid)))

      when(mockAuthMicroService.authority(s"/auth/pid/$hashPid")).thenReturn(None)

      val result = loginController.idaLogin()(FakeRequest(POST, "/ida/login").withFormUrlEncodedBody(("SAMLResponse", samlResponse)))

      status(result) should be(401)
    }
  }

  "Attempting to log in to SA via Government Gateway Geoff Fisher" should {

    val userId = "805933359724"
    val password = "passw0rd"

    "see the login form asking for his Government Gateway user id and password" in new WithApplication(FakeApplication()) {

      val response = route(FakeRequest(GET, "/business-tax/login"))

      response match {
        case Some(result) =>
          status(result) shouldBe OK
          contentType(result).get shouldBe "text/html"
          charset(result).get shouldBe "utf-8"
          contentAsString(result) should include("form")
          contentAsString(result) should include("Government Gateway User ID")
          contentAsString(result) should include("Government Gateway Password")
          contentAsString(result) should include("Log in")
          contentAsString(result) should not include ("Invalid")
        case _ => fail("no response from /business-tax/login")
      }
    }

    "not be able to log in and should return to the login form with an error message if he submits an empty Government Gateway user id" in new WithApplication(FakeApplication()) {
      val result = loginController.governmentGatewayLogin(FakeRequest().withFormUrlEncodedBody("userId" -> "", "password" -> password))

      status(result) shouldBe OK
      contentAsString(result) should include("form")
      contentAsString(result) should include("Government Gateway User ID")
      contentAsString(result) should include("Invalid User ID: This field is required")
      contentAsString(result) should not include ("Invalid Password")

      session(result).get("userId") shouldBe None
      verifyZeroInteractions(mockGovernmentGatewayMicroService)
    }

    "not be able to log in and should return to the login form with an error message if he submits an empty Government Gateway password" in new WithApplication(FakeApplication()) {

      val result = loginController.governmentGatewayLogin(FakeRequest().withFormUrlEncodedBody("userId" -> userId, "password" -> ""))

      status(result) shouldBe OK
      contentAsString(result) should include("Government Gateway Password")
      contentAsString(result) should include("Invalid Password: This field is required")
      contentAsString(result) should not include ("Invalid User ID")

      session(result).get("userId") shouldBe None
      verifyZeroInteractions(mockGovernmentGatewayMicroService)

    }

    "not be able to log in and should return to the login form with an error message on submitting invalid Government Gateway credentials" in new WithApplication(FakeApplication()) {

      when(mockGovernmentGatewayMicroService.login(Credentials(userId, password))).thenThrow(UnauthorizedException("Unauthenticated request"))

      val result = loginController.governmentGatewayLogin(FakeRequest().withFormUrlEncodedBody("userId" -> userId, "password" -> password))

      status(result) shouldBe OK
      contentAsString(result) should include("form")
      contentAsString(result) should include("Invalid User ID or Password")

      session(result).get("userId") shouldBe None
    }

    "be redirected to his SA homepage on submitting valid Government Gateway credentials with a cookie set containing his Government Gateway name" in new WithApplication(FakeApplication()) {

      val nameFromGovernmentGateway = "Geoff G.G.W. Nott-Fisher"
      val authId = "/auth/oid/notGeoff"

      when(mockGovernmentGatewayMicroService.login(Credentials(userId, password))).thenReturn(GovernmentGatewayResponse(authId, nameFromGovernmentGateway))

      val result = loginController.governmentGatewayLogin(FakeRequest().withFormUrlEncodedBody("userId" -> userId, "password" -> password))

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).get shouldBe routes.BusinessTaxController.home().toString()

      val sess = session(result)
      sess("nameFromGovernmentGateway") shouldBe nameFromGovernmentGateway
      decrypt(sess("userId")) shouldBe authId

    }

  }

  //copied here from the service test
  "Logging in with username and password" should {

    "respond with 200 and an AuthorityResponse object containing the auth details user's name and the last login time if the login is successful" in {
      pending
    }

    "respond with 200 and an AuthorityResponse object containing the auth details and user's name but no last login time if the login is successful and this is the first time the user has logged in" in {
      pending
    }
  }

  //copied here from the service test
  "Logging-in multiple times with the same user" should {

    "return no last login time for the first login" in {
      pending // Checks that first time this isn't set.
    }

    "return last login time set to the first login time with the second login" in {
      pending // Checks that the last login time is set correctly the first time the user logs in.
    }

    "return last login time set to the second login time with the third login" in {
      pending // Checks that the last login time is correctly updated when the user logs in subsequently.
    }

    "return the user's name the first time they login" in {
      pending
    }

    "return the user's updated name upon the second login if they changed it on the Gateway before the second login" in {
      pending // This test will be possible with a stubbed gateway, but probably not with the real thing without getting into quite a lot of complexity
    }
  }
}
