package controllers.paye.validation

import play.api.data.{Form, Mapping}
import play.api.data.Forms._
import uk.gov.hmrc.utils.DateTimeUtils._
import org.joda.time.{Interval, DateTimeZone, LocalDate}
import controllers.paye.CarBenefitFormFields._
import controllers.common.validators.{ExtractBoolean, StopOnFirstFail, Validators}
import models.paye.{CarBenefitData, EngineCapacity}
import EngineCapacity._
import StopOnFirstFail._
import controllers.paye.TaxYearSupport
import play.api.mvc.Request
import uk.gov.hmrc.common.microservice.paye.domain.AddCarBenefitConfirmationData
import play.api.i18n.Messages
import uk.gov.hmrc.common.FormBinders
import FormBinders.numberFromTrimmedString
import uk.gov.hmrc.utils.TaxYearResolver

object AddCarBenefitValidator extends Validators with TaxYearSupport {

  private val carRegistrationDateCutoff = LocalDate.parse("1998-01-01")
  private val fuelTypeOptions = Seq("diesel", AddCarBenefitConfirmationData.fuelTypeElectric, "other")
  private val employerPayeFuelDateOption = "date"
  private val employerPayFuelOptions = Seq("false", "true")

  private[paye] case class CarBenefitValues(providedFromVal: Option[LocalDate] = None,
                                            carUnavailableVal: Option[String] = None,
                                            numberOfDaysUnavailableVal: Option[String] = None,
                                            giveBackThisTaxYearVal: Option[String] = None,
                                            providedToVal: Option[LocalDate] = None,
                                            carRegistrationDate: Option[LocalDate] = None,
                                            employeeContributes: Option[String] = None,
                                            privateUsePayment: Option[String] = None,
                                            fuelType: Option[String] = None,
                                            co2Figure: Option[String] = None,
                                            co2NoFigure: Option[String] = None,
                                            employerPayFuel: Option[String] = None)

  def timeSource() = new LocalDate(DateTimeZone.UTC)

  private[paye] def getCarBenefitDates(request: Request[_]): CarBenefitValues = {
    validationlessForm.bindFromRequest()(request).value.get
  }

  private[paye] def validationlessForm = Form[CarBenefitValues](
    mapping(
      providedFrom -> dateTuple(false),
      carUnavailable -> optional(text),
      numberOfDaysUnavailable -> optional(text),
      giveBackThisTaxYear -> optional(text),
      providedTo -> dateTuple(false),
      carRegistrationDate -> dateTuple(false),
      employeeContributes -> optional(text),
      privateUsePayment -> optional(text),
      fuelType -> optional(text),
      co2Figure -> optional(text),
      co2NoFigure -> optional(text),
      employerPayFuel -> optional(text)
    )(CarBenefitValues.apply)(CarBenefitValues.unapply)
  )

  private[paye] def carBenefitForm(carBenefitValues: CarBenefitValues, timeSource: () => LocalDate = timeSource) = Form[CarBenefitData](
    mapping(
      providedFrom -> default(validateProvidedFrom(timeSource, taxYearInterval), Some(TaxYearResolver.startOfCurrentTaxYear)),
      carRegistrationDate -> validateCarRegistrationDate(timeSource),
      listPrice -> validateListPrice,
      employeeContributes -> optional(boolean).verifying("error.paye.answer_mandatory", data => data.isDefined),
      employeeContribution -> validateEmployeeContribution(carBenefitValues),
      privateUsePayment -> optional(boolean).verifying("error.paye.answer_mandatory", data => data.isDefined),
      privateUsePaymentAmount -> validatePrivateUsePayment(carBenefitValues),
      fuelType -> validateFuelType(carBenefitValues),
      co2Figure -> validateCo2Figure(carBenefitValues),
      co2NoFigure -> validateNoCo2Figure(carBenefitValues),
      engineCapacity -> validateEngineCapacity(carBenefitValues),
      employerPayFuel -> validateEmployerPayFuel(carBenefitValues),
      dateFuelWithdrawn -> validateDateFuelWithdrawn(carBenefitValues, taxYearInterval)
    )(CarBenefitData.apply)(CarBenefitData.unapply)
  )

