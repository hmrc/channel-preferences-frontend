package uk.gov.hmrc

import org.scalatest.mock.MockitoSugar
import org.scalatest._
import org.mockito.Mockito._
import org.mockito.Matchers.any
import java.net.URLEncoder
import play.api.test.{FakeApplication, WithApplication}
import scala.concurrent.Future
import org.scalatest.concurrent.ScalaFutures
import controllers.common.actions.HeaderCarrier

class TestEmailConnector extends EmailConnector with MockitoSugar {

  val httpWrapper = mock[HttpWrapper]

  override protected val serviceUrl: String = "notUsed"


  override protected def httpGetF[A](uri: String)(implicit m: Manifest[A], headerCarrier: HeaderCarrier): Future[Option[A]] = httpWrapper.getF(uri)

  class HttpWrapper {
    def getF[T](uri: String): Future[Option[T]] = Future.successful(None)
  }

}

class EmailConnectorSpec extends WordSpec with MockitoSugar with ShouldMatchers with BeforeAndAfterEach with ScalaFutures {

  lazy val emailConnector = new TestEmailConnector

  override def afterEach = reset(emailConnector.httpWrapper)

  private val emailAddress = "someEmail@email.com"
  private val encodedAddress = URLEncoder.encode(emailAddress, "UTF-8")

  "validateEmailAddress" should {
    
    "correctly invoke the email microservice and return true if the service returns true" in new WithApplication(FakeApplication()) {
      when(emailConnector.httpWrapper.getF[ValidateEmailResponse](s"/validate-email-address?email=$encodedAddress")).thenReturn(Future.successful(Some(ValidateEmailResponse(true))))

      emailConnector.validateEmailAddress(emailAddress)(HeaderCarrier()).futureValue shouldBe true

      verify(emailConnector.httpWrapper).getF[ValidateEmailResponse](s"/validate-email-address?email=$encodedAddress")
    }

    "correctly invoke the email microservice and return false if the service returns false" in new WithApplication(FakeApplication()) {
      when(emailConnector.httpWrapper.getF[ValidateEmailResponse](s"/validate-email-address?email=$encodedAddress")).thenReturn(Future.successful(Some(ValidateEmailResponse(false))))

      emailConnector.validateEmailAddress(emailAddress)(HeaderCarrier()).futureValue shouldBe false

      verify(emailConnector.httpWrapper).getF[ValidateEmailResponse](s"/validate-email-address?email=$encodedAddress")
    }
  }

}
