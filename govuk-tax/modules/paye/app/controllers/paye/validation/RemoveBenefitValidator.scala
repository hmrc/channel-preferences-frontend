package controllers.paye.validation

import play.api.data.Forms._
import org.joda.time.LocalDate
import models.paye.CarFuelBenefitDates
import uk.gov.hmrc.common.TaxYearResolver
import models.paye.BenefitTypes._
import scala.Some

object RemoveBenefitValidator {

  private val FUEL_DIFFERENT_DATE = "differentDateFuel"

  private[paye] def validateFuelDateChoice(benefitType:Int) = optional(text)
    .verifying("error.paye.benefit.choice.mandatory", fuelDateChoice => isThereIfFuel(fuelDateChoice, benefitType))

  private[paye] def localDateMapping(benefitStartDate:Option[LocalDate]) = jodaLocalDate
    .verifying("error.paye.benefit.date.next.taxyear", date => date.isBefore(new LocalDate(TaxYearResolver() + 1, 4, 6)))
    .verifying("error.paye.benefit.date.greater.7.days", date => date.minusDays(7).isBefore(new LocalDate()))
    .verifying("error.paye.benefit.date.previous.taxyear", date => date.isAfter(new LocalDate(TaxYearResolver.lasDayOfPreviousTaxYear)))
    .verifying("error.paye.benefit.date.previous.startdate", date => isAfter(date, benefitStartDate))

  private[paye] def validateFuelDate(dates:CarFuelBenefitDates, benefitStartDate:Option[LocalDate]) = optional(jodaLocalDate)
    .verifying("error.paye.benefit.date.mandatory",  data => checkFuelDate(dates.fuelDateType, data))
    .verifying("error.paye.benefit.fuelwithdrawdate.before.carwithdrawdate", data => isAfterIfDefined(dates.carDate, data))
    .verifying("error.paye.benefit.date.previous.taxyear", data => if (differentDateForFuel(dates.fuelDateType)) { isAfterIfDefined(data , Some(TaxYearResolver.lasDayOfPreviousTaxYear))} else true)
    .verifying("error.paye.benefit.date.previous.startdate", data => if (differentDateForFuel(dates.fuelDateType)) { isAfterIfDefined(data, benefitStartDate)} else true)

  private[paye] def differentDateForFuel(dateOption:Option[String]):Boolean = {
    dateOption match {
      case Some(a) if a == FUEL_DIFFERENT_DATE => true
      case _ => false
    }
  }

  private def isAfterIfDefined(dateOne:Option[LocalDate], dateTwo:Option[LocalDate]):Boolean = {
    dateOne match {
      case Some(date) => isAfter(date, dateTwo)
      case _ => true
    }
  }

  private def isThereIfFuel(fuelDateChoice:Option[Any] , benefitType:Int):Boolean = {
    benefitType match {
      case CAR => fuelDateChoice.isDefined
      case _ => true
    }
  }

  private def isAfter(withdrawDate:LocalDate, startDate:Option[LocalDate]) : Boolean = {
    startDate match {
      case Some(startDate) => withdrawDate.isAfter(startDate)
      case _ => true
    }
  }

  private def checkFuelDate (optionFuelDate:Option[String], date:Option[LocalDate]):Boolean = {
    optionFuelDate match {
      case Some(a) if a == FUEL_DIFFERENT_DATE && date.isEmpty => false
      case _ => true
    }
  }
}
