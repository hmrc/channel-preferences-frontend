package uk.gov.hmrc.common.microservice.vat

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import play.api.test.WithApplication
import org.mockito.Mockito._
import play.api.test.FakeApplication
import org.mockito.Matchers
import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.{ VatAccountBalance, VatAccountSummary, VatRoot }
import uk.gov.hmrc.domain.Vrn

class VatMicroServiceTest extends BaseSpec {

  "VatMicroService root service " should {

    "call the micro service with the correct uri and get the contents" in new WithApplication(FakeApplication()) {

      val service = new HttpMockedVatMicroService
      val vatRoot = VatRoot(Vrn("123456"), Map.empty)
      when(service.httpWrapper.get[VatRoot]("/vat/vrn/123456")).thenReturn(Some(vatRoot))

      val result = service.root("/vat/vrn/123456")

      result shouldBe vatRoot
      verify(service.httpWrapper).get[VatRoot](Matchers.eq("/vat/vrn/123456"))

    }

    "call the micro service with the correct uri but VAT root is not found" in new WithApplication(FakeApplication()) {

      val service = new HttpMockedVatMicroService
      when(service.httpWrapper.get[VatRoot]("/vat/vrn/123456")).thenReturn(None)

      evaluating(service.root("/vat/vrn/123456")) should produce[IllegalStateException]
      verify(service.httpWrapper).get[VatRoot](Matchers.eq("/vat/vrn/123456"))

    }

  }

  "VatMicroService account summary" should {

    "call the micro service with the correct uri and return the contents" in new WithApplication(FakeApplication()) {

      val service = new HttpMockedVatMicroService
      val accountSummary = Some(VatAccountSummary(Some(VatAccountBalance(Some(4.0), None)), None))
      when(service.httpWrapper.get[VatAccountSummary]("/vat/vrn/123456/accountSummary")).thenReturn(accountSummary)

      val result = service.accountSummary("/vat/vrn/123456/accountSummary")

      result shouldBe accountSummary
      verify(service.httpWrapper).get[VatRoot](Matchers.eq("/vat/vrn/123456/accountSummary"))

    }
  }
}

class HttpMockedVatMicroService extends VatMicroService with MockitoSugar {

  val httpWrapper = mock[HttpWrapper]

  override def httpGet[A](uri: String)(implicit m: Manifest[A]): Option[A] = httpWrapper.get[A](uri)

  class HttpWrapper {
    def get[T](uri: String): Option[T] = None
  }

}
