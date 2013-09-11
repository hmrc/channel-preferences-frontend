package uk.gov.hmrc.common

import org.joda.time.LocalDate

object TaxYearResolver {

  def apply() = currentTaxYear(new LocalDate)

  def currentTaxYear(dateToResolve: LocalDate): Int = {
    val year = dateToResolve.year.get

    if (dateToResolve.isBefore(new LocalDate(year, 4, 6)))
      year - 1
    else
      year
  }
}
