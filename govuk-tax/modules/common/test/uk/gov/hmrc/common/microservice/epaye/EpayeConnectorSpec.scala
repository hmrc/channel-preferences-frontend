package uk.gov.hmrc.common.microservice.epaye

import uk.gov.hmrc.common.{MockGet, BaseSpec}
import play.api.test.WithApplication
import org.mockito.Mockito._
import play.api.test.FakeApplication
import uk.gov.hmrc.common.microservice.MicroServiceException
import play.api.libs.ws.Response
import uk.gov.hmrc.common.microservice.epaye.domain.{NonRTI, EpayeAccountSummary, EpayeJsonRoot, EpayeLinks}
import scala.concurrent.Future
import org.scalatest.mock.MockitoSugar
import org.scalatest.concurrent.ScalaFutures

class EpayeConnectorSpec extends BaseSpec with ScalaFutures {

  private val uri = "someUri"

  "Requesting the EPAYE root" should {

    "return the root object for a successful response" in new EpayeConnectorApplication {

      val epayeLinks = EpayeLinks(accountSummary = Some("/some/path"))
      val rootObject = EpayeJsonRoot(epayeLinks)

      when(mockHttpClient.getF[EpayeJsonRoot](uri)).thenReturn(Some(rootObject))
      whenReady(connector.root(uri))(_ shouldBe rootObject)
    }

    "return a root object with an empty set of links for a 404 response" in new EpayeConnectorApplication {
      when(mockHttpClient.getF[EpayeJsonRoot](uri)).thenReturn(None)
      whenReady(connector.root(uri))(_ shouldBe EpayeJsonRoot(EpayeLinks(None)))
    }

    "Propagate any exception that gets thrown" in new EpayeConnectorApplication {
      val rootUri = "/epaye/123/456"

      when(mockHttpClient.getF[EpayeJsonRoot](rootUri)).thenThrow(new MicroServiceException("exception thrown by external service", mock[Response]))

      evaluating(connector.root(rootUri)) should produce[MicroServiceException]
    }
  }

  "Requesting the Epaye account summary" should {

    "Return the correct response for an example with account summary information" in new EpayeConnectorApplication {
      val summary = EpayeAccountSummary(nonRti = Some(NonRTI(BigDecimal(50D), 2013)))

      when(mockHttpClient.getF[EpayeAccountSummary](uri)).thenReturn(Future.successful(Some(summary)))
      await(connector.accountSummary(uri)) shouldBe Some(summary)
    }

    "Return None for an example with invalid data - containing neither RTI nor Non-RTI information" in new EpayeConnectorApplication {
      val invalidSummary = EpayeAccountSummary(rti = None, nonRti = None)

      when(mockHttpClient.getF[EpayeAccountSummary](uri)).thenReturn(Future.successful(Some(invalidSummary)))
      await(connector.accountSummary(uri)) shouldBe None
    }

    "Return None for an example where no data is returned (e.g. 404 occurs)" in new EpayeConnectorApplication {
      when(mockHttpClient.getF[EpayeAccountSummary](uri)).thenReturn(Future.successful(None))
      await(connector.accountSummary(uri)) shouldBe None
    }
  }
}

abstract class EpayeConnectorApplication extends WithApplication(FakeApplication()) with MockitoSugar {
  val connector = new EpayeConnector with MockGet
  val mockHttpClient = connector.mockHttpClient
}
