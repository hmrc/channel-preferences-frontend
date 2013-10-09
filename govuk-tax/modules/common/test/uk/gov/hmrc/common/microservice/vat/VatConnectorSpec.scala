package uk.gov.hmrc.common.microservice.vat

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import play.api.test.WithApplication
import org.mockito.Mockito._
import play.api.test.FakeApplication
import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.{VatJsonRoot, VatAccountBalance, VatAccountSummary}
import uk.gov.hmrc.microservice.MicroServiceException
import play.api.libs.ws.Response

class VatConnectorSpec extends BaseSpec {

  "Requesting the VAT root" should {

    "return the root object for a successful response" in new VatConnectorApplication {

      val vatRoot = VatJsonRoot(Map("some" -> "link"))

      when(mockHttpClient.get[VatJsonRoot]("/vat/vrn/123456")).thenReturn(Some(vatRoot))

      val result = connector.root("/vat/vrn/123456")

      result shouldBe vatRoot
    }

    "return a root object with an empty set of links for a 404 response" in new VatConnectorApplication {

      when(mockHttpClient.get[VatJsonRoot]("/vat/vrn/123456")).thenReturn(None)
      connector.root("/vat/vrn/123456") shouldBe VatJsonRoot(Map.empty)
    }

    "Propagate any exception that gets thrown" in new VatConnectorApplication {
      val rootUri = "/vat/111456111"

      when(mockHttpClient.get[VatJsonRoot](rootUri)).thenThrow(new MicroServiceException("exception thrown by external service", mock[Response]))

      evaluating(connector.root(rootUri)) should produce[MicroServiceException]
    }
  }

  "VatConnector account summary" should {

    "call the micro service with the correct uri and return the contents" in new VatConnectorApplication {

      val accountSummary = Some(VatAccountSummary(Some(VatAccountBalance(Some(4.0), None)), None))
      when(mockHttpClient.get[VatAccountSummary]("/vat/vrn/123456/accountSummary")).thenReturn(accountSummary)

      val result = connector.accountSummary("/vat/vrn/123456/accountSummary")

      result shouldBe accountSummary
    }
  }
}

class VatConnectorApplication extends WithApplication(FakeApplication()) with MockitoSugar {

  val mockHttpClient = mock[HttpWrapper]

  val connector = new VatConnector {
    override def httpGet[A](uri: String)(implicit m: Manifest[A]): Option[A] = mockHttpClient.get[A](uri)
  }

  class HttpWrapper {
    def get[T](uri: String): Option[T] = None
  }
}
