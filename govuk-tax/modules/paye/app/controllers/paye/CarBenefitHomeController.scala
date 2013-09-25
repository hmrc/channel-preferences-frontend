package controllers.paye

import controllers.common.{CarBenefitHomeRedirect, SessionTimeoutWrapper, BaseController}
import play.api.mvc.{Result, Request}
import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import uk.gov.hmrc.common.microservice.paye.domain.Employment._
import models.paye.BenefitTypes
import play.api.Logger
import org.joda.time._
import uk.gov.hmrc.utils.{TaxYearResolver, DateTimeUtils}
import play.api.data.Form
import play.api.data.Forms._
import CarBenefitFormFields._
import controllers.common.validators.Validators
import uk.gov.hmrc.common.microservice.domain.User
import controllers.paye.validation.AddCarBenefitValidator._

class CarBenefitHomeController(timeSource: () => DateTime) extends BaseController with SessionTimeoutWrapper with Benefits with Validators {

  def this() = this(() => DateTimeUtils.now)

  def carBenefitHome = WithSessionTimeoutValidation {
    AuthorisedForIdaAction(taxRegime = Some(PayeRegime), redirectCommand = Some(CarBenefitHomeRedirect)) {
      user => request => carBenefitHomeAction(user, request)
    }
  }

  def startAddCarBenefit(taxYear: Int, employmentSequenceNumber: Int) = WithSessionTimeoutValidation {
    AuthorisedForIdaAction(taxRegime = Some(PayeRegime), redirectCommand = Some(CarBenefitHomeRedirect)) {
      user => request => startAddCarBenefitAction(user, request, taxYear, employmentSequenceNumber)
    }
  }

  def saveAddCarBenefit(taxYear: Int, employmentSequenceNumber: Int) = WithSessionTimeoutValidation {
    AuthorisedForIdaAction(taxRegime = Some(PayeRegime)) {
      user => request => saveAddCarBenefitAction(user, request, taxYear, employmentSequenceNumber)
    }
  }

  private[paye] val carBenefitHomeAction: ((User, Request[_]) => Result) = (user, request) => {
    val currentTaxYear = TaxYearResolver.currentTaxYear
    user.regimes.paye.get.employments(currentTaxYear).find(_.employmentType == primaryEmploymentType) match {
      case Some(employment) => {
        val carBenefit = findExistingBenefit(user, employment.sequenceNumber, BenefitTypes.CAR)
        val fuelBenefit = findExistingBenefit(user, employment.sequenceNumber, BenefitTypes.FUEL)

        Ok(views.html.paye.car_benefit_home(carBenefit, fuelBenefit, employment.employerName, employment.sequenceNumber, currentTaxYear))
      }
      case None => {
        Logger.debug(s"Unable to find current employment for user ${user.oid}")
        InternalServerError
      }
    }
  }

  private def getCarBenefitDates(request:Request[_]):CarBenefitDates = {
    datesForm.bindFromRequest()(request).value.get
  }

  private def carBenefitForm(carBenefitDates: CarBenefitDates) = Form[CarBenefitData](
    mapping(
      providedFrom -> verifyProvidedFrom(timeSource),
      carUnavailable -> tuple(
        carUnavailable -> optional(boolean).verifying("error.paye.answer_mandatory", data => data.isDefined),
        numberOfDaysUnavailable -> validateNumberOfDaysUnavailable(carBenefitDates)
      ).verifying("error.paye.add_car_benefit.missing_days_unavailable", data => !(data._1.getOrElse(false) && data._2.isEmpty)),
      giveCarBack -> tuple(
        giveBackThisTaxYear -> optional(boolean).verifying("error.paye.answer_mandatory", data => data.isDefined),
        providedTo -> validateProvidedTo(carBenefitDates)
      ).verifying("error.paye.add_car_benefit.missing_car_return_date", data => !(data._1.getOrElse(false) && data._2.isEmpty)),
      listPrice -> optional(number.verifying("error.paye.list_price_less_than_1000", e => e >= 1000)
                            .verifying("error.paye.list_price_greater_than_99999", e => e <= 99999))
                            .verifying("error.paye.list_price_mandatory", e => {e.isDefined}),
      employeeContribution -> tuple(
        employeeContributes -> optional(boolean).verifying("error.paye.answer_mandatory", data => data.isDefined),
        employeeContribution -> optional(positiveInteger.verifying("error.paye.employee_contribution_greater_than_9999", e => e <= 9999))
      ).verifying("error.paye.add_car_benefit.missing_employee_contribution", data => !(data._1.getOrElse(false) && data._2.isEmpty)),
      employerContribution -> tuple(
        employerContributes -> optional(boolean).verifying("error.paye.answer_mandatory", data => data.isDefined),
        employerContribution -> optional(positiveInteger.verifying("error.paye.employer_contribution_greater_than_99999", e => e <= 99999))
      ).verifying("error.paye.add_car_benefit.missing_employer_contribution", data => !(data._1.getOrElse(false) && data._2.isEmpty))

    )(CarBenefitData.fromFormData)(CarBenefitData.toFormData)
  )

