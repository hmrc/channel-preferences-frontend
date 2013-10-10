package controllers.paye

import uk.gov.hmrc.utils.TaxYearResolver

trait TaxYearSupport {

  def currentTaxYear = TaxYearResolver.currentTaxYear
  def currentTaxYearYearRange = TaxYearResolver.currentTaxYearYearsRange
  def startOfCurrentTaxYear = TaxYearResolver.startOfCurrentTaxYear
  def endOfCurrentTaxYear = TaxYearResolver.endOfCurrentTaxYear

}
