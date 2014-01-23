package controllers.paye

import controllers.common.BaseController
import play.api.mvc.Request
import uk.gov.hmrc.common.microservice.paye.domain._
import models.paye._
import play.api.Logger
import play.api.data.Form
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
import play.api.mvc.SimpleResult
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.paye.domain.TaxYearData
import controllers.paye.validation.AddCarBenefitValidator.CarBenefitValues
import uk.gov.hmrc.common.microservice.paye.domain.AddCarBenefitConfirmationData
import scala.concurrent._
import org.joda.time.LocalDate
import views.formatting.Strings

class AddCarBenefitController(keyStoreService: KeyStoreConnector, override val auditConnector: AuditConnector, override val authConnector: AuthConnector)
                             (implicit payeConnector: PayeConnector, txQueueConnector: TxQueueConnector) extends BaseController
with Actions
with Validators
with PayeRegimeRoots {

  def this() = this(Connectors.keyStoreConnector, Connectors.auditConnector, Connectors.authConnector)(Connectors.payeConnector, Connectors.txQueueConnector)

  def timeSource() = new LocalDate

  private val keyStoreKey = "AddCarBenefitForm"

  def startAddCarBenefit(taxYear: Int, employmentSequenceNumber: Int) = AuthorisedFor(regime = PayeRegime, redirectToOrigin = true).async {
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

  private[paye] val startAddCarBenefitAction: (User, Request[_], Int, Int) => Future[SimpleResult] = AddBenefitFlow(BenefitTypes.CAR) {
    (user, request, taxYear, employmentSequenceNumber, payeRootData) =>
      findEmployment(employmentSequenceNumber, payeRootData) match {
        case Some(employment) => {
          implicit val hc = HeaderCarrier(request)
          lookupValuesFromKeystoreAndBuildForm(generateKeystoreActionId(taxYear, employmentSequenceNumber)).map {
            benefitFormWithSavedValues =>
              Ok(views.html.paye.add_car_benefit_form(benefitFormWithSavedValues, employment.employerName, taxYear, employmentSequenceNumber, currentTaxYearYearsRange)(user, request))
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
      case Some(carBenefitData) => {
        val rawForm = validationlessForm
        val valuesForValidation = rawForm.fill(rawValuesOf(carBenefitData)).value.get
        carBenefitForm(valuesForValidation, timeSource).fill(carBenefitData)
      }
      case None => carBenefitForm(CarBenefitValues(), timeSource)
    }
  }

  private def savedValuesFromKeyStore(keyStoreId: String)(implicit hc: HeaderCarrier) = keyStoreService.getEntry[CarBenefitData](keyStoreId, KeystoreUtils.source, keyStoreKey)

  private[paye] def rawValuesOf(defaults: CarBenefitData) =
    CarBenefitValues(providedFromVal = defaults.providedFrom,
      carRegistrationDate = defaults.carRegistrationDate,
      employeeContributes = defaults.employeeContributes.map(_.toString),
      privateUsePayment = defaults.privateUsePayment.map(_.toString),
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
          payeRoot.version.flatMap {
            version =>
              val carBenefitDataAndCalculation = savedData.getOrElse(throw new IllegalStateException(s"No value was returned from the keystore for AddCarBenefit:${user.oid}:$taxYear:$employmentSequenceNumber"))

              val payeAddBenefitUri = payeRoot.addBenefitLink(taxYear).getOrElse(throw new IllegalStateException(s"No link was available for adding a benefit for user with oid ${user.oid}"))
              val carBenefit = CarBenefitBuilder(carBenefitDataAndCalculation, taxYear, employmentSequenceNumber)
              keyStoreService.deleteKeyStore(generateKeystoreActionId(taxYear, employmentSequenceNumber), KeystoreUtils.source)

              for {
                currentTaxYearCode <- TaxCodeResolver.currentTaxCode(payeRoot, employmentSequenceNumber, taxYear)
                addBenefitsResponseOption <- payeConnector.addBenefits(payeAddBenefitUri, version, employmentSequenceNumber, carBenefit.toBenefits)
              }
              yield {
                val addBenefitsResponse = addBenefitsResponseOption.getOrElse(throw new IllegalStateException("No add benefits response was returned from the addBenefits call to paye"))
                val benefitUpdateConfirmationData = BenefitUpdateConfirmationBuilder.buildBenefitUpdatedConfirmationData(currentTaxYearCode, addBenefitsResponse)
                Ok(views.html.paye.add_car_benefit_confirmation(benefitUpdateConfirmationData, AddCar)(request))
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

          carBenefitForm(dates, timeSource).bindFromRequest()(request).fold(
            errors => Future.successful(BadRequest(add_car_benefit_form(errors, employment.employerName, taxYear, employmentSequenceNumber, currentTaxYearYearsRange)(user, request))),
            (addCarBenefitData: CarBenefitData) => {
              implicit val hc = HeaderCarrier(request)

              keyStoreService.addKeyStoreEntry(generateKeystoreActionId(taxYear, employmentSequenceNumber), KeystoreUtils.source, keyStoreKey, addCarBenefitData).map {
                _ => {
                  import AddCarBenefitConfirmationData._

                  val confirmationData =
                    AddCarBenefitConfirmationData(
                      Strings.optionalValue(employment.employerName, "your.employer"),
                      addCarBenefitData.providedFrom.getOrElse(startOfCurrentTaxYear),
                      addCarBenefitData.listPrice.get,
                      addCarBenefitData.fuelType.get,
                      addCarBenefitData.co2Figure,
                      addCarBenefitData.engineCapacity,
                      convertEmployerPayFuel(addCarBenefitData.fuelType, addCarBenefitData.employerPayFuel),
                      addCarBenefitData.employeeContribution,
                      addCarBenefitData.carRegistrationDate,
                      addCarBenefitData.privateUsePaymentAmount)

                  Ok(add_car_benefit_review(confirmationData, user, taxYear, employmentSequenceNumber)(request))
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
  val agreement = "agreement"
  val carUnavailable = "carUnavailable"
  val removeEmployeeContributes = "removeEmployeeContributes"
  val removeEmployeeContribution = "removeEmployeeContribution"
  val fuelRadio = "fuelRadio"
  val withdrawDate = "withdrawDate"
  val providedFrom = "providedFrom"
  val numberOfDaysUnavailable = "numberOfDaysUnavailable"
  val giveCarBack = "giveCarBack"
  val giveBackThisTaxYear = "giveBackThisTaxYear"
  val carRegistrationDate = "carRegistrationDate"
  val providedTo = "providedTo"
  val listPrice = "listPrice"
  val employeeContributes = "employeeContributes"
  val employeeContribution = "employeeContribution"
  val privateUsePayment = "privateUsePayment"
  val privateUsePaymentAmount = "privateUsePaymentAmount"
  val fuelType = "fuelType"
  val engineCapacity = "engineCapacity"
  val employerPayFuel = "employerPayFuel"
  val dateFuelWithdrawn = "dateFuelWithdrawn"
  val co2Figure = "co2Figure"
  val co2NoFigure = "co2NoFigure"
}
