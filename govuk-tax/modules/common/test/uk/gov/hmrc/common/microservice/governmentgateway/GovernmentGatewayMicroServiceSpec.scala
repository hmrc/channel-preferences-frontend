package uk.gov.hmrc.common.microservice.governmentgateway

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.BaseSpec
import org.mockito.Mockito._
import play.api.test.WithApplication
import play.api.test.FakeApplication
import scala.Some
import play.api.libs.json._
import uk.gov.hmrc.utils.DateTimeUtils

class TestGovernmentGatewayMicroService extends GovernmentGatewayMicroService with MockitoSugar {

  val httpWrapper = mock[HttpWrapper]

  override protected def httpGet[A](uri: String)(implicit m: Manifest[A]): Option[A] = {
    httpWrapper.get[A](uri)
  }

  override protected def httpPost[A](uri: String, body: JsValue, headers: Map[String, String])(implicit m: Manifest[A]): Option[A] = {
    httpWrapper.post[A](uri, body, headers)
  }

  class HttpWrapper {
    def get[T](uri: String): Option[T] = None
    def post[A](uri: String, body: JsValue, headers: Map[String, String] = Map.empty)(implicit m: Manifest[A]): Option[A] = None
  }

}

class GovernmentGatewayMicroServiceSpec extends BaseSpec with MockitoSugar {

  lazy val service = new TestGovernmentGatewayMicroService

  "login endpoint" should {

    "return a government gateway response when credentials are correct" in new WithApplication(FakeApplication()){

      val credentials = Credentials("user", "pw")
      val credentialsJson = JsObject(Seq("userId" -> JsString(credentials.userId), "password" -> JsString(credentials.password)))
      val expectedResponse = GovernmentGatewayResponse("/auth/oid/123456", "Tim Cook", "Individual", GatewayToken("12343534545454", DateTimeUtils.now, DateTimeUtils.now))
      when(service.httpWrapper.post[GovernmentGatewayResponse]("/login", credentialsJson, Map.empty)).thenReturn(Some(expectedResponse))

      val result = service.login(credentials)

      result shouldBe expectedResponse
    }

    "throw an exception when no government gateway details are retrieved from Government Gateway"  in new WithApplication(FakeApplication()){

      val credentials = Credentials("user", "incorrectPw")
      val credentialsJson = JsObject(Seq("userId" -> JsString(credentials.userId), "password" -> JsString(credentials.password)))
      when(service.httpWrapper.post[GovernmentGatewayResponse]("/login", credentialsJson, Map.empty)).thenReturn(None)

      intercept[IllegalStateException]{
        service.login(credentials)
      }

    }

  }

  "ssoLogin endpoint" should {

    "return a government gateway response when the sso login request is correct" in new WithApplication(FakeApplication()){
      
      val ssoLoginRequest = SsoLoginRequest("3jeih3g3gg3ljkdlh3", 3837636)
      val ssoLoginRequestJson = JsObject(Seq("token" -> JsString(ssoLoginRequest.token), "timestamp" -> JsNumber(ssoLoginRequest.timestamp)))
      val expectedResponse = GovernmentGatewayResponse("/auth/oid/123456", "Tim Cook", "Individual", GatewayToken("12343534545454", DateTimeUtils.now, DateTimeUtils.now))
      when(service.httpWrapper.post[GovernmentGatewayResponse]("/sso-login", ssoLoginRequestJson, Map.empty)).thenReturn(Some(expectedResponse))

      val result = service.ssoLogin(ssoLoginRequest)

      result shouldBe expectedResponse
      
    }

    "throw an exception when no government gateway details are retrieved from Government Gateway" in new WithApplication(FakeApplication()){

      val ssoLoginRequest = SsoLoginRequest("3jeih3g3gg3ljkdlh3", 3837636)
      val ssoLoginRequestJson = JsObject(Seq("token" -> JsString(ssoLoginRequest.token), "timestamp" -> JsNumber(ssoLoginRequest.timestamp)))
      when(service.httpWrapper.post[GovernmentGatewayResponse]("/sso-login", ssoLoginRequestJson, Map.empty)).thenReturn(None)

      intercept[IllegalStateException]{
        service.ssoLogin(ssoLoginRequest)
      }

    }

  }

  "profile endpoint" should {

    "return the user profile when details are found in government gateway microservice" in new WithApplication(FakeApplication()) {

      val userId = "/auth/oid/geofffisher"
      val expectedResponse = Some(JsonProfileResponse(affinityGroup = "Organisation", activeEnrolments = Set("HMCE-EBTI-ORG","HMRC-EMCS-ORG")))
      val expectedResult = Some(ProfileResponse(
        affinityGroup = AffinityGroup("organisation"),
        activeEnrolments = Set(
          Enrolment("HMCE-EBTI-ORG"),
          Enrolment("HMRC-EMCS-ORG"))))

      when(service.httpWrapper.get[JsonProfileResponse](s"/profile$userId")).thenReturn(expectedResponse)
      val result = service.profile(userId)
      result shouldBe expectedResult
    }

    "return None when profile for given user is not found" in new WithApplication(FakeApplication()) {

      val userId = "/auth/oid/missingUser"
      val expectedResponse = None

      when(service.httpWrapper.get[JsonProfileResponse](s"/profile$userId")).thenReturn(expectedResponse)
      val result = service.profile(userId)
      result shouldBe expectedResponse
    }

  }

}
