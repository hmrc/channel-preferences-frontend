package uk.gov.hmrc.common.microservice.epaye

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import play.api.test.WithApplication
import org.mockito.Mockito._
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeDomain._
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeDomain.EpayeLinks
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeDomain.NonRTI
import play.api.test.FakeApplication
import scala.Some
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeDomain.EpayeAccountSummary

class EpayeConnectorSpec extends BaseSpec {

   private val uri = "someUri"

  "Requesting the Epaye root" should {

    "Make an HTTP call to the service and return the root data" in new WithEpayeConnector {
      val epayeLinks = EpayeLinks(accountSummary = Some("/some/path"))
      val rootObject = EpayeJsonRoot(epayeLinks)

      when(mockHttpClient.get[EpayeJsonRoot](uri)).thenReturn(Some(rootObject))
      epayeConnector.root(uri) shouldBe rootObject
    }

    "Make an HTTP call to the service and throw an IllegalStateException if there is no root data" in new WithEpayeConnector {
      when(mockHttpClient.get[EpayeRoot](uri)).thenReturn(None)
      evaluating(epayeConnector.root(uri)) should produce[IllegalStateException]
    }
  }

  "Requesting the Epaye account summary" should {

    "Return the correct response for an example with account summary information" in new WithEpayeConnector {
      val summary = EpayeAccountSummary(nonRti = Some(NonRTI(BigDecimal(50D), 2013)))

      when(mockHttpClient.get[EpayeAccountSummary](uri)).thenReturn(Some(summary))
      epayeConnector.accountSummary(uri) shouldBe Some(summary)
    }

    "Return None for an example with invalid data - containing neither RTI nor Non-RTI information" in new WithEpayeConnector {
      val invalidSummary = EpayeAccountSummary(rti = None, nonRti = None)

      when(mockHttpClient.get[EpayeAccountSummary](uri)).thenReturn(Some(invalidSummary))
      epayeConnector.accountSummary(uri) shouldBe None
    }

    "Return None for an example where no data is returned (e.g. 404 occurs)" in new WithEpayeConnector {
      when(mockHttpClient.get[EpayeAccountSummary](uri)).thenReturn(None)
      epayeConnector.accountSummary(uri) shouldBe None
    }
  }
}

abstract class WithEpayeConnector extends WithApplication(FakeApplication()) with MockitoSugar {

  val mockHttpClient = mock[HttpWrapper]

  val epayeConnector = new EpayeConnector {
    override def httpGet[A](uri: String)(implicit m: Manifest[A]): Option[A] = mockHttpClient.get[A](uri)
  }

  class HttpWrapper {
    def get[T](uri: String): Option[T] = None
  }
}
