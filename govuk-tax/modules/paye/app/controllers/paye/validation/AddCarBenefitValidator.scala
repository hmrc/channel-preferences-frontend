package controllers.paye.validation

import play.api.data.{Mapping, Form}
import play.api.data.Forms._
import scala.Some
import uk.gov.hmrc.utils.{DateTimeUtils, TaxYearResolver}
import org.joda.time.{DateTime, DateTimeZone, LocalDate}
import controllers.paye.CarBenefitFormFields._
import controllers.common.validators.Validators

object AddCarBenefitValidator extends Validators {

  private[paye] case class CarBenefitDates(providedFromVal : Option[LocalDate],
                                     carUnavailableVal:  Option[String],
                                     numberOfDaysUnavailableVal:  Option[String],
                                     giveBackThisTaxYearVal:  Option[String],
                                     providedToVal: Option[LocalDate])

  private[paye] def datesForm() = Form[CarBenefitDates](
  mapping(
    providedFrom -> dateTuple(false, Some(TaxYearResolver.startOfCurrentTaxYear)),
    qualifiedCarUnavailable -> optional(text),
    qualifiedNumberOfDaysUnavailable -> optional(text),
    qualifiedGiveBackThisTaxYear -> optional(text),
    qualifiedProvidedTo -> dateTuple(false, Some(TaxYearResolver.endOfCurrentTaxYear))
  )(CarBenefitDates.apply)(CarBenefitDates.unapply)
  )

  private[paye] def validateProvidedTo(dates: CarBenefitDates) : Mapping[Option[LocalDate]] = dates.giveBackThisTaxYearVal.map(_.toBoolean) match {
    case Some(true) => dateInCurrentTaxYear.verifying("error.paye.providedTo_after_providedFrom", d => dates.providedFromVal.get.isBefore(dates.providedToVal.get))
    case _ => ignored(None)
  }

  private[paye] def validateNumberOfDaysUnavailable(dates: CarBenefitDates) : Mapping[Option[Int]] = dates.carUnavailableVal.map(_.toBoolean) match {
    case Some(true) => {
      optional(positiveInteger)
        .verifying("error.number", n => n.getOrElse(0) <= 999 )
        .verifying("error.paye.add_car_benefit.car_unavailable_too_long", e =>
          {dates.numberOfDaysUnavailableVal.getOrElse("0").toInt < DateTimeUtils.daysBetween(dates.providedFromVal.get, dates.providedToVal.get)})
    }
    case _ => {
      optional(ignored(0))
    }
  }

  private val dateInCurrentTaxYear = dateTuple.verifying(
    "error.paye.date_not_in_current_tax_year",
    data =>  data match {
      case Some(d) => TaxYearResolver.taxYearInterval.contains(d.toDateTimeAtStartOfDay(DateTimeZone.UTC))
      case _ => true
    }
  )

  private[paye] def verifyProvidedFrom(timeSource: () => DateTime) = {
    dateInCurrentTaxYear.verifying("error.paye.date_within_7_days",
      data => data match {
        case Some(d) => DateTimeUtils.daysBetween(timeSource().toLocalDate, d)  <= 7
        case None => true
      })
  }

}
