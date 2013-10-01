package controllers.paye.validation

import play.api.data.Forms._
import org.joda.time.LocalDate
import uk.gov.hmrc.utils.TaxYearResolver
import controllers.common.validators.Validators
import models.paye.CarFuelBenefitDates
import scala.Some
import uk.gov.hmrc.utils.TaxYearResolver
import play.api.data.Mapping

object RemoveBenefitValidator extends Validators {

  private val FUEL_DIFFERENT_DATE = "differentDateFuel"

  private[paye] def validateFuelDateChoice(carBenefitWithUnremoved:Boolean) = optional(text)
    .verifying("error.paye.benefit.choice.mandatory", fuelDateChoice => verifyFuelDate(fuelDateChoice, carBenefitWithUnremoved))

  private[paye] def localDateMapping(benefitStartDate:Option[LocalDate]) = mandatoryDateTuple("error.paye.benefit.date.mandatory")
    .verifying("error.paye.benefit.date.next.taxyear", date => date.isBefore(TaxYearResolver.endOfCurrentTaxYear))
    .verifying("error.paye.benefit.date.greater.7.days", date => date.minusDays(7).isBefore(new LocalDate()))
    .verifying("error.paye.benefit.date.previous.taxyear", date => date.isAfter(TaxYearResolver.startOfCurrentTaxYear.minusDays(1)))
    .verifying("error.paye.benefit.date.previous.startdate", date => isAfter(date, benefitStartDate))

  private[paye] def validateFuelDate(dates:Option[CarFuelBenefitDates], benefitStartDate:Option[LocalDate]) : Mapping[Option[LocalDate]] = dates.get.fuelDateType.getOrElse("") match {
    case FUEL_DIFFERENT_DATE => dateTuple
    .verifying("error.paye.benefit.date.mandatory",  data => if(dates.isDefined) {checkFuelDate(dates.get.fuelDateType, data)} else true)
    .verifying("error.paye.benefit.fuelwithdrawdate.before.carwithdrawdate", data => if(dates.isDefined) {isAfterIfDefined(dates.get.carDate, data)} else true)
    .verifying("error.paye.benefit.date.previous.taxyear", data => if (dates.isDefined && differentDateForFuel(dates.get.fuelDateType)) { isAfterIfDefined(data , Some(TaxYearResolver.startOfCurrentTaxYear.minusDays(1)))} else true)
    .verifying("error.paye.benefit.date.previous.startdate", data => if (dates.isDefined && differentDateForFuel(dates.get.fuelDateType)) { isAfterIfDefined(data, benefitStartDate)} else true)
    case _ => ignored(None)
  }

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

  private def verifyFuelDate(fuelDateChoice:Option[Any], carBenefitWithUnremoved:Boolean):Boolean = {
    if(carBenefitWithUnremoved){
      fuelDateChoice.isDefined
    } else true
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
