package controllers.paye.validation

import play.api.data.Forms._
import org.joda.time.{Interval, LocalDate}
import controllers.common.validators.Validators
import play.api.data.{Mapping, Form}
import uk.gov.hmrc.utils.DateTimeUtils._
import models.paye.{RemoveCarBenefitFormData, CarFuelBenefitDates}
import scala.Some
import play.api.i18n.Messages
import controllers.paye.TaxYearSupport
import views.formatting.Dates

object RemoveBenefitValidator extends Validators with TaxYearSupport {

  private[paye] case class RemoveCarBenefitFormDataValues(withdrawDateVal: Option[LocalDate],
                                                       carUnavailableVal: Option[String],
                                                       numberOfDaysUnavailableVal: Option[String] = None,
                                                       removeEmployeeContributesVal: Option[String],
                                                       removeEmployeeContributionVal: Option[String] = None,
                                                       fuelDateChoiceVal: Option[String],
                                                       fuelWithdrawDateVal: Option[LocalDate] = None)
  private[paye] object RemoveCarBenefitFormDataValues {
    def apply(data: RemoveCarBenefitFormData): RemoveCarBenefitFormDataValues = {
      RemoveCarBenefitFormDataValues(
        Some(data.withdrawDate),
        data.carUnavailable.map{_.toString},
        data.numberOfDaysUnavailable.map{_.toString},
        data.removeEmployeeContributes.map{_.toString},
        data.removeEmployeeContribution.map{_.toString},
        data.fuelDateChoice,
        data.fuelWithdrawDate
      )
    }
  }

  private[paye] def validationlessForm = Form[RemoveCarBenefitFormDataValues](
    mapping(
      "withdrawDate" -> dateTuple(validate = false),
      "carUnavailable" -> optional(text),
      "numberOfDaysUnavailable" -> optional(text),
      "removeEmployeeContributes" -> optional(text),
      "removeEmployeeContribution" -> optional(text),
      "fuelRadio" -> optional(text),
      "fuelWithdrawDate" -> dateTuple(validate = false)
    )(RemoveCarBenefitFormDataValues.apply)(RemoveCarBenefitFormDataValues.unapply)
  )

  private[paye] val FUEL_DIFFERENT_DATE = "differentDateFuel"

  private[paye] def validateMandatoryBoolean: Mapping[Option[Boolean]] = optional(boolean).verifying("error.paye.answer_mandatory", data => data.isDefined)

  private[paye] def validateNumberOfDaysUnavailable(values: Option[RemoveCarBenefitFormDataValues], benefitStartDate: LocalDate, taxYearInterval: Interval): Mapping[Option[Int]] = {
    values.flatMap(s => s.carUnavailableVal.map(_.toBoolean)) match {
    case Some(true) => {
      optional(number
        .verifying("error.paye.remove_car_benefit.question2.number_of_days_unavailable_less_than_0", n => n > 0)
        .verifying(Messages("error.paye.remove_car_benefit.question2.car_unavailable_too_long", currentTaxYear.toString), unavailableDays => acceptableNumberOfDays(unavailableDays, values, benefitStartDate, taxYearInterval))
      ).verifying("error.paye.remove_car_benefit.question2.missing_days_unavailable", data => data.isDefined)
    }
    case _ => ignored(None)
  }
  }

  private def acceptableNumberOfDays(numberOfDays: Int, values: Option[RemoveCarBenefitFormDataValues], benefitStartDate: LocalDate, taxYearInterval: Interval): Boolean = {
    val startOfTaxYear = taxYearInterval.getStart.toLocalDate
    val startDate = if (benefitStartDate.isBefore(startOfTaxYear)) startOfTaxYear else benefitStartDate
    numberOfDays < daysBetween(startDate, getEndDate(values, taxYearInterval))
  }

  private def getEndDate(values: Option[RemoveCarBenefitFormDataValues], taxYearInterval: Interval) = values.flatMap(v => v.withdrawDateVal).getOrElse(taxYearInterval.getEnd.toLocalDate)

  private[paye] def validateEmployeeContribution(values: Option[RemoveCarBenefitFormDataValues]): Mapping[Option[Int]] =
    values.flatMap(s => s.removeEmployeeContributesVal.map(_.toBoolean)) match {
      case Some(true) => optional(number
        .verifying("error.paye.remove_car_benefit.question3.number_max_5_chars", e => e <= 99999)
        .verifying("error.paye.remove_car_benefit.question3.number_less_than_0", data => data > 0))
        .verifying("error.paye.remove_car_benefit.question3.missing_employee_contribution", data => data.isDefined)
      case _ => ignored(None)
    }

