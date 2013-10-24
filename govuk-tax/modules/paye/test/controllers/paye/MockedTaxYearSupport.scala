package controllers.paye

import org.joda.time.LocalDate

trait MockedTaxYearSupport extends TaxYearSupport {

  override def currentTaxYear = 2013
  override def startOfCurrentTaxYear = new LocalDate(2013, 4, 6)
  override def endOfCurrentTaxYear = new LocalDate(2014, 4, 5)
  override def currentTaxYearYearsRange = 2013 to 2014

}
