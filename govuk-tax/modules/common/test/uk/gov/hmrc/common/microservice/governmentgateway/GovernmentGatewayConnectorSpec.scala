package uk.gov.hmrc.common.microservice.governmentgateway

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.BaseSpec
import org.mockito.Mockito._
import play.api.test.WithApplication
import play.api.test.FakeApplication
import scala.Some
import play.api.libs.json._
import uk.gov.hmrc.utils.DateTimeUtils
import controllers.common.actions.HeaderCarrier
import scala.concurrent.{Await, Future}
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.duration._

class TestGovernmentGatewayConnector extends GovernmentGatewayConnector with MockitoSugar {

  val httpWrapper = mock[HttpWrapper]

  override protected def httpGet[A](uri: String)(implicit m: Manifest[A], hc: HeaderCarrier): Option[A] = {
    httpWrapper.get[A](uri)
  }

  override protected def httpPostF[A](uri: String, body: JsValue, headers: Map[String, String])(implicit m: Manifest[A], headerCarrier:HeaderCarrier): Future[Option[A]] = {
    httpWrapper.postF[A](uri, body, headers)
  }


  override protected def httpGetF[A](uri: String)(implicit m: Manifest[A], headerCarrier: HeaderCarrier): Future[Option[A]] = {
    httpWrapper.getF[A](uri)
  }

  class HttpWrapper {
    def get[T](uri: String): Option[T] = None
    def getF[T](uri: String): Future[Option[T]] = Future.successful(None)
    def postF[A](uri: String, body: JsValue, headers: Map[String, String] = Map.empty)(implicit m: Manifest[A]): Future[Option[A]] = Future.successful(None)
  }

}

class GovernmentGatewayConnectorSpec extends BaseSpec with MockitoSugar with ScalaFutures {

  lazy val service = new TestGovernmentGatewayConnector

  "login endpoint" should {

    "return a government gateway response when credentials are correct" in new WithApplication(FakeApplication()){

      val credentials = Credentials("user", "pw")
      val credentialsJson = JsObject(Seq("userId" -> JsString(credentials.userId), "password" -> JsString(credentials.password)))
      val expectedResponse = GovernmentGatewayResponse("/auth/oid/123456", "Tim Cook", "Individual", GatewayToken("12343534545454", DateTimeUtils.now, DateTimeUtils.now))
      when(service.httpWrapper.postF[GovernmentGatewayResponse]("/login", credentialsJson, Map.empty)).thenReturn(Future.successful(Some(expectedResponse)))

      service.login(credentials).futureValue shouldBe expectedResponse
    }

    "throw an exception when no government gateway details are retrieved from Government Gateway"  in new WithApplication(FakeApplication()){

      val credentials = Credentials("user", "incorrectPw")
      val credentialsJson = JsObject(Seq("userId" -> JsString(credentials.userId), "password" -> JsString(credentials.password)))
      when(service.httpWrapper.postF[GovernmentGatewayResponse]("/login", credentialsJson, Map.empty)).thenReturn(Future.successful(None))

      service.login(credentials).failed.futureValue shouldBe an [IllegalStateException]
    }

  }

  "ssoLogin endpoint" should {

    "return a government gateway response when the sso login request is correct" in new WithApplication(FakeApplication()){
      
      val ssoLoginRequest = SsoLoginRequest("3jeih3g3gg3ljkdlh3", 3837636)
      val ssoLoginRequestJson = JsObject(Seq("token" -> JsString(ssoLoginRequest.token), "timestamp" -> JsNumber(ssoLoginRequest.timestamp)))
      val expectedResponse = GovernmentGatewayResponse("/auth/oid/123456", "Tim Cook", "Individual", GatewayToken("12343534545454", DateTimeUtils.now, DateTimeUtils.now))
      when(service.httpWrapper.postF[GovernmentGatewayResponse]("/sso-login", ssoLoginRequestJson, Map.empty)).thenReturn(Future.successful(Some(expectedResponse)))

      service.ssoLogin(ssoLoginRequest).futureValue shouldBe expectedResponse
    }

    "throw an exception when no government gateway details are retrieved from Government Gateway" in new WithApplication(FakeApplication()){

      val ssoLoginRequest = SsoLoginRequest("3jeih3g3gg3ljkdlh3", 3837636)
      val ssoLoginRequestJson = JsObject(Seq("token" -> JsString(ssoLoginRequest.token), "timestamp" -> JsNumber(ssoLoginRequest.timestamp)))
      when(service.httpWrapper.postF[GovernmentGatewayResponse]("/sso-login", ssoLoginRequestJson, Map.empty)).thenReturn(Future.successful(None))

      service.ssoLogin(ssoLoginRequest).failed.futureValue shouldBe an [IllegalStateException]
    }

  }

  "profile endpoint" should {

    "return the user profile when details are found in government gateway microservice" in new WithApplication(FakeApplication()) {

      val userId = "/auth/oid/geofffisher"
      val expectedResponse = Some(ProfileResponse("Organisation", activeEnrolments = List("HMCE-EBTI-ORG","HMRC-EMCS-ORG")))
      when(service.httpWrapper.getF[ProfileResponse](s"/profile$userId")).thenReturn(expectedResponse)
      val result = service.profile(userId)
      whenReady(result){_ shouldBe expectedResponse.get}
    }

    "return None when profile for given user is not found" in new WithApplication(FakeApplication()) {

      val userId = "/auth/oid/missingUser"
      val expectedResponse = None

      when(service.httpWrapper.getF[ProfileResponse](s"/profile$userId")).thenReturn(Future.successful(expectedResponse))
      intercept[RuntimeException] {
        Await.result(service.profile(userId), 5 seconds)
      }
    }

  }

}
