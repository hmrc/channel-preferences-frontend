package uk.gov.hmrc.common.microservice.epaye

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import play.api.test.WithApplication
import org.mockito.Mockito._
import play.api.test.FakeApplication
import uk.gov.hmrc.microservice.MicroServiceException
import play.api.libs.ws.Response
import uk.gov.hmrc.common.microservice.epaye.domain.{NonRTI, EpayeAccountSummary, EpayeJsonRoot, EpayeLinks}
import controllers.common.actions.HeaderCarrier

class EpayeConnectorSpec extends BaseSpec {

  private val uri = "someUri"
  implicit val hc = HeaderCarrier()


  "Requesting the EPAYE root" should {

    "return the root object for a successful response" in new EpayeConnectorApplication {

      val epayeLinks = EpayeLinks(accountSummary = Some("/some/path"))
      val rootObject = EpayeJsonRoot(epayeLinks)

      when(mockHttpClient.get[EpayeJsonRoot](uri)).thenReturn(Some(rootObject))
      connector.root(uri) shouldBe rootObject
    }

    "return a root object with an empty set of links for a 404 response" in new EpayeConnectorApplication {
      when(mockHttpClient.get[EpayeJsonRoot](uri)).thenReturn(None)
      connector.root(uri) shouldBe EpayeJsonRoot(EpayeLinks(None))
    }

    "Propagate any exception that gets thrown" in new EpayeConnectorApplication {
      val rootUri = "/epaye/123/456"

      when(mockHttpClient.get[EpayeJsonRoot](rootUri)).thenThrow(new MicroServiceException("exception thrown by external service", mock[Response]))

      evaluating(connector.root(rootUri)) should produce[MicroServiceException]
    }
  }

  "Requesting the Epaye account summary" should {

    "Return the correct response for an example with account summary information" in new EpayeConnectorApplication {
      val summary = EpayeAccountSummary(nonRti = Some(NonRTI(BigDecimal(50D), 2013)))

      when(mockHttpClient.get[EpayeAccountSummary](uri)).thenReturn(Some(summary))
      connector.accountSummary(uri) shouldBe Some(summary)
    }

    "Return None for an example with invalid data - containing neither RTI nor Non-RTI information" in new EpayeConnectorApplication {
      val invalidSummary = EpayeAccountSummary(rti = None, nonRti = None)

      when(mockHttpClient.get[EpayeAccountSummary](uri)).thenReturn(Some(invalidSummary))
      connector.accountSummary(uri) shouldBe None
    }

    "Return None for an example where no data is returned (e.g. 404 occurs)" in new EpayeConnectorApplication {
      when(mockHttpClient.get[EpayeAccountSummary](uri)).thenReturn(None)
      connector.accountSummary(uri) shouldBe None
    }
  }
}

abstract class EpayeConnectorApplication extends WithApplication(FakeApplication()) with MockitoSugar {

  val mockHttpClient = mock[HttpWrapper]

  val connector = new EpayeConnector {
    override def httpGet[A](uri: String)(implicit m: Manifest[A]): Option[A] = mockHttpClient.get[A](uri)
  }

  class HttpWrapper {
    def get[T](uri: String): Option[T] = None
  }

}
