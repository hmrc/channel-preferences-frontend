package controllers

import test.BaseSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import microservice.saml.SamlMicroService
import org.mockito.Mockito._
import microservices.MockMicroServicesForTests
import play.api.test.{ WithApplication, FakeRequest }
import microservice.auth.AuthMicroService
import microservice.ggw.{ GovernmentGatewayResponse, GgwMicroService, Credentials }
import play.api.http._
import org.scalatest.BeforeAndAfterEach
import microservice.auth.domain.UserAuthority
import microservice.saml.domain.AuthRequestFormData
import microservice.UnauthorizedException
import scala.Some
import microservice.saml.domain.AuthResponseValidationResult
import play.api.test.FakeApplication

class LoginControllerSpec extends BaseSpec with ShouldMatchers with MockitoSugar with CookieEncryption with BeforeAndAfterEach {

  import play.api.test.Helpers._

  private val mockSamlMicroService = mock[SamlMicroService]

  private val mockAuthMicroService = mock[AuthMicroService]
  private var mockGgwMicroService = mock[GgwMicroService]

  when(mockSamlMicroService.create).thenReturn(
    AuthRequestFormData("http://www.ida.gov.uk/saml", "0987654321")
  )

  lazy val loginController = new LoginController with MockMicroServicesForTests {
    override val samlMicroService = mockSamlMicroService
    override val authMicroService = mockAuthMicroService
    override val ggwMicroService = mockGgwMicroService
  }

  override def beforeEach() {
    reset(mockGgwMicroService) //todo instead of resetting mocks it would be better to set a new one up before each test
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

      when(mockAuthMicroService.authority(s"/auth/pid/$hashPid")).thenReturn(Some(UserAuthority(id, Map.empty, None)))

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

    val ggwUserId = "805933359724"
    val ggwPassword = "passw0rd"

    "see the login form asking for his GGW user id and password" in new WithApplication(FakeApplication()) {

      val response = route(FakeRequest(GET, "/sa/login"))

      response match {
        case Some(result) =>
          status(result) shouldBe OK
          contentType(result).get shouldBe "text/html"
          charset(result).get shouldBe "utf-8"
          contentAsString(result) should include("form")
          contentAsString(result) should include("Government Gateway user ID")
          contentAsString(result) should include("Government Gateway password")
          contentAsString(result) should include("Log in")
          contentAsString(result) should not include ("Invalid")
        case _ => fail("no response from /sa/login")
      }
    }

    "not be able to log in and should return to the login form with an error message if he submits an empty GGW user id" in new WithApplication(FakeApplication()) {
      val result = loginController.ggwLogin(FakeRequest().withFormUrlEncodedBody("userId" -> "", "password" -> ggwPassword))

      status(result) shouldBe OK
      contentAsString(result) should include("form")
      contentAsString(result) should include("Government Gateway user ID")
      contentAsString(result) should include("Invalid user ID: This field is required")
      contentAsString(result) should not include ("Invalid password")

      session(result).get("userId") shouldBe None
      verifyZeroInteractions(mockGgwMicroService)
    }

    "not be able to log in and should return to the login form with an error message if he submits an empty GGW password" in new WithApplication(FakeApplication()) {

      val result = loginController.ggwLogin(FakeRequest().withFormUrlEncodedBody("userId" -> ggwUserId, "password" -> ""))

      status(result) shouldBe OK
      contentAsString(result) should include("Government Gateway password")
      contentAsString(result) should include("Invalid password: This field is required")
      contentAsString(result) should not include ("Invalid user ID")

      session(result).get("userId") shouldBe None
      verifyZeroInteractions(mockGgwMicroService)

    }

    "not be able to log in and should return to the login form with an error message on submitting invalid GGW credentials" in new WithApplication(FakeApplication()) {

      when(mockGgwMicroService.login(Credentials(ggwUserId, ggwPassword))).thenThrow(UnauthorizedException("Unauthenticated request"))

      val result = loginController.ggwLogin(FakeRequest().withFormUrlEncodedBody("userId" -> ggwUserId, "password" -> ggwPassword))

      status(result) shouldBe OK
      contentAsString(result) should include("form")
      contentAsString(result) should include("Invalid user ID or password")

      session(result).get("userId") shouldBe None
    }

    "be redirected to his SA homepage on submitting valid GGW credentials with a cookie set containing his GGW name" in new WithApplication(FakeApplication()) {

      val ggwName = "Geoff G.G.W. Nott-Fisher"
      val authId = "/auth/oid/notGeoff"

      when(mockGgwMicroService.login(Credentials(ggwUserId, ggwPassword))).thenReturn(GovernmentGatewayResponse(authId, ggwName))

      val result = loginController.ggwLogin(FakeRequest().withFormUrlEncodedBody("userId" -> ggwUserId, "password" -> ggwPassword))

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).get shouldBe routes.SaController.home().toString()

      val sess = session(result)
      sess("ggwName") shouldBe ggwName
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