  private[paye] val startAddCarBenefitAction: ((User, Request[_], Int, Int) => Result) = (user, request, taxYear, employmentSequenceNumber) => {
    val dates = getCarBenefitDates(request)
    user.regimes.paye.get.employments(taxYear).find(_.sequenceNumber == employmentSequenceNumber) match {
      case Some(employment) => {
        Ok(views.html.paye.add_car_benefit_form(carBenefitForm(dates), employment.employerName, taxYear, employmentSequenceNumber))
      }
      case None => {
        Logger.debug(s"Unable to find employment for user ${user.oid} with sequence number ${employmentSequenceNumber}")
        BadRequest
      }
    }
  }

  private[paye] val saveAddCarBenefitAction: ((User, Request[_], Int, Int) => Result) = (user, request, taxYear, employmentSequenceNumber) => {
    user.regimes.paye.get.employments(taxYear).find(_.sequenceNumber == employmentSequenceNumber) match {
      case Some(employment) => {
        val dates = getCarBenefitDates(request)
        carBenefitForm(dates).bindFromRequest()(request).fold(
          errors => {
            BadRequest(views.html.paye.add_car_benefit_form(errors, employment.employerName, taxYear, employmentSequenceNumber))
          },
          removeBenefitData => Ok
        )
      }
      case None => {
        Logger.debug(s"Unable to find employment for user ${user.oid} with sequence number ${employmentSequenceNumber}")
        BadRequest
      }
    }
  }
}

case class CarBenefitData(providedFrom: Option[LocalDate], carUnavailable: Boolean, numberOfDaysUnavailable: Option[Int], giveBackThisTaxYear: Boolean, providedTo: Option[LocalDate],
                          listPrice: Option[Int], employeeContributes: Boolean, employeeContribution: Option[Int], employerContributes: Boolean, employerContribution: Option[Int])

object CarBenefitData {
  // TODO: Fix DateTimeUtils or test to make "DateTimeUtils.startOfCurrentTaxYear" to work
  def fromFormData(providedFrom: Option[LocalDate], carUnavailable : (Option[Boolean], Option[Int]),
                   giveCarBack: (Option[Boolean], Option[LocalDate]) = (None, Some(TaxYearResolver.endOfCurrentTaxYear)),
                   listPrice: Option[Int], employeeContribution: (Option[Boolean], Option[Int]), employerContribution: (Option[Boolean], Option[Int])) = {
    CarBenefitData(providedFrom, carUnavailable._1.getOrElse(false), carUnavailable._2, giveCarBack._1.getOrElse(false), giveCarBack._2,
      listPrice, employeeContribution._1.getOrElse(false), employeeContribution._2, employerContribution._1.getOrElse(false), employerContribution._2)
  }

  def toFormData(data: CarBenefitData) = {

    Some(data.providedFrom, (Some(data.carUnavailable), data.numberOfDaysUnavailable), (Some(data.giveBackThisTaxYear), data.providedTo),
      data.listPrice, (Some(data.employeeContributes), data.employeeContribution), (Some(data.employerContributes), data.employerContribution))
  }

}


object CarBenefitFormFields {
  val providedFrom = "providedFrom"
  val carUnavailable = "carUnavailable"
  val numberOfDaysUnavailable = "numberOfDaysUnavailable"
  val qualifiedCarUnavailable = carUnavailable + "." + carUnavailable
  val qualifiedNumberOfDaysUnavailable = carUnavailable + "." + numberOfDaysUnavailable
  val giveCarBack = "giveCarBack"
  val giveBackThisTaxYear = "giveBackThisTaxYear"
  val providedTo = "providedTo"
  val qualifiedGiveBackThisTaxYear = giveCarBack + "." + giveBackThisTaxYear
  val qualifiedProvidedTo = giveCarBack + "." + providedTo
  val listPrice = "listPrice"
  val employeeContributes = "employeeContributes"
  val employeeContribution = "employeeContribution"
  val qualifiedEmployeeContributes = employeeContribution + "." + employeeContributes
  val qualifiedEmployeeContribution = employeeContribution + "." + employeeContribution
  val employerContributes = "employerContributes"
  val employerContribution = "employerContribution"
  val qualifiedEmployerContributes = employerContribution + "." + employerContributes
  val qualifiedEmployerContribution = employerContribution + "." + employerContribution
}