  private[paye] val validateListPrice = optional(numberFromTrimmedString.verifying("error.paye.list_price_less_than_1000", e => e >= 1000)
    .verifying("error.paye.list_price_greater_than_99999", e => e <= 99999)
  ).verifying("error.paye.list_price_mandatory", e => e.isDefined)

  private[paye] def validatePrivateUsePayment(values: CarBenefitValues): Mapping[Option[Int]] =
    values.privateUsePayment match {
      case Some(ExtractBoolean(true)) => optional(numberFromTrimmedString
        .verifying("error.paye.employer_contribution_greater_than_99999", e => e <= 99999)
        .verifying("error.paye.employer_contribution_less_than_0", e => e > 0)
      ).verifying("error.paye.add_car_benefit.missing_employer_contribution", e => e.isDefined)
      case Some(ExtractBoolean(false)) => optional(numberFromTrimmedString).verifying("error.paye.add_car_benefit.extra_private_use_payment", _.isEmpty)
      case _ => ignored(None)
    }

  private[paye] def validateFuelType(values: CarBenefitValues): Mapping[Option[String]] =
    optional(text.verifying("error.paye.fuel_type_correct_option", data => fuelTypeOptions.contains(data))
      .verifying("error.paye.fuel_type_electricity_must_be_registered_after_98", data => if (isRegisteredBeforeCutoff(values.carRegistrationDate)) {!isFuelTypeElectric(Some(data))} else true)
    ).verifying("error.paye.answer_mandatory", data => data.isDefined)


  private[paye] def isRegisteredBeforeCutoff(carRegistrationDate: Option[LocalDate]): Boolean = carRegistrationDate.map(_.isBefore(carRegistrationDateCutoff)).getOrElse(false)


  private[paye] def validateEmployeeContribution(values: CarBenefitValues): Mapping[Option[Int]] =
    values.employeeContributes match {
      case Some(ExtractBoolean(true)) => optional(numberFromTrimmedString.verifying("error.paye.employee_contribution_greater_than_5000", e => e <= 5000)
        .verifying("error.paye.employee_contribution_less_than_0", data => data > 0))
        .verifying("error.paye.add_car_benefit.missing_employee_contribution", data => data.isDefined)
      case Some(ExtractBoolean(false)) =>
        optional(numberFromTrimmedString).verifying("error.paye.add_car_benefit.extra_employee_contribution", _.isEmpty)
      case _ => ignored(None)
    }

  private[paye] def validateEngineCapacity(values: CarBenefitValues): Mapping[Option[String]] = optional(text
    .verifying("error.paye.non_valid_option", data => engineCapacityOptions.contains(data))
  ).verifying("error.paye.engine_capacity_must_be_blank_for_fuel_type_electricity", data => data.isEmpty || !isFuelTypeElectric(values.fuelType))
    .verifying("error.paye.engine_capacity_must_not_be_blank_for_fuel_type_not_electricity", data => if (data.isEmpty) {isFuelTypeElectric(values.fuelType)} else true)

  private[paye] def validateEmployerPayFuel(values: CarBenefitValues): Mapping[Option[String]] = validateEmployerPayFuel(values.fuelType)

  private[paye] def validateEmployerPayFuelForAddFuelOnly(values: CarBenefitValues): Mapping[Option[String]] = validateEmployerPayFuelForAddFuelOnly(values.fuelType)

  private[paye] def validateEmployerPayFuelForAddFuelOnly(fuelType: Option[String]): Mapping[Option[String]] = optional(text
    .verifying("error.paye.non_valid_option", data => employerPayFuelOptions.contains(data))
    .verifying("error.paye.add_fuel_benefit.question1.employer_pay_fuel_cannot_be_false", data => data == "true")
  ).verifying("error.paye.answer_mandatory", data => data.isDefined)

