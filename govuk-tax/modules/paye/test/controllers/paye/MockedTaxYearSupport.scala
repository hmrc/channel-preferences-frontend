package controllers.paye

import org.joda.time.LocalDate

trait MockedTaxYearSupport extends TaxYearSupport {

  override def currentTaxYear = 2013
  private def nextTaxYear = currentTaxYear + 1
  override def startOfCurrentTaxYear = new LocalDate(currentTaxYear, 4, 6)
  override def endOfCurrentTaxYear = new LocalDate(nextTaxYear, 4, 5)
  override def currentTaxYearYearsRange = currentTaxYear to nextTaxYear

}
