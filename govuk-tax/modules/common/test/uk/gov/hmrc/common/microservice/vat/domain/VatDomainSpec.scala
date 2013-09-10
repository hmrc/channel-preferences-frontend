package uk.gov.hmrc.common.microservice.vat.domain

import uk.gov.hmrc.common.BaseSpec
import uk.gov.hmrc.microservice.auth.domain.Vrn
import uk.gov.hmrc.common.microservice.vat.VatMicroService
import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.{VatAccountSummary, VatRoot}
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers

class VatDomainSpec extends BaseSpec with MockitoSugar{


  "VatRoot accountSummary" should {

    "call the VAT microservice when the uri is found in the VatRoot and return its value" in {
      val uri = "/vat/vrn/12345/accountSummary"
      val vatRoot = VatRoot(Vrn("12345"), Map("accountSummary"-> uri))
      val accountSummary = Some(VatAccountSummary(None,Some("2013-03-03")))
      val vatMicroService = mock[VatMicroService]

      when(vatMicroService.accountSummary(uri)).thenReturn(accountSummary)

      vatRoot.accountSummary(vatMicroService) shouldBe accountSummary
      verify(vatMicroService).accountSummary(Matchers.eq(uri))
    }

    "return None when the account summary link is not present" in {
      val vatRoot = VatRoot(Vrn("12345"), Map("designatoryDetails"-> "/vat/vrn/12345/designatoryDetails"))
      val vatMicroService = mock[VatMicroService]

      vatRoot.accountSummary(vatMicroService) shouldBe None
      verify(vatMicroService, times(0)).accountSummary(Matchers.anyString())
    }

  }

}
