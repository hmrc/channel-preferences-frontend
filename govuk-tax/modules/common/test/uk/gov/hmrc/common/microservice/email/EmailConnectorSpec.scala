package uk.gov.hmrc.common.microservice.email

import domain.ValidateEmailResponse
import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.JsValue
import org.mockito.Mockito._
import java.net.URLEncoder
import play.api.test.{FakeApplication, WithApplication}

class EmailConnectorSpec extends BaseSpec {

  "Calling validateEmailAddress" should {

    "correctly invoke the email microservice and return true if the service returns true" in new WithApplication(FakeApplication()) {
      val connector = new HttpMockedEmailConnector
      val address = "bob@somewhere.com"
      val encodedAddress = URLEncoder.encode(address, "UTF-8")
      when(connector.httpWrapper.get[ValidateEmailResponse](s"/validate-email-address?email=$encodedAddress")).thenReturn(Some(ValidateEmailResponse(true)))
      connector.validateEmailAddress(address) shouldBe true
    }

    "correctly invoke the email microservice and return false if the service returns false" in new WithApplication(FakeApplication()) {
      val connector = new HttpMockedEmailConnector
      val address = "bob@somewhere.com"
      val encodedAddress = URLEncoder.encode(address, "UTF-8")
      when(connector.httpWrapper.get[ValidateEmailResponse](s"/validate-email-address?email=$encodedAddress")).thenReturn(Some(ValidateEmailResponse(false)))
      connector.validateEmailAddress(address) shouldBe false
    }
  }
}


class HttpMockedEmailConnector extends EmailConnector with MockitoSugar {

  val httpWrapper = mock[HttpWrapper]

  override def httpGet[A](uri: String)(implicit m: Manifest[A]): Option[A] = httpWrapper.get[A](uri)

  override def httpPost[A](uri: String, body: JsValue, headers: Map[String, String])(implicit m: Manifest[A]): Option[A] = httpWrapper.post[A](uri, body, headers)

  class HttpWrapper {
    def get[T](uri: String): Option[T] = None
    def post[T](uri: String, body: JsValue, headers: Map[String, String]): Option[T] = None
  }

}