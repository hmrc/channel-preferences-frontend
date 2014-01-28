package controllers.paye

import org.joda.time.{DateTimeZone, Interval, LocalDate}

trait TaxYearSupport {

  implicit def currentTaxYear = 2015//TMP TaxYearResolver.currentTaxYear
  implicit def taxYearInterval =  new Interval(new LocalDate(currentTaxYear, 4, 6).toDateTimeAtStartOfDay(DateTimeZone.forID("Europe/London")),
                                 new LocalDate(currentTaxYear+1, 4, 6).toDateTimeAtStartOfDay(DateTimeZone.forID("Europe/London")))
                                  //TMP TaxYearResolver.taxYearInterval
  implicit def currentTaxYearYearsRange = currentTaxYear to currentTaxYear+1 //TMP TaxYearResolver.currentTaxYearYearsRange
  implicit def startOfCurrentTaxYear = new LocalDate(currentTaxYear, 4, 6) //TMP TaxYearResolver.startOfCurrentTaxYear
  implicit def endOfCurrentTaxYear = new LocalDate(currentTaxYear+1, 4, 5) //TMP TaxYearResolver.endOfCurrentTaxYear

}
