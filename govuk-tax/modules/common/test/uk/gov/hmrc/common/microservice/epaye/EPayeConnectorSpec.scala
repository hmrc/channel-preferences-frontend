package uk.gov.hmrc.common.microservice.epaye

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import play.api.test.{FakeApplication, WithApplication}
import org.mockito.Mockito._
import uk.gov.hmrc.common.microservice.epaye.domain.EPayeDomain.{EPayeLinks, NonRTI, EPayeAccountSummary, EPayeRoot}
import uk.gov.hmrc.domain.EmpRef

class EPayeConnectorSpec extends BaseSpec {

   private val uri = "someUri"

  "Requesting the EPaye root" should {

    "Make an HTTP call to the service and return the root data" in new WithEPayeConnector {
      val epayeLinks = EPayeLinks(accountSummary = Some("/some/path"))
      val rootObject = EPayeRoot(epayeLinks, EmpRef("dummyOffice", "dummyReference"))

      when(mockHttpClient.get[EPayeRoot](uri)).thenReturn(Some(rootObject))
      epayeConnector.root(uri) shouldBe rootObject
    }

    "Make an HTTP call to the service and throw an IllegalStateException if there is no root data" in new WithEPayeConnector {
      when(mockHttpClient.get[EPayeRoot](uri)).thenReturn(None)
      evaluating(epayeConnector.root(uri)) should produce[IllegalStateException]
    }
  }

  "Requesting the EPaye account summary" should {

    "Return the correct response for an example with account summary information" in new WithEPayeConnector {
      val summary = EPayeAccountSummary(nonRti = Some(NonRTI(BigDecimal(50D), 2013)))

      when(mockHttpClient.get[EPayeAccountSummary](uri)).thenReturn(Some(summary))
      epayeConnector.accountSummary(uri) shouldBe Some(summary)
    }

    "Return None for an example with invalid data - containing neither RTI nor Non-RTI information" in new WithEPayeConnector {
      val invalidSummary = EPayeAccountSummary(rti = None, nonRti = None)

      when(mockHttpClient.get[EPayeAccountSummary](uri)).thenReturn(Some(invalidSummary))
      epayeConnector.accountSummary(uri) shouldBe None
    }

    "Return None for an example where no data is returned (e.g. 404 occurs)" in new WithEPayeConnector {
      when(mockHttpClient.get[EPayeAccountSummary](uri)).thenReturn(None)
      epayeConnector.accountSummary(uri) shouldBe None
    }
  }
}

abstract class WithEPayeConnector extends WithApplication(FakeApplication()) with MockitoSugar {

  val mockHttpClient = mock[HttpWrapper]

  val epayeConnector = new EPayeConnector {
    override def httpGet[A](uri: String)(implicit m: Manifest[A]): Option[A] = mockHttpClient.get[A](uri)
  }

  class HttpWrapper {
    def get[T](uri: String): Option[T] = None
  }
}
