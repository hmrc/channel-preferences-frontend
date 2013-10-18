package controllers.paye

import controllers.common.BaseController
import play.api.mvc.{Result, Request}
import uk.gov.hmrc.common.microservice.paye.domain._
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
import controllers.common.service.MicroServices
import uk.gov.hmrc.common.microservice.paye.PayeMicroService
import views.html.paye.add_car_benefit_review
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.paye.domain.AddCarBenefitConfirmationData

class CarBenefitAddController(timeSource: () => DateTime, keyStoreService: KeyStoreMicroService, payeService: PayeMicroService)
  extends BaseController
  with Benefits
  with Validators
  with TaxYearSupport {

  def this() = this(() => DateTimeUtils.now, MicroServices.keyStoreMicroService, MicroServices.payeMicroService)

  def startAddCarBenefit(taxYear: Int, employmentSequenceNumber: Int) =
    AuthorisedForIdaAction(taxRegime = Some(PayeRegime), redirectToOrigin = true) {
      user =>
        request =>
          startAddCarBenefitAction(user, request, taxYear, employmentSequenceNumber)
    }

  def reviewAddCarBenefit(taxYear: Int, employmentSequenceNumber: Int) =
    AuthorisedForIdaAction(taxRegime = Some(PayeRegime)) {
      user => request => reviewAddCarBenefitAction(user, request, taxYear, employmentSequenceNumber)
    }

  private def providedFromDefaultValue = startOfCurrentTaxYear

  private def providedToDefaultValue = endOfCurrentTaxYear

  private def findPrimaryEmployment(payeRootData: PayeRootData): Option[Employment] =
    payeRootData.taxYearEmployments.find(_.employmentType == primaryEmploymentType)

  private def findEmployment(employmentSequenceNumber: Int, payeRootData: PayeRootData) = {
    payeRootData.taxYearEmployments.find(_.sequenceNumber == employmentSequenceNumber)
  }

  private def getCarBenefitDates(request: Request[_]): CarBenefitValues = {
    datesForm(providedFromDefaultValue, providedToDefaultValue).bindFromRequest()(request).value.get
  }

  private def carBenefitForm(carBenefitValues: CarBenefitValues) = Form[CarBenefitData](
    mapping(
      providedFrom -> validateProvidedFrom(timeSource),
      carUnavailable -> optional(boolean).verifying("error.paye.answer_mandatory", data => data.isDefined),
      numberOfDaysUnavailable -> validateNumberOfDaysUnavailable(carBenefitValues),
      giveBackThisTaxYear -> validateGiveBackThisTaxYear(carBenefitValues),
      carRegistrationDate -> validateCarRegistrationDate(carBenefitValues),
      providedTo -> validateProvidedTo(carBenefitValues),
      listPrice -> validateListPrice,
      employeeContributes -> optional(boolean).verifying("error.paye.answer_mandatory", data => data.isDefined),
      employeeContribution -> validateEmployeeContribution(carBenefitValues),
      employerContributes -> optional(boolean).verifying("error.paye.answer_mandatory", data => data.isDefined),
      employerContribution -> validateEmployerContribution(carBenefitValues),
      fuelType -> validateFuelType(carBenefitValues),
      co2Figure -> validateCo2Figure(carBenefitValues),
      co2NoFigure -> validateNoCo2Figure(carBenefitValues),
      engineCapacity -> validateEngineCapacity(carBenefitValues),
      employerPayFuel -> validateEmployerPayFuel(carBenefitValues),
      dateFuelWithdrawn -> validateDateFuelWithdrawn(carBenefitValues)
    )(CarBenefitData.apply)(CarBenefitData.unapply)
  )

  private[paye] val startAddCarBenefitAction: (User, Request[_], Int, Int) => Result = WithValidatedRequest {
    (request, user, taxYear, employmentSequenceNumber, payeRootData) => {
      val dates = getCarBenefitDates(request)
      findEmployment(employmentSequenceNumber, payeRootData) match {
        case Some(employment) => {
          Ok(views.html.paye.add_car_benefit_form(carBenefitForm(dates), employment.employerName, taxYear, employmentSequenceNumber, TaxYearResolver.currentTaxYearYearsRange)(user))
        }
        case None => {
          Logger.debug(s"Unable to find employment for user ${user.oid} with sequence number ${employmentSequenceNumber}")
          BadRequest
        }
      }
    }
  }

  private[paye] val reviewAddCarBenefitAction: (User, Request[_], Int, Int) => Result = WithValidatedRequest {
    (request, user, taxYear, employmentSequenceNumber, payeRootData) => {
      findEmployment(employmentSequenceNumber, payeRootData) match {
        case Some(employment) => {
          val dates = getCarBenefitDates(request)
          val payeRoot = user.regimes.paye.get
          carBenefitForm(dates).bindFromRequest()(request).fold(
            errors => {
              BadRequest(views.html.paye.add_car_benefit_form(errors, employment.employerName, taxYear, employmentSequenceNumber, TaxYearResolver.currentTaxYearYearsRange)(user))
            },
            addCarBenefitData => {

              keyStoreService.addKeyStoreEntry(s"AddCarBenefit:${user.oid}:$taxYear:$employmentSequenceNumber", "paye", "AddCarBenefitForm", addCarBenefitData)

              val emission = if (addCarBenefitData.co2NoFigure.getOrElse(false)) None else addCarBenefitData.co2Figure

              val addBenefitPayload = NewBenefitCalculationData(
                registeredBefore98 = isRegisteredBeforeCutoff(addCarBenefitData.carRegistrationDate),
                fuelType = addCarBenefitData.fuelType.get,
                co2Emission = emission,
                engineCapacity = addCarBenefitData.engineCapacity.map(_.toInt),
                userContributingAmount = addCarBenefitData.employeeContribution,
                listPrice = addCarBenefitData.listPrice.get,
                carBenefitStartDate = addCarBenefitData.providedFrom,
                carBenefitStopDate = addCarBenefitData.providedTo,
                numDaysCarUnavailable = addCarBenefitData.numberOfDaysUnavailable,
                employeePayments = addCarBenefitData.employerContribution,
                employerPayFuel = addCarBenefitData.employerPayFuel.get,
                fuelBenefitStopDate = addCarBenefitData.dateFuelWithdrawn) //TODO check if employerPayFuel can be None

              val uri = payeRoot.actions.getOrElse("calculateBenefitValue", throw new IllegalArgumentException(s"No calculateBenefitValue action uri found"))

              val benefitCalculations = payeService.calculateBenefitValue(uri, addBenefitPayload).get
              val carBenefitValue = benefitCalculations.carBenefitValue.map(BenefitValue(_))
              val carFuelBenefitValue = benefitCalculations.fuelBenefitValue.map(BenefitValue(_))

              val confirmationData = AddCarBenefitConfirmationData(employment.employerName, addCarBenefitData.providedFrom.getOrElse(providedFromDefaultValue),
                addCarBenefitData.listPrice.get, addCarBenefitData.fuelType.get, addCarBenefitData.co2Figure, addCarBenefitData.engineCapacity,
                addCarBenefitData.employerPayFuel, addCarBenefitData.dateFuelWithdrawn, carBenefitValue, carFuelBenefitValue)
              Ok(add_car_benefit_review(confirmationData, TaxYearResolver.currentTaxYearYearsRange)(user))
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

  object WithValidatedRequest {
    def apply(action: (Request[_], User, Int, Int, PayeRootData) => Result): (User, Request[_], Int, Int) => Result = {
      (user, request, taxYear, employmentSequenceNumber) => {
        if (TaxYearResolver.currentTaxYear != taxYear) {
          Logger.error("Adding car benefit is only allowed for the current tax year")
          BadRequest
        } else {
          val payeRootData = user.regimes.paye.get.fetchTaxYearData(currentTaxYear)

          if (employmentSequenceNumber != findPrimaryEmployment(payeRootData).get.sequenceNumber) {
            Logger.error("Adding car benefit is only allowed for the primary employment")
            BadRequest
          } else {
            if (findExistingBenefit(employmentSequenceNumber, BenefitTypes.CAR, payeRootData).isDefined) {
              redirectToCarBenefitHome(request, user)
            } else {
              action(request, user, taxYear, employmentSequenceNumber, payeRootData)
            }
          }
        }
      }
    }

    private val redirectToCarBenefitHome: (Request[_], User) => Result = (r, u) => Redirect(routes.CarBenefitHomeController.carBenefitHome())
  }

}

case class CarBenefitData(providedFrom: Option[LocalDate],
                          carUnavailable: Option[Boolean],
                          numberOfDaysUnavailable: Option[Int],
                          giveBackThisTaxYear: Option[Boolean],
                          carRegistrationDate: Option[LocalDate],
                          providedTo: Option[LocalDate],
                          listPrice: Option[Int],
                          employeeContributes: Option[Boolean],
                          employeeContribution: Option[Int],
                          employerContributes: Option[Boolean],
                          employerContribution: Option[Int],
                          fuelType: Option[String],
                          co2Figure: Option[Int],
                          co2NoFigure: Option[Boolean],
                          engineCapacity: Option[String],
                          employerPayFuel: Option[String],
                          dateFuelWithdrawn: Option[LocalDate])

object CarBenefitFormFields {
  val providedFrom = "providedFrom"
  val carUnavailable = "carUnavailable"
  val numberOfDaysUnavailable = "numberOfDaysUnavailable"
  val giveCarBack = "giveCarBack"
  val giveBackThisTaxYear = "giveBackThisTaxYear"
  val carRegistrationDate = "carRegistrationDate"
  val providedTo = "providedTo"
  val listPrice = "listPrice"
  val employeeContributes = "employeeContributes"
  val employeeContribution = "employeeContribution"
  val employerContributes = "employerContributes"
  val employerContribution = "employerContribution"
  val fuelType = "fuelType"
  val engineCapacity = "engineCapacity"
  val employerPayFuel = "employerPayFuel"
  val dateFuelWithdrawn = "dateFuelWithdrawn"
  val co2Figure = "co2Figure"
  val co2NoFigure = "co2NoFigure"
}
