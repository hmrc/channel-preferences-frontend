package controllers.paye.validation

import play.api.data.{Mapping, Form}
import play.api.data.Forms._
import uk.gov.hmrc.utils.DateTimeUtils._
import uk.gov.hmrc.utils.TaxYearResolver
import org.joda.time.{DateTimeZone, LocalDate}
import controllers.paye.CarBenefitFormFields._
import controllers.common.validators.{StopOnFirstFail, Validators}
import EngineCapacity._
import StopOnFirstFail._

object AddCarBenefitValidator extends Validators {

  private val carRegistrationDateCutoff = LocalDate.parse("1998-01-01")
  private val fuelTypeElectric = "electricity"
  private val fuelTypeOptions = Seq("diesel", fuelTypeElectric, "other")
  private val employerPayeFuelDateOption = "date"
  private val employerPayFuelOptions = Seq("false", "true", employerPayeFuelDateOption, "again")

  private[paye] case class CarBenefitValues(providedFromVal : Option[LocalDate],
                                     carUnavailableVal:  Option[String],
                                     numberOfDaysUnavailableVal:  Option[String],
                                     giveBackThisTaxYearVal:  Option[String],
                                     providedToVal: Option[LocalDate],
                                     carRegistrationDate: Option[LocalDate],
                                     employeeContributes: Option[String],
                                     employerContributes: Option[String],
                                     fuelType: Option[String],
                                     co2Figure: Option[String],
                                     co2NoFigure:Option[String],
                                     employerPayFuel:Option[String])

  private[paye] def datesForm(providedFromDefaultValue: LocalDate, providedToDefaultValue: LocalDate) = Form[CarBenefitValues](
    mapping(
      providedFrom -> dateTuple(false, Some(providedFromDefaultValue)),
      carUnavailable -> optional(text),
      numberOfDaysUnavailable -> optional(text),
      giveBackThisTaxYear -> optional(text),
      providedTo -> dateTuple(false, Some(providedToDefaultValue)),
      carRegistrationDate -> dateTuple(false, None),
      employeeContributes -> optional(text),
      employerContributes -> optional(text),
      fuelType -> optional(text),
      co2Figure -> optional(text),
      co2NoFigure -> optional(text),
      employerPayFuel -> optional(text)
    )(CarBenefitValues.apply)(CarBenefitValues.unapply)
  )

  private[paye] val validateListPrice = optional(number.verifying("error.paye.list_price_less_than_1000", e => e >= 1000)
                            .verifying("error.paye.list_price_greater_than_99999", e => e <= 99999)
                            ).verifying("error.paye.list_price_mandatory", e => e.isDefined)

  private[paye] def validateEmployerContribution(values: CarBenefitValues) : Mapping[Option[Int]] =
    values.employerContributes.map(_.toBoolean) match {
      case Some(true) => optional(number
        .verifying("error.paye.employer_contribution_greater_than_99999", e => e <= 99999)
        .verifying("error.paye.employer_contribution_less_than_0", e => e > 0 )
        ).verifying("error.paye.add_car_benefit.missing_employer_contribution", e => e.isDefined)
      case _ => ignored(None)
    }

  private[paye] def validateFuelType(values: CarBenefitValues): Mapping[Option[String]] =
     optional(text.verifying("error.paye.fuel_type_correct_option" , data => fuelTypeOptions.contains(data))
     .verifying("error.paye.fuel_type_electricity_must_be_registered_after_98", data => if(isRegisteredBeforeCutoff(values.carRegistrationDate)) {!isFuelTypeElectric(Some(data)) } else true)
     ).verifying("error.paye.answer_mandatory", data => data.isDefined)


  private[paye] def isRegisteredBeforeCutoff(carRegistrationDate: Option[LocalDate]): Boolean = carRegistrationDate.map(_.isBefore(carRegistrationDateCutoff)).getOrElse(false)

  private[paye] def validateEmployeeContribution(values: CarBenefitValues) : Mapping[Option[Int]] =
    values.employeeContributes.map(_.toBoolean) match {
      case Some(true) =>  optional(number.verifying("error.paye.employee_contribution_greater_than_9999", e => e <= 9999)
        .verifying("error.paye.employee_contribution_less_than_0", data => data > 0))
        .verifying("error.paye.add_car_benefit.missing_employee_contribution", data => data.isDefined)
      case _ => ignored(None)
  }

  private[paye] def validateEngineCapacity(values: CarBenefitValues) : Mapping[Option[String]] = optional(text
    .verifying("error.paye.non_valid_option" , data => engineCapacityOptions.contains(data))
    .verifying("error.paye.engine_capacity_must_be_blank_for_fuel_type_electricity" , data => engineCapacityEmpty(data) || !isFuelTypeElectric(values.fuelType))
  ).verifying("error.paye.engine_capacity_must_not_be_blank_for_fuel_type_not_electricity" , data => if(engineCapacityEmpty(data)) {isFuelTypeElectric(values.fuelType)} else true)

  private[paye] def validateEmployerPayFuel(values: CarBenefitValues) : Mapping[Option[String]] = optional(text
    .verifying("error.paye.non_valid_option" , data => employerPayFuelOptions.contains(data))
    .verifying("error.paye.employer_pay_fuel_must_not_have_days_unavailable" , data => isValidCompareTo(data, values.numberOfDaysUnavailableVal))
  ).verifying("error.paye.answer_mandatory", data => data.isDefined)

