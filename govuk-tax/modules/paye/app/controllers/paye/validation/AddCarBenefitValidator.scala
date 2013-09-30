package controllers.paye.validation

import play.api.data.{Mapping, Form}
import play.api.data.Forms._
import scala.Some
import uk.gov.hmrc.utils.{DateTimeUtils, TaxYearResolver}
import org.joda.time.{DateTime, DateTimeZone, LocalDate}
import controllers.paye.CarBenefitFormFields._
import controllers.common.validators.Validators

object AddCarBenefitValidator extends Validators {

  private[paye] case class CarBenefitValues(providedFromVal : Option[LocalDate],
                                     carUnavailableVal:  Option[String],
                                     numberOfDaysUnavailableVal:  Option[String],
                                     giveBackThisTaxYearVal:  Option[String],
                                     providedToVal: Option[LocalDate],
                                     employeeContributes: Option[String],
                                     employerContributes: Option[String])

  private[paye] def datesForm(taxYearStart: LocalDate, taxYearEnd: LocalDate) = Form[CarBenefitValues](
    mapping(
      providedFrom -> dateTuple(false, Some(taxYearStart)),
      carUnavailable -> optional(text),
      numberOfDaysUnavailable -> optional(text),
      giveBackThisTaxYear -> optional(text),
      providedTo -> dateTuple(false, Some(taxYearEnd)),
      employeeContributes -> optional(text),
      employerContributes -> optional(text)
    )(CarBenefitValues.apply)(CarBenefitValues.unapply)
  )

  private[paye] val validateListPrice = optional(number.verifying("error.paye.list_price_less_than_1000", e => e >= 1000)
                            .verifying("error.paye.list_price_greater_than_99999", e => e <= 99999))
                            .verifying("error.paye.list_price_mandatory", e => {e.isDefined})

  private[paye] def validateEmployerContribution(values: CarBenefitValues) : Mapping[Option[Int]] =
    values.employerContributes.map(_.toBoolean) match {
      case Some(true) => optional(number
        .verifying("error.paye.employer_contribution_greater_than_99999", e => e <= 99999)
        .verifying("error.paye.employer_contribution_less_than_0", e => e > 0 ))
        .verifying("error.paye.add_car_benefit.missing_employer_contribution", e => e.isDefined )
      case _ => ignored(None)
    }

  private[paye] def validateEmployeeContribution(values: CarBenefitValues) : Mapping[Option[Int]] =
    values.employeeContributes.map(_.toBoolean) match {
      case Some(true) =>  optional(number.verifying("error.paye.employee_contribution_greater_than_9999", e => e <= 9999)
        .verifying("error.paye.employee_contribution_less_than_0", data => data > 0))
        .verifying("error.paye.add_car_benefit.missing_employee_contribution", data => data.isDefined)
      case _ => ignored(None)
  }

  private[paye] def validateGiveBackThisTaxYear(values: CarBenefitValues) : Mapping[Option[Boolean]] =
    optional(boolean).verifying("error.paye.answer_mandatory", data => data.isDefined)

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
          {values.numberOfDaysUnavailableVal.map(_.toInt).getOrElse(0) < DateTimeUtils.daysBetween(values.providedFromVal.get, values.providedToVal.get)})
        .verifying("error.paye.add_car_benefit.missing_days_unavailable", data => !(values.carUnavailableVal.map(_.toBoolean).getOrElse(false) && data.isEmpty))
    }
    case _ => ignored(None)
  }

  private val dateInCurrentTaxYear = dateTuple.verifying(
    "error.paye.date_not_in_current_tax_year",
    data =>  data match {
      case Some(d) => TaxYearResolver.taxYearInterval.contains(d.toDateTimeAtStartOfDay(DateTimeZone.UTC))
      case _ => true
    }
  )

  private[paye] def validateProvidedFrom(timeSource: () => DateTime) = {
    dateInCurrentTaxYear.verifying("error.paye.date_within_7_days",
      data => data match {
        case Some(d) => DateTimeUtils.daysBetween(timeSource().toLocalDate, d)  <= 7
        case None => true
      })
  }

}
