package controllers.paye

import org.joda.time.{DateTimeZone, Interval, LocalDate}


//TODO:Remove this trait if the tests keep using the real tax year support (i.e. they always use the current valid tax year).
//At the moment the TaxYearSupport cannot be totally overridden in tests with this stub as some components use TaxYearResolver, and other are objects
//(see AddCarBenefitValidator) which are required in controllers tests
trait StubTaxYearSupport extends TaxYearSupport {

  override def currentTaxYear = 2013
  private def nextTaxYear = currentTaxYear + 1
  private val ukTime = DateTimeZone.forID("Europe/London")

  override def startOfCurrentTaxYear = new LocalDate(currentTaxYear, 4, 6)
  override def endOfCurrentTaxYear = new LocalDate(nextTaxYear, 4, 5)
  override def currentTaxYearYearsRange = currentTaxYear to nextTaxYear
  override def taxYearInterval = new Interval(startOfCurrentTaxYear.toDateTimeAtStartOfDay(ukTime),
    startOfCurrentTaxYear.plusYears(1).toDateTimeAtStartOfDay(ukTime))
}
