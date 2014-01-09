package uk.gov.hmrc.common.microservice.governmentgateway

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.BaseSpec
import org.mockito.Mockito._
import play.api.test.WithApplication
import play.api.test.FakeApplication
import controllers.common.actions.HeaderCarrier
import scala.concurrent.Future
import org.scalatest.concurrent.ScalaFutures

class TestGovernmentGatewayConnector extends GovernmentGatewayConnector with MockitoSugar {

  val httpWrapper = mock[HttpWrapper]

  override protected def httpPostF[B, A](uri: String, body: Option[A], headers: Map[String, String] = Map.empty)(implicit a: Manifest[A], b: Manifest[B], headerCarrier: HeaderCarrier): Future[Option[B]] = {
    body.map(body => httpWrapper.postF[B, A](uri, body, headers)).get
  }

  override protected def httpGetF[A](uri: String)(implicit m: Manifest[A], headerCarrier: HeaderCarrier): Future[Option[A]] = {
    httpWrapper.getF[A](uri)
  }

  class HttpWrapper {
    def getF[T](uri: String): Future[Option[T]] = Future.successful(None)

    def postF[TResult, TBody](uri: String, body: TBody, headers: Map[String, String] = Map.empty)(implicit a: Manifest[TBody], b: Manifest[TResult], headerCarrier: HeaderCarrier): Future[Option[TResult]] = Future.successful(None)
  }

}

class GovernmentGatewayConnectorSpec extends BaseSpec with MockitoSugar with ScalaFutures {

  lazy val service = new TestGovernmentGatewayConnector

  "GGW Connector" should {

    "throw an exception when no details are found in Government Gateway on login" in new WithApplication(FakeApplication()) {

      val credentials = Credentials("user", "incorrectPw")
      when(service.httpWrapper.postF[GovernmentGatewayResponse, Credentials]("/login", credentials, Map.empty)).thenReturn(Future.successful(None))

      service.login(credentials).failed.futureValue shouldBe an[IllegalStateException]
    }

    "throw an exception when no details are found in Government Gateway on sso login" in new WithApplication(FakeApplication()) {

      val ssoLoginRequest = SsoLoginRequest("3jeih3g3gg3ljkdlh3", 3837636)
      when(service.httpWrapper.postF[GovernmentGatewayResponse, SsoLoginRequest]("/sso-login", ssoLoginRequest, Map.empty)).thenReturn(Future.successful(None))

      service.ssoLogin(ssoLoginRequest).failed.futureValue shouldBe an[IllegalStateException]
    }

    "throw an exception when a profile for the given user is not found" in new WithApplication(FakeApplication()) {

      val userId = "/auth/oid/missingUser"
      val expectedResponse = None

      when(service.httpWrapper.getF[ProfileResponse](s"/profile$userId")).thenReturn(Future.successful(expectedResponse))

      service.profile(userId).failed.futureValue shouldBe an[RuntimeException]
    }

  }

}
