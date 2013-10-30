package controllers.paye

import controllers.common.{Ida, Actions, BaseController2}
import play.api.mvc.{SimpleResult, Request}
import uk.gov.hmrc.common.microservice.paye.domain._
import uk.gov.hmrc.common.microservice.paye.domain.Employment._
import models.paye.BenefitTypes
import play.api.Logger
import org.joda.time._
import play.api.data.Form
import play.api.data.Forms._
import CarBenefitFormFields._
import controllers.common.validators.Validators
import controllers.paye.validation.AddCarBenefitValidator._
import controllers.common.service.MicroServices
import views.html.paye.add_car_benefit_review
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.paye.domain.AddCarBenefitConfirmationData
import controllers.paye.validation.EngineCapacity
import uk.gov.hmrc.common.microservice.keystore.KeyStoreMicroService
import uk.gov.hmrc.common.microservice.audit.AuditMicroService
import uk.gov.hmrc.common.microservice.paye.PayeMicroService
import uk.gov.hmrc.microservice.txqueue.TxQueueMicroService
import uk.gov.hmrc.common.microservice.auth.AuthMicroService

class CarBenefitAddController(keyStoreService: KeyStoreMicroService, override val auditMicroService: AuditMicroService, override val authMicroService: AuthMicroService)
                             (implicit payeMicroService: PayeMicroService, txQueueMicroService: TxQueueMicroService) extends BaseController2
  with Actions
  with Benefits
  with Validators
  with TaxYearSupport {

  def this() = this(MicroServices.keyStoreMicroService, MicroServices.auditMicroService, MicroServices.authMicroService)(MicroServices.payeMicroService, MicroServices.txQueueMicroService)

  def timeSource() = new LocalDate(DateTimeZone.UTC)

  def startAddCarBenefit(taxYear: Int, employmentSequenceNumber: Int) =
    ActionAuthorisedBy(Ida)(taxRegime = Some(PayeRegime), redirectToOrigin = true) {
      user =>
        request =>
          startAddCarBenefitAction(user, request, taxYear, employmentSequenceNumber)
    }

  def reviewAddCarBenefit(taxYear: Int, employmentSequenceNumber: Int) =
    ActionAuthorisedBy(Ida)(taxRegime = Some(PayeRegime)) {
      user => request => reviewAddCarBenefitAction(user, request, taxYear, employmentSequenceNumber)
    }

  def confirmAddingBenefit(taxYear: Int, employmentSequenceNumber: Int) =
    ActionAuthorisedBy(Ida)(taxRegime = Some(PayeRegime)) {
      user => request => confirmAddingBenefitAction(user, request, taxYear, employmentSequenceNumber)
    }

  private def findPrimaryEmployment(payeRootData: PayeRootData): Option[Employment] =
    payeRootData.taxYearEmployments.find(_.employmentType == primaryEmploymentType)

  private def findEmployment(employmentSequenceNumber: Int, payeRootData: PayeRootData) = {
    payeRootData.taxYearEmployments.find(_.sequenceNumber == employmentSequenceNumber)
  }

  private def getCarBenefitDates(request: Request[_]): CarBenefitValues = {
    validationlessForm.bindFromRequest()(request).value.get
  }

  private def carBenefitForm(carBenefitValues: CarBenefitValues) = Form[CarBenefitData](
    mapping(
      providedFrom -> validateProvidedFrom(timeSource),
      carUnavailable -> optional(boolean).verifying("error.paye.answer_mandatory", data => data.isDefined),
      numberOfDaysUnavailable -> validateNumberOfDaysUnavailable(carBenefitValues),
      giveBackThisTaxYear -> validateGiveBackThisTaxYear(),
      carRegistrationDate -> validateCarRegistrationDate(timeSource),
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

  private[paye] val startAddCarBenefitAction: (User, Request[_], Int, Int) => SimpleResult = WithValidatedRequest {
    (request, user, taxYear, employmentSequenceNumber, payeRootData) => {
      findEmployment(employmentSequenceNumber, payeRootData) match {
        case Some(employment) => {
          val benefitFormWithSavedValues = lookupValuesFromKeystoreAndBuildForm(s"AddCarBenefit:${user.oid}:$taxYear:$employmentSequenceNumber")
          Ok(views.html.paye.add_car_benefit_form(benefitFormWithSavedValues, employment.employerName, taxYear, employmentSequenceNumber, currentTaxYearYearsRange)(user))
        }
        case None => {
          Logger.debug(s"Unable to find employment for user ${user.oid} with sequence number ${employmentSequenceNumber}")
          BadRequest
        }
      }
    }
  }

  private def lookupValuesFromKeystoreAndBuildForm(keyStoreId:String) = {
    savedValuesFromKeyStore(keyStoreId) match {
      case Some(savedValues) => {
        val rawForm = validationlessForm
        val valuesForValidation = rawForm.fill(rawValuesOf(savedValues)).value.get
        carBenefitForm(valuesForValidation).fill(savedValues)
      }
      case None => carBenefitForm(CarBenefitValues())
    }
  }

  private def savedValuesFromKeyStore(keyStoreId:String) = keyStoreService.getEntry[CarBenefitData](keyStoreId, "paye", "AddCarBenefitForm")

  private[paye] def rawValuesOf(defaults: CarBenefitData) =
    CarBenefitValues(providedFromVal = defaults.providedFrom,
      carUnavailableVal = defaults.carUnavailable.map(_.toString),
      numberOfDaysUnavailableVal = defaults.numberOfDaysUnavailable.map(_.toString),
      giveBackThisTaxYearVal = defaults.giveBackThisTaxYear.map(_.toString),
      providedToVal = defaults.providedTo,
      carRegistrationDate = defaults.carRegistrationDate,
      employeeContributes = defaults.employeeContributes.map(_.toString),
      employerContributes = defaults.employeeContributes.map(_.toString),
      fuelType = defaults.fuelType,
      co2Figure = defaults.co2Figure.map(_.toString),
      co2NoFigure = defaults.co2NoFigure.map(_.toString),
      employerPayFuel = defaults.employerPayFuel)

  private[paye] val confirmAddingBenefitAction: (User, Request[_], Int, Int) => SimpleResult = WithValidatedRequest {
    (request, user, taxYear, employmentSequenceNumber, payeRootData) => {

      keyStoreService.deleteKeyStore(s"AddCarBenefit:${user.oid}:$taxYear:$employmentSequenceNumber", "paye")

      Ok(views.html.paye.add_car_benefit_confirmation())
    }
  }

  private[paye] val reviewAddCarBenefitAction: (User, Request[_], Int, Int) => SimpleResult = WithValidatedRequest {
    (request, user, taxYear, employmentSequenceNumber, payeRootData) => {
      findEmployment(employmentSequenceNumber, payeRootData) match {
        case Some(employment) => {
          val dates = getCarBenefitDates(request)
          val payeRoot = user.regimes.paye.get
          carBenefitForm(dates).bindFromRequest()(request).fold(
            errors => {
              BadRequest(views.html.paye.add_car_benefit_form(errors, employment.employerName, taxYear, employmentSequenceNumber, currentTaxYearYearsRange)(user))
            },
            addCarBenefitData => {

              keyStoreService.addKeyStoreEntry(s"AddCarBenefit:${user.oid}:$taxYear:$employmentSequenceNumber", "paye", "AddCarBenefitForm", addCarBenefitData)

              val emission = if (addCarBenefitData.co2NoFigure.getOrElse(false)) None else addCarBenefitData.co2Figure

              val addBenefitPayload = NewBenefitCalculationData(
                carRegisteredBefore98 = isRegisteredBeforeCutoff(addCarBenefitData.carRegistrationDate),
                fuelType = addCarBenefitData.fuelType.get,
                co2Emission = emission,
                engineCapacity = EngineCapacity.mapEngineCapacityToInt(addCarBenefitData.engineCapacity),
                userContributingAmount = addCarBenefitData.employeeContribution,
                listPrice = addCarBenefitData.listPrice.get,
                carBenefitStartDate = addCarBenefitData.providedFrom,
                carBenefitStopDate = addCarBenefitData.providedTo,
                numDaysCarUnavailable = addCarBenefitData.numberOfDaysUnavailable,
                employeePayments = addCarBenefitData.employerContribution,
                employerPayFuel = addCarBenefitData.employerPayFuel.get,
                fuelBenefitStopDate = addCarBenefitData.dateFuelWithdrawn) //TODO check if employerPayFuel can be None

              val uri = payeRoot.actions.getOrElse("calculateBenefitValue", throw new IllegalArgumentException(s"No calculateBenefitValue action uri found"))

              val benefitCalculations = payeMicroService.calculateBenefitValue(uri, addBenefitPayload).get
              val carBenefitValue = benefitCalculations.carBenefitValue.map(BenefitValue(_))
              val carFuelBenefitValue = benefitCalculations.fuelBenefitValue.map(BenefitValue(_))

              val confirmationData = AddCarBenefitConfirmationData(employment.employerName, addCarBenefitData.providedFrom.getOrElse(startOfCurrentTaxYear),
                addCarBenefitData.listPrice.get, addCarBenefitData.fuelType.get, addCarBenefitData.co2Figure, addCarBenefitData.engineCapacity,
                addCarBenefitData.employerPayFuel, addCarBenefitData.dateFuelWithdrawn, carBenefitValue, carFuelBenefitValue)
              Ok(add_car_benefit_review(confirmationData, currentTaxYearYearsRange, user, request.uri, taxYear, employmentSequenceNumber))
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
    def apply(action: (Request[_], User, Int, Int, PayeRootData) => SimpleResult): (User, Request[_], Int, Int) => SimpleResult = {
      (user, request, taxYear, employmentSequenceNumber) => {
        if (currentTaxYear != taxYear) {
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

    private val redirectToCarBenefitHome: (Request[_], User) => SimpleResult = (r, u) => Redirect(routes.CarBenefitHomeController.carBenefitHome.url)
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
