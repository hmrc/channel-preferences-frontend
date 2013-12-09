package uk.gov.hmrc.common.microservice.email

import domain.ValidateEmailResponse
import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.JsValue
import org.mockito.Mockito._
import java.net.URLEncoder
import play.api.test.{FakeApplication, WithApplication}
import controllers.common.actions.HeaderCarrier
import scala.concurrent.Future
import org.scalatest.concurrent.ScalaFutures

class EmailConnectorSpec extends BaseSpec with ScalaFutures {

  "Calling validateEmailAddress" should {

    "correctly invoke the email microservice and return true if the service returns true" in new WithApplication(FakeApplication()) {
      val connector = new HttpMockedEmailConnector
      val address = "bob@somewhere.com"
      val encodedAddress = URLEncoder.encode(address, "UTF-8")
      when(connector.httpWrapper.getF[ValidateEmailResponse](s"/validate-email-address?email=$encodedAddress")).thenReturn(Future.successful(Some(ValidateEmailResponse(true))))
      connector.validateEmailAddress(address).futureValue shouldBe true
    }

    "correctly invoke the email microservice and return false if the service returns false" in new WithApplication(FakeApplication()) {
      val connector = new HttpMockedEmailConnector
      val address = "bob@somewhere.com"
      val encodedAddress = URLEncoder.encode(address, "UTF-8")
      when(connector.httpWrapper.getF[ValidateEmailResponse](s"/validate-email-address?email=$encodedAddress")).thenReturn(Future.successful(Some(ValidateEmailResponse(false))))
      connector.validateEmailAddress(address).futureValue shouldBe false
    }
  }
}


class HttpMockedEmailConnector extends EmailConnector with MockitoSugar {

  val httpWrapper = mock[HttpWrapper]

  override def httpGetF[A](uri: String)(implicit m: Manifest[A], hc: HeaderCarrier):  Future[Option[A]] = httpWrapper.getF[A](uri)


  class HttpWrapper {
    def getF[T](uri: String): Future[Option[T]] = Future.successful(None)
  }

}