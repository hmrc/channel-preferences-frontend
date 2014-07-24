package connectors

import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import java.net.URLEncoder
import uk.gov.hmrc.play.connectors.HeaderCarrier
import uk.gov.hmrc.test.UnitSpec
import scala.concurrent.Future
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.play.test._

class EmailConnectorSpec extends UnitSpec with ScalaFutures with WithFakeApplication {

  "Calling validateEmailAddress" should {

    implicit val hc = HeaderCarrier()

    "correctly invoke the email microservice and return true if the service returns true" in  {
      val connector = new HttpMockedEmailConnector
      val address = "bob@somewhere.com"
      val encodedAddress = URLEncoder.encode(address, "UTF-8")
      when(connector.httpWrapper.getF[ValidateEmailResponse](s"/validate-email-address?email=$encodedAddress")).thenReturn(Future.successful(Some(ValidateEmailResponse(true))))
      connector.validateEmailAddress(address).futureValue shouldBe true
    }

    "correctly invoke the email microservice and return false if the service returns false" in  {
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