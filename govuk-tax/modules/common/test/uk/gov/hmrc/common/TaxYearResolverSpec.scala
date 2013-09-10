package uk.gov.hmrc.common

import org.joda.time.LocalDate

class TaxYearResolverSpec extends BaseSpec {

  "Current Tax Year " should {
    "return the year 2010 when the current date is 5-April-2011" in {
      val actualYear = TaxYearResolver.currentTaxYear(new LocalDate(2011, 4, 5))
      actualYear shouldBe 2010
    }

    "return the year 2010 when the current date is 5-December-2010" in {
      val actualYear = TaxYearResolver.currentTaxYear(new LocalDate(2010, 12, 5))
      actualYear shouldBe 2010
    }

    "return the year 2011 when the current date is 6-April-2011" in {
      val actualYear = TaxYearResolver.currentTaxYear(new LocalDate(2011, 4, 6))
      actualYear shouldBe 2011
    }

    "return the year 2011 when the current date is 1-May-2011" in {
      val actualYear = TaxYearResolver.currentTaxYear(new LocalDate(2011, 5, 1))
      actualYear shouldBe 2011
    }
  }
}
