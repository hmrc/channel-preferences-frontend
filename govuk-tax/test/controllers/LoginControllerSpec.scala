package controllers

import test.BaseSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import microservice.saml.SamlMicroService
import org.mockito.Mockito.when
import microservice.saml.domain.{ AuthResponseValidationResult, AuthRequestFormData }
import microservices.MockMicroServicesForTests
import play.api.test.{ FakeApplication, WithApplication, FakeRequest }
import microservice.auth.AuthMicroService
import microservice.auth.domain.UserAuthority
import java.util

class LoginControllerSpec extends BaseSpec with ShouldMatchers with MockitoSugar with CookieEncryption {

  import play.api.test.Helpers._

  private val mockSamlMicroService = mock[SamlMicroService]

  private val mockAuthMicroService = mock[AuthMicroService]

  when(mockSamlMicroService.create).thenReturn(
    AuthRequestFormData("http://www.ida.gov.uk/saml", "0987654321")
  )

  lazy val loginController = new LoginController with MockMicroServicesForTests {
    override val samlMicroService = mockSamlMicroService
    override val authMicroService = mockAuthMicroService
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

      when(mockAuthMicroService.authority(s"/auth/pid/$hashPid")).thenReturn(Some(UserAuthority(id, Map.empty)))

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

  "On logging in to SA via Government Gateway Geoff Fisher" should {

    "be presented with a login form asking for his GGW user id and password" in new WithApplication(FakeApplication()) {

      val response = route(FakeRequest(GET, "/sa/login"))

      response match {
        case Some(result) =>
          status(result) shouldBe OK
          contentType(result).get shouldBe "text/html"
          charset(result).get shouldBe "utf-8"
          contentAsString(result) should include ("form")
          contentAsString(result) should include ("Government Gateway user ID")
          contentAsString(result) should include ("Government Gateway password")
          contentAsString(result) should include ("Log in")
        case _ => fail("no response to /sa/login")
      }

    }

    "see his SA homepage with his UTR and name returned from GGW after submitting valid GGW credentials and is authorized to see SA data" in {
      pending
    }

    "should see an error page if his login credentials are not valid" in {
      pending
    }

    "should see an error page if his login credentials are valid but he is not authorized to see SA data" in {
      pending
    }

  }
}
