package controllers.paye

import uk.gov.hmrc.utils.TaxYearResolver

trait TaxYearSupport {

  implicit def currentTaxYear = TaxYearResolver.currentTaxYear
  implicit def currentTaxYearYearsRange = TaxYearResolver.currentTaxYearYearsRange
  implicit def startOfCurrentTaxYear = TaxYearResolver.startOfCurrentTaxYear
  implicit def endOfCurrentTaxYear = TaxYearResolver.endOfCurrentTaxYear

}
