package controllers.paye.validation

import play.api.data.Forms._
import org.joda.time.{Interval, LocalDate}
import controllers.common.validators.Validators
import models.paye.CarFuelBenefitDates
import play.api.data.Mapping

object RemoveBenefitValidator extends Validators {

  private[paye] val FUEL_DIFFERENT_DATE = "differentDateFuel"

  private[paye] def validateFuelDateChoice(carBenefitWithUnremoved:Boolean) = optional(text)
    .verifying("error.paye.benefit.choice.mandatory", fuelDateChoice => verifyFuelDate(fuelDateChoice, carBenefitWithUnremoved))

  private[paye] def localDateMapping(benefitStartDate:Option[LocalDate], today: LocalDate, taxYearInterval:Interval) = mandatoryDateTuple("error.paye.benefit.date.mandatory")
    .verifying("error.paye.benefit.date.next.taxyear", date => date.isBefore(taxYearInterval.getEnd.toLocalDate))
    .verifying("error.paye.benefit.date.greater.7.days", date => date.minusDays(7).isBefore(today))
    .verifying("error.paye.benefit.date.previous.taxyear", date => date.isAfter(taxYearInterval.getStart.toLocalDate.minusDays(1)))
    .verifying("error.paye.benefit.date.previous.startdate", date => isAfter(date, benefitStartDate))

  private[paye] def validateFuelDate(dates:Option[CarFuelBenefitDates], benefitStartDate:Option[LocalDate], taxYearInterval:Interval) : Mapping[Option[LocalDate]] = dates.get.fuelDateType.getOrElse("") match {
    case FUEL_DIFFERENT_DATE => dateTuple
    .verifying("error.paye.benefit.date.mandatory",  data => if(dates.isDefined) {checkFuelDate(dates.get.fuelDateType, data)} else true)
    .verifying("error.paye.benefit.fuelwithdrawdate.before.carwithdrawdate", data => if(dates.isDefined) { !isAfterIfDefined(data, dates.get.carDate)} else true)
    .verifying("error.paye.benefit.date.previous.taxyear", data => if (dates.isDefined && differentDateForFuel(dates.get.fuelDateType)) { isAfterIfDefined(data , Some(taxYearInterval.getStart.toLocalDate.minusDays(1)))} else true)
    .verifying("error.paye.benefit.date.previous.startdate", data => if (dates.isDefined && differentDateForFuel(dates.get.fuelDateType)) { isAfterIfDefined(data, benefitStartDate)} else true)
    case _ => ignored(None)
  }

  private[paye] def differentDateForFuel(dateOption:Option[String]):Boolean = {
    dateOption match {
      case Some(a) if a == FUEL_DIFFERENT_DATE => true
      case _ => false
    }
  }

  private def isAfterIfDefined(left:Option[LocalDate], right:Option[LocalDate]):Boolean = {
    left match {
      case Some(aDate) => isAfter(aDate, right)
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
      case Some(dateOfStart) => withdrawDate.isAfter(dateOfStart)
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
