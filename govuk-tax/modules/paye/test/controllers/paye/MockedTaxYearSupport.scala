package controllers.paye

import org.joda.time.{DateTimeZone, Interval, LocalDate}

trait MockedTaxYearSupport extends TaxYearSupport {

  override def currentTaxYear = 2013
  private def nextTaxYear = currentTaxYear + 1
  private val ukTime = DateTimeZone.forID("Europe/London")

  override def startOfCurrentTaxYear = new LocalDate(currentTaxYear, 4, 6)
  override def endOfCurrentTaxYear = new LocalDate(nextTaxYear, 4, 5)
  override def currentTaxYearYearsRange = currentTaxYear to nextTaxYear
  override def taxYearInterval = new Interval(startOfCurrentTaxYear.toDateTimeAtStartOfDay(ukTime),
    startOfCurrentTaxYear.plusYears(1).toDateTimeAtStartOfDay(ukTime))
}
