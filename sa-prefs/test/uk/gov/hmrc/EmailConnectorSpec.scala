package uk.gov.hmrc

import org.scalatest.mock.MockitoSugar
import org.scalatest._
import org.mockito.Mockito._
import java.net.URLEncoder
import play.api.test.{FakeApplication, WithApplication}

class TestEmailConnector extends EmailConnector with MockitoSugar {

  val httpWrapper = mock[HttpWrapper]

  override protected def httpGet[A](uri: String)(implicit m: Manifest[A]): Option[A] = {
    httpWrapper.get(uri)
  }

  class HttpWrapper {
    def get[T](uri: String): Option[T] = None
  }

}

class EmailConnectorSpec extends WordSpec with MockitoSugar with ShouldMatchers with BeforeAndAfterEach {

  lazy val emailConnector = new TestEmailConnector

  override def afterEach = reset(emailConnector.httpWrapper)

  private val emailAddress = "someEmail@email.com"
  private val encodedAddress = URLEncoder.encode(emailAddress, "UTF-8")

  "validateEmailAddress" should {
    
    "correctly invoke the email microservice and return true if the service returns true" in new WithApplication(FakeApplication()) {
      when(emailConnector.httpWrapper.get[ValidateEmailResponse](s"/validate-email-address?email=$encodedAddress")).thenReturn(Some(ValidateEmailResponse(true)))

      emailConnector.validateEmailAddress(emailAddress) shouldBe true

      verify(emailConnector.httpWrapper).get[ValidateEmailResponse](s"/validate-email-address?email=$encodedAddress")
    }

    "correctly invoke the email microservice and return false if the service returns false" in new WithApplication(FakeApplication()) {
      when(emailConnector.httpWrapper.get[ValidateEmailResponse](s"/validate-email-address?email=$encodedAddress")).thenReturn(Some(ValidateEmailResponse(false)))

      emailConnector.validateEmailAddress(emailAddress) shouldBe false

      verify(emailConnector.httpWrapper).get[ValidateEmailResponse](s"/validate-email-address?email=$encodedAddress")
    }
  }

}
