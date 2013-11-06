package controllers.paye

import controllers.common.{Ida, Actions, BaseController2}
import play.api.mvc.Request
import uk.gov.hmrc.common.microservice.paye.domain._
import uk.gov.hmrc.common.microservice.paye.domain.Employment._
import models.paye.{TaxCodeResolver, BenefitUpdatedConfirmationData, BenefitTypes}
import play.api.Logger
import org.joda.time._
import play.api.data.Form
import play.api.data.Forms._
import CarBenefitFormFields._
import controllers.common.validators.Validators
import controllers.paye.validation.AddCarBenefitValidator._
import controllers.common.service.Connectors
import views.html.paye.add_car_benefit_review
import controllers.paye.validation.EngineCapacity
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.paye.domain.NewBenefitCalculationData
import uk.gov.hmrc.common.microservice.paye.domain.BenefitValue
import play.api.mvc.SimpleResult
import uk.gov.hmrc.common.microservice.paye.domain.TaxYearData
import uk.gov.hmrc.common.microservice.domain.User
import controllers.paye.validation.AddCarBenefitValidator.CarBenefitValues
import uk.gov.hmrc.common.microservice.paye.domain.AddCarBenefitConfirmationData
import uk.gov.hmrc.common.microservice.txqueue.TxQueueConnector

class CarBenefitAddController(keyStoreService: KeyStoreConnector, override val auditConnector: AuditConnector, override val authConnector: AuthConnector)
                             (implicit payeConnector: PayeConnector, txQueueConnector: TxQueueConnector) extends BaseController2
with Actions
with Validators
with TaxYearSupport {

  def this() = this(Connectors.keyStoreConnector, Connectors.auditConnector, Connectors.authConnector)(Connectors.payeConnector, Connectors.txQueueConnector)

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

  private def findPrimaryEmployment(payeRootData: TaxYearData): Option[Employment] =
    payeRootData.employments.find(_.employmentType == primaryEmploymentType)

  private def findEmployment(employmentSequenceNumber: Int, payeRootData: TaxYearData) = {
    payeRootData.employments.find(_.sequenceNumber == employmentSequenceNumber)
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
          Logger.debug(s"Unable to find employment for user ${user.oid} with sequence number $employmentSequenceNumber")
          BadRequest
        }
      }
    }
  }

  private def lookupValuesFromKeystoreAndBuildForm(keyStoreId: String) = {
    savedValuesFromKeyStore(keyStoreId) match {
      case Some(savedValuesAndCalculation) => {
        val savedValues = savedValuesAndCalculation.carBenefitData
        val rawForm = validationlessForm
        val valuesForValidation = rawForm.fill(rawValuesOf(savedValues)).value.get
        carBenefitForm(valuesForValidation).fill(savedValues)
      }
      case None => carBenefitForm(CarBenefitValues())
    }
  }

  private def savedValuesFromKeyStore(keyStoreId: String) = keyStoreService.getEntry[CarBenefitDataAndCalculations](keyStoreId, "paye", "AddCarBenefitForm")

  private[paye] def rawValuesOf(defaults: CarBenefitData) =
    CarBenefitValues(providedFromVal = defaults.providedFrom,
      carUnavailableVal = defaults.carUnavailable.map(_.toString),
      numberOfDaysUnavailableVal = defaults.numberOfDaysUnavailable.map(_.toString),
      giveBackThisTaxYearVal = defaults.giveBackThisTaxYear.map(_.toString),
      providedToVal = defaults.providedTo,
      carRegistrationDate = defaults.carRegistrationDate,
      employeeContributes = defaults.employeeContributes.map(_.toString),
      employerContributes = defaults.employerContributes.map(_.toString),
      fuelType = defaults.fuelType,
      co2Figure = defaults.co2Figure.map(_.toString),
      co2NoFigure = defaults.co2NoFigure.map(_.toString),
      employerPayFuel = defaults.employerPayFuel)

  private[paye] val confirmAddingBenefitAction: (User, Request[_], Int, Int) => SimpleResult = WithValidatedRequest {
    (request, user, taxYear, employmentSequenceNumber, payeRootData) => {

      val payeRoot = user.getPaye
      val carBenefitDataAndCalculation = savedValuesFromKeyStore(s"AddCarBenefit:${user.oid}:$taxYear:$employmentSequenceNumber").getOrElse(throw new IllegalStateException(s"No value was returned from the keystore for AddCarBenefit:${user.oid}:$taxYear:$employmentSequenceNumber"))

      val payeAddBenefitUri = payeRoot.addBenefitLink(taxYear).getOrElse(throw new IllegalStateException(s"No link was available for adding a benefit for user with oid ${user.oid}"))
      val addBenefitsResponse = payeConnector.addBenefits(payeAddBenefitUri, payeRoot.version, employmentSequenceNumber, CarBenefits(carBenefitDataAndCalculation, taxYear, employmentSequenceNumber))
      keyStoreService.deleteKeyStore(s"AddCarBenefit:${user.oid}:$taxYear:$employmentSequenceNumber", "paye")
      Ok(views.html.paye.add_car_benefit_confirmation(BenefitUpdatedConfirmationData(
        TaxCodeResolver.currentTaxCode(user.regimes.paye.get, employmentSequenceNumber, taxYear), addBenefitsResponse.get.calculatedTaxCode, addBenefitsResponse.get.personalAllowance, "start date", "end date")))
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
            (addCarBenefitData: CarBenefitData) => {


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
              val benefitCalculations = payeConnector.calculateBenefitValue(uri, addBenefitPayload).get
              val carBenefitValue : Option[BenefitValue]= benefitCalculations.carBenefitValue.map(BenefitValue)
              val fuelBenefitValue: Option[BenefitValue] = benefitCalculations.fuelBenefitValue.map(BenefitValue)

              keyStoreService.addKeyStoreEntry(s"AddCarBenefit:${user.oid}:$taxYear:$employmentSequenceNumber", "paye", "AddCarBenefitForm", CarBenefitDataAndCalculations(addCarBenefitData, carBenefitValue.get.taxableValue , fuelBenefitValue.map(_.taxableValue)))
              val confirmationData = AddCarBenefitConfirmationData(employment.employerName, addCarBenefitData.providedFrom.getOrElse(startOfCurrentTaxYear),
                addCarBenefitData.listPrice.get, addCarBenefitData.fuelType.get, addCarBenefitData.co2Figure, addCarBenefitData.engineCapacity,
                addCarBenefitData.employerPayFuel, addCarBenefitData.dateFuelWithdrawn, carBenefitValue, fuelBenefitValue)
              Ok(add_car_benefit_review(confirmationData, currentTaxYearYearsRange, user, request.uri, taxYear, employmentSequenceNumber))
            }
          )
        }
        case None => {
          Logger.debug(s"Unable to find employment for user ${user.oid} with sequence number $employmentSequenceNumber")
          BadRequest
        }
      }
    }

  }

  object WithValidatedRequest {
    def apply(action: (Request[_], User, Int, Int, TaxYearData) => SimpleResult): (User, Request[_], Int, Int) => SimpleResult = {
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
            if (payeRootData.findExistingBenefit(employmentSequenceNumber, BenefitTypes.CAR).isDefined) {
              redirectToCarBenefitHome(request, user)
            } else {
              action(request, user, taxYear, employmentSequenceNumber, payeRootData)
            }
          }
        }
      }
    }

    private val redirectToCarBenefitHome: (Request[_], User) => SimpleResult = (r, u) => Redirect(routes.CarBenefitHomeController.carBenefitHome().url)
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