  private[paye] def validateEmployerPayFuel(fuelType: Option[String]): Mapping[Option[String]] = optional(text
    .verifying("error.paye.non_valid_option", data => employerPayFuelOptions.contains(data))
    .verifying("error.paye.employer_cannot_pay_fuel_on_electric_cars", data => !isFuelTypeElectric(fuelType) || data == "false")
  ).verifying("error.paye.answer_mandatory", data => data.isDefined)

  private[paye] def validateGiveBackThisTaxYear(): Mapping[Option[Boolean]] =
    optional(boolean).verifying("error.paye.answer_mandatory", data => data.isDefined)

  private[paye] def validateCarRegistrationDate(timeSource: () => LocalDate): Mapping[Option[LocalDate]] =
    dateTuple.verifying("error.paye.registered_date_not_after_today", data => if (data.isDefined) !data.get.isAfter(timeSource()) else true)
      .verifying("error.paye.date_must_be_after_1900", data => if (data.isDefined) data.get.getYear >= 1900 else true)
      .verifying("error.paye.answer_mandatory", data => data.isDefined)

  private[paye] def validateProvidedTo(values: CarBenefitValues, taxYearInterval: Interval): Mapping[Option[LocalDate]] = values.giveBackThisTaxYearVal.map(_.toBoolean) match {
    case Some(true) => dateInCurrentTaxYear(taxYearInterval: Interval).verifying("error.paye.providedTo_after_providedFrom", d => getStartDate(values, taxYearInterval).isBefore(getEndDate(values, taxYearInterval)))
      .verifying("error.paye.add_car_benefit.missing_car_return_date", data => !data.isEmpty)
    case _ => ignored(None)
  }

  private def getStartDate(values: CarBenefitValues, taxYearInterval: Interval): LocalDate = values.providedFromVal.getOrElse(taxYearInterval.getStart.toLocalDate)

  private def getEndDate(values: CarBenefitValues, taxYearInterval: Interval): LocalDate = (values.giveBackThisTaxYearVal, values.providedToVal) match {
    case (Some("true"), Some(date)) => date
    case _ => taxYearInterval.getEnd.toLocalDate
  }

  private[paye] def validateNumberOfDaysUnavailable(values: CarBenefitValues, taxYearInterval: Interval): Mapping[Option[Int]] = values.carUnavailableVal.map(_.trim.toBoolean) match {
    case Some(true) => {
      optional(numberFromTrimmedString
        .verifying("error.paye.number_of_days_unavailable_less_than_0", n => n > 0)
        .verifying("error.paye.number_max_3_chars", n => n <= 999)
        .verifying(Messages("error.paye.add_car_benefit.car_unavailable_too_long", currentTaxYear.toString), unavailableDays => acceptableNumberOfDays(unavailableDays, values, taxYearInterval))
      ).verifying("error.paye.add_car_benefit.missing_days_unavailable", data => data.isDefined)
    }
    case _ => ignored(None)
  }

  private def acceptableNumberOfDays(numberOfDays: Int, values: CarBenefitValues, taxYearInterval: Interval): Boolean = {
    val startOfTaxYear = taxYearInterval.getStart.toLocalDate
    val dateCarMadeVailable = values.providedFromVal.getOrElse(startOfTaxYear)
    val startDate = if (dateCarMadeVailable.isBefore(startOfTaxYear)) startOfTaxYear else dateCarMadeVailable
    numberOfDays < daysBetween(startDate, getEndDate(values, taxYearInterval))
  }