  private[paye] def validateGiveBackThisTaxYear(values: CarBenefitValues) : Mapping[Option[Boolean]] =
    optional(boolean).verifying("error.paye.answer_mandatory", data => data.isDefined)

  private[paye] def validateCarRegistrationDate(values: CarBenefitValues) : Mapping[Option[LocalDate]] =
    dateTuple.verifying("error.paye.answer_mandatory", data => data.isDefined)

  private[paye] def validateProvidedTo(values: CarBenefitValues) : Mapping[Option[LocalDate]] = values.giveBackThisTaxYearVal.map(_.toBoolean) match { 
    case Some(true) => dateInCurrentTaxYear.verifying("error.paye.providedTo_after_providedFrom", d => values.providedFromVal.get.isBefore(values.providedToVal.get))
      .verifying("error.paye.add_car_benefit.missing_car_return_date", data => !data.isEmpty)
    case _ => ignored(None)
  }

  private[paye] def validateNumberOfDaysUnavailable(values: CarBenefitValues) : Mapping[Option[Int]] = values.carUnavailableVal.map(_.toBoolean) match {
    case Some(true) => {
      optional(positiveInteger)
        .verifying("error.paye.number_max_3_chars", n => n.getOrElse(0) <= 999 )
        .verifying("error.paye.add_car_benefit.car_unavailable_too_long", e =>
          {values.numberOfDaysUnavailableVal.map(_.toInt).getOrElse(0) < daysBetween(values.providedFromVal.get, values.providedToVal.get)})
        .verifying("error.paye.add_car_benefit.missing_days_unavailable", data => !(values.carUnavailableVal.map(_.toBoolean).getOrElse(false) && data.isEmpty))
    }
    case _ => ignored(None)
  }

  private[paye] def validateNoCo2Figure(carBenefitValues: CarBenefitValues): Mapping[Option[Boolean]] = optional(boolean
    .verifying("error.paye.co2_figure_blank_for_electricity_fuel_type", data => !(data==true && isFuelTypeElectric(carBenefitValues.fuelType) && carBenefitValues.co2Figure.isEmpty))
  ) .verifying("error.paye.co2_figures_not_blank", co2NoFig => if(!isFuelTypeElectric(carBenefitValues.fuelType)){co2FiguresNotBlank(carBenefitValues.co2Figure, co2NoFig)} else true)

  private[paye] def validateCo2Figure(carBenefitValues: CarBenefitValues): Mapping[Option[Int]] = optional(number
    .verifying("error.paye.co2_figure_max_3_chars", e => (e <= 999))
    .verifying("error.paye.co2_figure_greater_than_zero", e => (e > 0))
    .verifying("error.paye.co2_figure_blank_for_electricity_fuel_type", data => !(isFuelTypeElectric(carBenefitValues.fuelType)))
    .verifying("error.paye.co2_figure_and_co2_no_figure_cannot_be_both_present", data => (carBenefitValues.co2NoFigure.getOrElse("") != "true"))
  )

  private def isFuelTypeElectric(fuelType:Option[String]) = fuelType.getOrElse("") == fuelTypeElectric

  private val dateInCurrentTaxYear: Mapping[Option[LocalDate]] = dateTuple.verifying(
    "error.paye.date_not_in_current_tax_year", data => isInCurrentTaxYear(data)
  )

  private def co2FiguresNotBlank(co2Figure:Option[_], co2NoFigure:Option[_]) = {
    co2NoFigure match {
      case Some(value) if value.toString == "true" => true
      case _ => co2Figure.isDefined
    }
  }

  private[paye] def validateDateFuelWithdrawn(values: CarBenefitValues): Mapping[Option[LocalDate]] = values.employerPayFuel match {
    case Some(employerPayeFuel) if employerPayeFuel == employerPayeFuelDateOption => dateTuple verifying StopOnFirstFail[Option[LocalDate]](
      constraint("error.paye.employer_pay_fuel_date_option_mandatory_withdrawn_date", (data) => data.isDefined),
      constraint("error.paye.date_not_in_current_tax_year", isInCurrentTaxYear),
      constraint("error.paye.fuel_withdraw_date_must_be_after_car_start_date", (data) => values.providedFromVal.isEmpty || data.get.isAfter(values.providedFromVal.get)),
      constraint("error.paye.fuel_withdraw_date_must_be_before_car_end_date", (data) => values.providedToVal.isEmpty || isEqualOrAfter(data.get,values.providedToVal.get))
    )
    case _ => ignored(None)
  }

  private def isInCurrentTaxYear = (data:Option[LocalDate]) =>  data match {
    case Some(d) => TaxYearResolver.taxYearInterval.contains(d.toDateTimeAtStartOfDay(DateTimeZone.UTC))
    case _ => true
  }

  private def isValidCompareTo(employerPayFuel:String , daysCarUnavailable:Option[String]) = {
    daysCarUnavailable match {
      case Some(days) =>  employerPayFuel != employerPayeFuelDateOption
      case _ => true
    }
  }

  private[paye] def validateProvidedFrom(timeSource: () => LocalDate) = {
    validateNotMoreThan7DaysFromNow(timeSource, dateInCurrentTaxYear)
  }


  def validateNotMoreThan7DaysFromNow(timeSource: () => LocalDate, dateInCurrentTaxYear:Mapping[Option[LocalDate]]): Mapping[Option[LocalDate]] = {
    dateInCurrentTaxYear.verifying("error.paye.date_within_7_days",
      data => data match {
        case Some(d) => daysBetween(timeSource(), d) <= 7
        case None => true
      })
  }
}
