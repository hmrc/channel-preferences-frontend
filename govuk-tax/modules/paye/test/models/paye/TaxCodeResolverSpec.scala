package models.paye

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.paye.domain.TaxCode

class TaxCodeResolverSpec extends BaseSpec with MockitoSugar{

  "TaxCodeResolverSpec" should {

    "return the tax code associated to an employment using the latest coding sequence number" in {

      val taxCode1 = TaxCode(1, Some(1), 2013, "1T", List.empty)
      val taxCodeForEmployment1 = TaxCode(1, Some(3), 2013, "2T", List.empty)
      val taxCode3 = TaxCode(1, Some(2), 2013, "3T", List.empty)
      val taxCode4 = TaxCode(1, None, 2013, "4T", List.empty)
      val taxCode5 = TaxCode(2, Some(4), 2013, "5T", List.empty)
      val taxCodeForEmployment2 = TaxCode(2, Some(5), 2013, "6T", List.empty)

      val taxCodes = Seq(taxCode1, taxCodeForEmployment1, taxCode3, taxCode4, taxCode5, taxCodeForEmployment2)

      val taxCodeEmployment1 = TaxCodeResolver.currentTaxCode(taxCodes, 1)
      taxCodeEmployment1 shouldBe "2T"

      val taxCodeEmployment2 = TaxCodeResolver.currentTaxCode(taxCodes, 2)
      taxCodeEmployment2 shouldBe "6T"

    }

    "return N/A (non defined tax code) when list of tax codes does not contain any tax codes for the employment selected" in {

      val taxCode1 = TaxCode(1, Some(1), 2013, "1T", List.empty)
      val taxCode2 = TaxCode(1, Some(2), 2013, "2T", List.empty)
      val taxCodes = Seq(taxCode1, taxCode2)

      val taxCodeEmployment2 = TaxCodeResolver.currentTaxCode(taxCodes, 2)

      taxCodeEmployment2 shouldBe TaxCodeResolver.NON_DEFINED_TAXCODE

    }

  }

}
