package controllers.paye

import controllers.common.BaseController
import play.api.mvc.Request
import uk.gov.hmrc.common.microservice.paye.domain._
import models.paye._
import play.api.Logger
import org.joda.time._
import play.api.data.Form
import play.api.data.Forms._
import CarBenefitFormFields._
import controllers.common.validators.Validators
import controllers.paye.validation.AddCarBenefitValidator._
import controllers.common.service.Connectors
import views.html.paye.{add_car_benefit_form, add_car_benefit_review}
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.txqueue.TxQueueConnector
import controllers.common.actions.{HeaderCarrier, Actions}
import controllers.paye.validation.AddBenefitFlow
import models.paye.CarBenefitData
import models.paye.CarBenefitDataAndCalculations
import uk.gov.hmrc.common.microservice.paye.domain.BenefitValue
import play.api.mvc.SimpleResult
import uk.gov.hmrc.common.microservice.domain.User
import models.paye.BenefitUpdatedConfirmationData
import uk.gov.hmrc.common.microservice.paye.domain.TaxYearData
import controllers.paye.validation.AddCarBenefitValidator.CarBenefitValues
import uk.gov.hmrc.common.microservice.paye.domain.AddCarBenefitConfirmationData
import scala.concurrent._

class AddCarBenefitController(keyStoreService: KeyStoreConnector, override val auditConnector: AuditConnector, override val authConnector: AuthConnector)
                             (implicit payeConnector: PayeConnector, txQueueConnector: TxQueueConnector) extends BaseController