case class CarBenefitDataAndCalculations(carBenefitData : CarBenefitData, carBenefitValue: Int, fuelBenefitValue: Option[Int])

//TODO this code can be removed from here once it has been copied to the PAYE service
object CarBenefits {
  def apply(carBenefitDataAndCalculations: CarBenefitDataAndCalculations, taxYear: Int, employmentSequenceNumber: Int): Seq[Benefit] = {
  val carBenefitData = carBenefitDataAndCalculations.carBenefitData
    val car = createCar(carBenefitData)

    val carBenefit = createBenefit(31, carBenefitData.providedTo, taxYear, employmentSequenceNumber, Some(car), carBenefitDataAndCalculations.carBenefitValue)

    val fuelBenefit = carBenefitData.employerPayFuel match {
        //benefitType: Int, withdrawnDate: Option[LocalDate], taxYear: Int, employmentSeqNumber: Int, car: Option[Car], grossBenefitAmount : Int
      case Some(data) if data == "true" || data == "again" => Some(createBenefit(benefitType = 29, withdrawnDate = carBenefitData.providedTo, taxYear = taxYear,
        employmentSeqNumber =  employmentSequenceNumber, car = Some(car), grossBenefitAmount = carBenefitDataAndCalculations.fuelBenefitValue.get))
      case Some("date") => Some(createBenefit(benefitType = 29, withdrawnDate = carBenefitData.dateFuelWithdrawn, taxYear = taxYear,
        employmentSeqNumber =  employmentSequenceNumber, car = Some(car), grossBenefitAmount = carBenefitDataAndCalculations.fuelBenefitValue.get))
      case _ => None
    }
    Seq(Some(carBenefit), fuelBenefit).flatten
  }


  private def createCar(carBenefitData: CarBenefitData) = {
    Car(dateCarMadeAvailable = carBenefitData.providedFrom,
      dateCarWithdrawn = carBenefitData.providedTo,
      dateCarRegistered = carBenefitData.carRegistrationDate,
      employeeCapitalContribution = carBenefitData.employeeContribution.map(BigDecimal(_)),
      fuelType = carBenefitData.fuelType,
      co2Emissions = carBenefitData.co2Figure,
      engineSize = engineSize(carBenefitData.engineCapacity),
      mileageBand = None,
      carValue = carBenefitData.listPrice.map(BigDecimal(_)),
      employeePayments = carBenefitData.employerContribution.map(BigDecimal(_)),
      daysUnavailable = carBenefitData.numberOfDaysUnavailable)
  }

  private def engineSize(engineCapacity: Option[String]) : Option[Int] = {
    engineCapacity match {
      // TODO: Investigate why keystore is returning a string value 'none' instead of an Option None value
      case Some(engine) if(engine != EngineCapacity.NOT_APPLICABLE) => Some(engine.toInt)
      case _ => None
    }
  }

  private def createBenefit(benefitType: Int, withdrawnDate: Option[LocalDate], taxYear: Int, employmentSeqNumber: Int, car: Option[Car], grossBenefitAmount : Int) = {
    Benefit(benefitType = benefitType,
      taxYear = taxYear,
      grossAmount = grossBenefitAmount,
      employmentSequenceNumber = employmentSeqNumber,
      costAmount = None,
      amountMadeGood = None,
      cashEquivalent = None,
      expensesIncurred = None,
      amountOfRelief = None,
      paymentOrBenefitDescription = None,
      dateWithdrawn = withdrawnDate,
      car = car,
      actions = Map.empty[String, String],
      calculations = Map.empty[String, String])
  }
}

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
