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
import controllers.paye.validation.AddCarBenefitValidator._
import uk.gov.hmrc.common.microservice.keystore.KeyStoreMicroService
import scala.Some
import uk.gov.hmrc.common.microservice.domain.User
import controllers.paye.validation.AddCarBenefitValidator.CarBenefitValues
import controllers.common.service.MicroServices

class CarBenefitHomeController(timeSource: () => DateTime, keyStoreService: KeyStoreMicroService) extends BaseController with SessionTimeoutWrapper with Benefits with Validators {

  def this() = this(() => DateTimeUtils.now, MicroServices.keyStoreMicroService)

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

  private def getCarBenefitDates(request:Request[_]):CarBenefitValues = {
    datesForm.bindFromRequest()(request).value.get
  }

  private def carBenefitForm(carBenefitValues: CarBenefitValues) = Form[CarBenefitData](
    mapping(
      providedFrom -> validateProvidedFrom(timeSource),
      carUnavailable -> optional(boolean).verifying("error.paye.answer_mandatory", data => data.isDefined),
      numberOfDaysUnavailable -> validateNumberOfDaysUnavailable(carBenefitValues),
      giveBackThisTaxYear -> validateGiveBackThisTaxYear(carBenefitValues),
      providedTo -> validateProvidedTo(carBenefitValues),
      listPrice -> validateListPrice,
      employeeContributes -> optional(boolean).verifying("error.paye.answer_mandatory", data => data.isDefined),
      employeeContribution -> validateEmployeeContribution(carBenefitValues),
      employerContributes -> optional(boolean).verifying("error.paye.answer_mandatory", data => data.isDefined),
      employerContribution -> validateEmployerContribution(carBenefitValues)
    )(CarBenefitData.apply)(CarBenefitData.unapply)
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
          removeBenefitData => {
            keyStoreService.addKeyStoreEntry(s"AddCarBenefit:${user.oid}:$taxYear:$employmentSequenceNumber", "paye", "AddCarBenefitForm", removeBenefitData)
            Ok
          }
        )
      }
      case None => {
        Logger.debug(s"Unable to find employment for user ${user.oid} with sequence number ${employmentSequenceNumber}")
        BadRequest
      }
    }
  }
}

case class CarBenefitData(providedFrom: Option[LocalDate], carUnavailable: Option[Boolean], numberOfDaysUnavailable: Option[Int], giveBackThisTaxYear: Option[Boolean], providedTo: Option[LocalDate],
                          listPrice: Option[Int], employeeContributes: Option[Boolean], employeeContribution: Option[Int], employerContributes: Option[Boolean], employerContribution: Option[Int])

// TODO: Fix DateTimeUtils or test to make "DateTimeUtils.startOfCurrentTaxYear" to work

object CarBenefitFormFields {
  val providedFrom = "providedFrom"
  val carUnavailable = "carUnavailable"
  val numberOfDaysUnavailable = "numberOfDaysUnavailable"
  val giveCarBack = "giveCarBack"
  val giveBackThisTaxYear = "giveBackThisTaxYear"
  val providedTo = "providedTo"
  val listPrice = "listPrice"
  val employeeContributes = "employeeContributes"
  val employeeContribution = "employeeContribution"
  val employerContributes = "employerContributes"
  val employerContribution = "employerContribution"
}