with Actions
with Validators
with TaxYearSupport
with PayeRegimeRoots {

  def this() = this(Connectors.keyStoreConnector, Connectors.auditConnector, Connectors.authConnector)(Connectors.payeConnector, Connectors.txQueueConnector)

  private val keyStoreKey = "AddCarBenefitForm"

  def timeSource() = new LocalDate(DateTimeZone.UTC)

  def startAddCarBenefit(taxYear: Int, employmentSequenceNumber: Int) = AuthorisedFor(account = PayeRegime, redirectToOrigin = true).async {
    user =>
      request =>
        startAddCarBenefitAction(user, request, taxYear, employmentSequenceNumber)
  }

  def reviewAddCarBenefit(taxYear: Int, employmentSequenceNumber: Int) = AuthorisedFor(PayeRegime).async {
    user => request => reviewAddCarBenefitAction(user, request, taxYear, employmentSequenceNumber)
  }

  def confirmAddingBenefit(taxYear: Int, employmentSequenceNumber: Int) = AuthorisedFor(PayeRegime).async {
    user => request => confirmAddingBenefitAction(user, request, taxYear, employmentSequenceNumber)
  }

  private def findEmployment(employmentSequenceNumber: Int, payeRootData: TaxYearData) = {
    payeRootData.employments.find(_.sequenceNumber == employmentSequenceNumber)
  }

  private def getCarBenefitDates(request: Request[_]): CarBenefitValues = {
    validationlessForm.bindFromRequest()(request).value.get
  }

  private def carBenefitForm(carBenefitValues: CarBenefitValues) = Form[CarBenefitData](
    mapping(
      providedFrom -> validateProvidedFrom(timeSource, taxYearInterval),
      carRegistrationDate -> validateCarRegistrationDate(timeSource),
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
      dateFuelWithdrawn -> validateDateFuelWithdrawn(carBenefitValues, taxYearInterval)
    )(CarBenefitData.apply)(CarBenefitData.unapply)
  )

  private[paye] val startAddCarBenefitAction: (User, Request[_], Int, Int) => Future[SimpleResult] = AddBenefitFlow(BenefitTypes.CAR) {
    (user, request, taxYear, employmentSequenceNumber, payeRootData) =>
      findEmployment(employmentSequenceNumber, payeRootData) match {
        case Some(employment) => {
          implicit val hc = HeaderCarrier(request)
          lookupValuesFromKeystoreAndBuildForm(generateKeystoreActionId(taxYear, employmentSequenceNumber)).map {
            benefitFormWithSavedValues =>
              Ok(views.html.paye.add_car_benefit_form(benefitFormWithSavedValues, employment.employerName, taxYear, employmentSequenceNumber, currentTaxYearYearsRange)(user))
          }
        }
        case None => {
          Logger.debug(s"Unable to find employment for user ${user.oid} with sequence number $employmentSequenceNumber")
          Future.successful(BadRequest)
        }
      }
  }

  private def lookupValuesFromKeystoreAndBuildForm(keyStoreId: String)(implicit hc: HeaderCarrier): Future[Form[CarBenefitData]] = {
    savedValuesFromKeyStore(keyStoreId).map {
      case Some(savedValuesAndCalculation) => {
        val savedValues = savedValuesAndCalculation.carBenefitData
        val rawForm = validationlessForm
        val valuesForValidation = rawForm.fill(rawValuesOf(savedValues)).value.get
        carBenefitForm(valuesForValidation).fill(savedValues)
      }
      case None => carBenefitForm(CarBenefitValues())
    }
  }

  private def savedValuesFromKeyStore(keyStoreId: String)(implicit hc: HeaderCarrier) = keyStoreService.getEntry[CarBenefitDataAndCalculations](keyStoreId, KeystoreUtils.source, keyStoreKey)

  private[paye] def rawValuesOf(defaults: CarBenefitData) =
    CarBenefitValues(providedFromVal = defaults.providedFrom,
      carRegistrationDate = defaults.carRegistrationDate,
      employeeContributes = defaults.employeeContributes.map(_.toString),
      employerContributes = defaults.employerContributes.map(_.toString),
      fuelType = defaults.fuelType,
      co2Figure = defaults.co2Figure.map(_.toString),
      co2NoFigure = defaults.co2NoFigure.map(_.toString),
      employerPayFuel = defaults.employerPayFuel)

  private[paye] val confirmAddingBenefitAction: (User, Request[_], Int, Int) => Future[SimpleResult] = AddBenefitFlow(BenefitTypes.CAR) {
    (user, request, taxYear, employmentSequenceNumber, payeRootData) => {

      implicit val hc = HeaderCarrier(request)

      val payeRoot = user.getPaye
      savedValuesFromKeyStore(generateKeystoreActionId(taxYear, employmentSequenceNumber)).flatMap {
        savedData =>
          val carBenefitDataAndCalculation = savedData.getOrElse(throw new IllegalStateException(s"No value was returned from the keystore for AddCarBenefit:${user.oid}:$taxYear:$employmentSequenceNumber"))

          val payeAddBenefitUri = payeRoot.addBenefitLink(taxYear).getOrElse(throw new IllegalStateException(s"No link was available for adding a benefit for user with oid ${user.oid}"))
          val carAndFuel = CarAndFuelBuilder(carBenefitDataAndCalculation, taxYear, employmentSequenceNumber)
          val addBenefitsResponse = payeConnector.addBenefits(payeAddBenefitUri, payeRoot.version, employmentSequenceNumber, Seq(carAndFuel.carBenefit) ++ carAndFuel.fuelBenefit)
          keyStoreService.deleteKeyStore(generateKeystoreActionId(taxYear, employmentSequenceNumber), KeystoreUtils.source)

          TaxCodeResolver.currentTaxCode(payeRoot, employmentSequenceNumber, taxYear).flatMap {
            currentTaxYearCode =>
              val f1 = addBenefitsResponse.map(_.get.newTaxCode)
              val f2 = addBenefitsResponse.map(_.get.netCodedAllowance)

              for {
                newTaxCode <- f1
                netCodedAllowance <- f2
              } yield {
                val benefitUpdateConfirmationData = BenefitUpdatedConfirmationData(currentTaxYearCode, newTaxCode, netCodedAllowance, startOfCurrentTaxYear, endOfCurrentTaxYear)
                Ok(views.html.paye.add_car_benefit_confirmation(benefitUpdateConfirmationData))
              }
          }
      }
    }
  }

  private[paye] val reviewAddCarBenefitAction: (User, Request[_], Int, Int) => Future[SimpleResult] = AddBenefitFlow(BenefitTypes.CAR) {
    (user, request, taxYear, employmentSequenceNumber, payeRootData) =>
      findEmployment(employmentSequenceNumber, payeRootData) match {
        case Some(employment) => {
          val dates = getCarBenefitDates(request)
          val payeRoot = user.regimes.paye.get

          carBenefitForm(dates).bindFromRequest()(request).fold(
            errors => Future.successful(BadRequest(add_car_benefit_form(errors, employment.employerName, taxYear, employmentSequenceNumber, currentTaxYearYearsRange)(user))),
            (addCarBenefitData: CarBenefitData) => {
              implicit val hc = HeaderCarrier(request)
              val emission = if (addCarBenefitData.co2NoFigure.getOrElse(false)) None else addCarBenefitData.co2Figure
              val carAndFuelBenefit = CarAndFuelBuilder(CarBenefitDataAndCalculations(addCarBenefitData.copy(co2Figure = emission), 0, Some(0), Some(0), Some(0)), taxYear, employmentSequenceNumber)
              val uri = payeRoot.actions.getOrElse("calculateBenefitValue", throw new IllegalArgumentException(s"No calculateBenefitValue action uri found"))

              payeConnector.calculateBenefitValue(uri, carAndFuelBenefit).map(_.get).flatMap {
                benefitCalculations =>
                  val carBenefitValue: Option[BenefitValue] = benefitCalculations.carBenefitValue.map(BenefitValue)
                  val fuelBenefitValue: Option[BenefitValue] = benefitCalculations.fuelBenefitValue.map(BenefitValue)
                  println(benefitCalculations.carBenefitForecastValue.get)
                  println(benefitCalculations.fuelBenefitForecastValue.get)
                  keyStoreService.addKeyStoreEntry(generateKeystoreActionId(taxYear, employmentSequenceNumber), KeystoreUtils.source, keyStoreKey, CarBenefitDataAndCalculations(addCarBenefitData, carBenefitValue.get.taxableValue, fuelBenefitValue.map(_.taxableValue), benefitCalculations.carBenefitForecastValue, benefitCalculations.fuelBenefitForecastValue)).map {
                  _=>
                    val confirmationData = AddCarBenefitConfirmationData(employment.employerName, addCarBenefitData.providedFrom.getOrElse(startOfCurrentTaxYear),
                    addCarBenefitData.listPrice.get, addCarBenefitData.fuelType.get, addCarBenefitData.co2Figure, addCarBenefitData.engineCapacity,
                    addCarBenefitData.employerPayFuel, addCarBenefitData.dateFuelWithdrawn, carBenefitValue, fuelBenefitValue)
                    Ok(add_car_benefit_review(confirmationData, currentTaxYearYearsRange, user, request.uri, taxYear, employmentSequenceNumber))
                  }
              }
            }
          )
        }
        case None => {
          Logger.debug(s"Unable to find employment for user ${user.oid} with sequence number $employmentSequenceNumber")
          Future.successful(BadRequest)
        }
      }
  }

  private def generateKeystoreActionId(taxYear: Int, employmentSequenceNumber: Int) = {
    s"AddCarBenefit:$taxYear:$employmentSequenceNumber"
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