  private[paye] def validateFuelDateChoice(carBenefitWithUnremoved: Boolean) = optional(text)
    .verifying("error.paye.benefit.choice.mandatory", fuelDateChoice => verifyFuelDate(fuelDateChoice, carBenefitWithUnremoved))

  private[paye] def localDateMapping(today: LocalDate, taxYearInterval: Interval, fuelDateWithdrawn: Option[LocalDate] = None, specificMapping: (Mapping[LocalDate]) => Mapping[LocalDate]) = {
    val theMapping = mandatoryDateTuple("error.paye.benefit.date.mandatory")
      .verifying(Messages("error.paye.benefit.date.next.taxyear", currentTaxYear.toString, (currentTaxYear+1).toString), date => date.isBefore(taxYearInterval.getEnd.toLocalDate))
      .verifying("error.paye.benefit.date.greater.7.days", date => date.minusDays(7).isBefore(today))
      .verifying(Messages("error.paye.benefit.date.previous.taxyear", currentTaxYear.toString, (currentTaxYear+1).toString), date => date.isAfter(taxYearInterval.getStart.toLocalDate.minusDays(1)))
      .verifying(Messages("error.paye.benefit.carwithdrawdate.before.fuelwithdrawdate", Dates.formatDate(fuelDateWithdrawn, "Unknown date")), carWithdrawn => !fuelDateWithdrawn.exists(fuelWithdrawn => fuelWithdrawn.isAfter(carWithdrawn)))

    specificMapping(theMapping)
  }

  private[paye] def fuelBenefitMapping(benefitStartDate: Option[LocalDate])(mapping:Mapping[LocalDate]) = {
    mapping.verifying("error.paye.fuel_withdraw_date_must_be_after_car_start_date", date => isAfter(date, benefitStartDate))
  }

  private[paye] def carBenefitMapping(benefitStartDate: Option[LocalDate])(mapping:Mapping[LocalDate]) = {
    mapping.verifying("error.paye.benefit.date.previous.startdate", date => isAfter(date, benefitStartDate))
  }

  // TODO: Fix this. Naked get on dates means it is not Optional!
  private[paye] def validateFuelDate(dates: Option[CarFuelBenefitDates], benefitStartDate: Option[LocalDate], taxYearInterval: Interval): Mapping[Option[LocalDate]] = dates.get.fuelDateType.getOrElse("") match {
    case FUEL_DIFFERENT_DATE => dateTuple
      .verifying("error.paye.benefit.date.mandatory", data => if (dates.isDefined) {
      checkFuelDate(dates.get.fuelDateType, data)
    } else true)
      .verifying("error.paye.benefit.fuelwithdrawdate.before.carwithdrawdate", data => if (dates.isDefined) {
      !isAfterIfDefined(data, dates.get.carDate)
    } else true)
      .verifying(Messages("error.paye.benefit.date.previous.taxyear", currentTaxYear.toString, (currentTaxYear+1).toString), data => if (dates.isDefined && differentDateForFuel(dates.get.fuelDateType)) {
      isAfterIfDefined(data, Some(taxYearInterval.getStart.toLocalDate.minusDays(1)))
    } else true)
      .verifying("error.paye.benefit.date.previous.startdate", data => if (dates.isDefined && differentDateForFuel(dates.get.fuelDateType)) {
      isAfterIfDefined(data, benefitStartDate)
    } else true)
    case _ => ignored(None)
  }

  private[paye] def differentDateForFuel(dateOption: Option[String]): Boolean = {
    dateOption match {
      case Some(a) if a == FUEL_DIFFERENT_DATE => true
      case _ => false
    }
  }

  private def isAfterIfDefined(left: Option[LocalDate], right: Option[LocalDate]): Boolean = {
    left match {
      case Some(aDate) => isAfter(aDate, right)
      case _ => true
    }
  }

  private def verifyFuelDate(fuelDateChoice: Option[Any], carBenefitWithUnremoved: Boolean): Boolean = {
    if (carBenefitWithUnremoved) {
      fuelDateChoice.isDefined
    } else true
  }

  private def isAfter(withdrawDate: LocalDate, startDate: Option[LocalDate]): Boolean = {
    startDate match {
      case Some(dateOfStart) => withdrawDate.isAfter(dateOfStart)
      case _ => true
    }
  }

  private def checkFuelDate(optionFuelDate: Option[String], date: Option[LocalDate]): Boolean = {
    optionFuelDate match {
      case Some(a) if a == FUEL_DIFFERENT_DATE && date.isEmpty => false
      case _ => true
    }
  }
}
