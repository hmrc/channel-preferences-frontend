package uk.gov.hmrc.common.microservice.vat.domain

import uk.gov.hmrc.common.BaseSpec
import uk.gov.hmrc.common.microservice.vat.VatConnector
import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.{ VatAccountSummary, VatRoot }
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers
import uk.gov.hmrc.domain.Vrn

class VatDomainSpec extends BaseSpec with MockitoSugar {

  "VatRoot accountSummary" should {

    "call the VAT microservice when the uri is found in the VatRoot and return its value" in {
      val uri = "/vat/vrn/12345/accountSummary"
      val vatRoot = VatRoot(Vrn("12345"), Map("accountSummary" -> uri))
      val accountSummary = Some(VatAccountSummary(None, Some("2013-03-03")))
      val vatConnector = mock[VatConnector]

      when(vatConnector.accountSummary(uri)).thenReturn(accountSummary)

      vatRoot.accountSummary(vatConnector) shouldBe accountSummary
      verify(vatConnector).accountSummary(Matchers.eq(uri))
    }

    "return None when the account summary link is not present" in {
      val vatRoot = VatRoot(Vrn("12345"), Map("designatoryDetails" -> "/vat/vrn/12345/designatoryDetails"))
      val vatConnector = mock[VatConnector]

      vatRoot.accountSummary(vatConnector) shouldBe None
      verify(vatConnector, times(0)).accountSummary(Matchers.anyString())
    }

  }

}