  private[paye] def validateNoCo2Figure(carBenefitValues: CarBenefitValues): Mapping[Option[Boolean]] = optional(boolean
    .verifying("error.paye.co2_figure_blank_for_electricity_fuel_type", data => !(data == true && isFuelTypeElectric(carBenefitValues.fuelType) && carBenefitValues.co2Figure.isEmpty))
  ).verifying("error.paye.co2_figures_not_blank", co2NoFig => if (!isFuelTypeElectric(carBenefitValues.fuelType)) {co2FiguresNotBlank(carBenefitValues.co2Figure, co2NoFig)} else true)

  private[paye] def validateCo2Figure(carBenefitValues: CarBenefitValues): Mapping[Option[Int]] = optional(numberFromTrimmedString
    .verifying("error.paye.co2_figure_max_3_chars", e => (e <= 999))
    .verifying("error.paye.co2_figure_greater_than_zero", e => (e > 0))
    .verifying("error.paye.co2_figure_blank_for_electricity_fuel_type", data => !(isFuelTypeElectric(carBenefitValues.fuelType)))
    .verifying("error.paye.co2_figure_and_co2_no_figure_cannot_be_both_present", data => (carBenefitValues.co2NoFigure.getOrElse("") != "true"))
  )

  private def isFuelTypeElectric(fuelType: Option[String]) = fuelType.getOrElse("") == AddCarBenefitConfirmationData.fuelTypeElectric

  private[validation] def dateInCurrentTaxYear(taxYearInterval: Interval): Mapping[Option[LocalDate]] = dateTuple.verifying(
    Messages("error.paye.date_not_in_current_tax_year", currentTaxYear.toString, (currentTaxYear + 1).toString), data => isInCurrentTaxYear(data, taxYearInterval)
  )

  private def co2FiguresNotBlank(co2Figure: Option[_], co2NoFigure: Option[_]) = {
    co2NoFigure match {
      case Some(value) if value.toString == "true" => true
      case _ => co2Figure.isDefined
    }
  }

  private[paye] def validateDateFuelWithdrawn(values: CarBenefitValues, taxYearInterval: Interval): Mapping[Option[LocalDate]] = values.employerPayFuel match {
    case Some(employerPayeFuel) if employerPayeFuel == employerPayeFuelDateOption => dateTuple verifying StopOnFirstFail[Option[LocalDate]](
      constraint("error.paye.employer_pay_fuel_date_option_mandatory_withdrawn_date", (data) => data.isDefined),
      constraint(Messages("error.paye.date_not_in_current_tax_year", currentTaxYear.toString, (currentTaxYear + 1).toString), data => isInCurrentTaxYear(data, taxYearInterval)),
      constraint("error.paye.fuel_withdraw_date_must_be_after_car_start_date", (data) => data.get.isAfter(getStartDate(values, taxYearInterval))),
      constraint("error.paye.fuel_withdraw_date_must_be_before_car_end_date", (data) => isEqualOrAfter(data.get, getEndDate(values, taxYearInterval)))
    )
    case _ => ignored(None)
  }

  private def isEqualOrAfter(date: LocalDate, laterDate: LocalDate): Boolean = date.isEqual(laterDate) || date.isBefore(laterDate)

  private def isInCurrentTaxYear = (data: Option[LocalDate], taxYearInterval: Interval) =>
    data match {
      case Some(d) => taxYearInterval.contains(d.toDateTimeAtStartOfDay(DateTimeZone.UTC))
      case _ => true
    }


  private[paye] def validateProvidedFrom(timeSource: () => LocalDate, taxYearInterval: Interval) = {
    validateNotMoreThan7DaysFromNow(timeSource, dateInCurrentTaxYear(taxYearInterval))
  }


  private def validateNotMoreThan7DaysFromNow(timeSource: () => LocalDate, dateInCurrentTaxYear: Mapping[Option[LocalDate]]): Mapping[Option[LocalDate]] = {
    dateInCurrentTaxYear.verifying("error.paye.date_within_7_days",
      data =>
        data match {
          case Some(d) => daysBetween(timeSource(), d) <= 7
          case None => true
        })
  }
}