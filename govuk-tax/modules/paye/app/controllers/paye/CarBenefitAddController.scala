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
import uk.gov.hmrc.common.microservice.domain.User
import controllers.paye.validation.AddCarBenefitValidator.CarBenefitValues
import controllers.common.service.MicroServices
import uk.gov.hmrc.common.microservice.paye.PayeMicroService
import views.html.paye.{add_car_benefit_review, remove_benefit_confirm}
import controllers.paye.CarBenefitData
import scala.Some
import uk.gov.hmrc.common.microservice.domain.User
import controllers.paye.validation.AddCarBenefitValidator.CarBenefitValues
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

  private def findPrimaryEmployment(user: User) : Option[Employment] =
    user.regimes.paye.get.get.employments(currentTaxYear).find(_.employmentType == primaryEmploymentType)

  private def findEmployment(user: User, taxYear: Int, employmentSequenceNumber: Int) = {
    user.regimes.paye.get.get.employments(taxYear).find(_.sequenceNumber == employmentSequenceNumber)
  }

  private def getCarBenefitDates(request:Request[_]):CarBenefitValues = {
    datesForm(providedFromDefaultValue, providedToDefaultValue).bindFromRequest()(request).value.get
  }

  private def carBenefitForm(carBenefitValues: CarBenefitValues) = Form[CarBenefitData](
    mapping(
      providedFrom -> validateProvidedFrom(timeSource),
      carUnavailable -> optional(boolean).verifying("error.paye.answer_mandatory", data => data.isDefined),
      numberOfDaysUnavailable -> validateNumberOfDaysUnavailable(carBenefitValues),
      giveBackThisTaxYear -> validateGiveBackThisTaxYear(carBenefitValues),
      registeredBefore98 -> validateRegisteredBefore98(carBenefitValues),
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
    (request, user, taxYear, employmentSequenceNumber) => {
      val dates = getCarBenefitDates(request)
      findEmployment(user, taxYear, employmentSequenceNumber) match {
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
    (request, user, taxYear, employmentSequenceNumber) => {
      findEmployment(user, taxYear, employmentSequenceNumber) match {
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

              val addBenefitPayload = AddBenefitCalculationData(addCarBenefitData.registeredBefore98.get, addCarBenefitData.fuelType.get, emission , addCarBenefitData.engineCapacity.map(_.toInt),
                                                                None,0,None,None,None,None,"",None)

              val uri = payeRoot.get.actions.getOrElse("addBenefit", throw new IllegalArgumentException(s"No addBenefit action uri found"))
                .replace("{year}", taxYear.toString).replace("{employment}", employmentSequenceNumber.toString)

              val response = payeService.addBenefit(uri, addBenefitPayload)

              val confirmationData =  AddCarBenefitConfirmationData(employment.employerName, addCarBenefitData.providedFrom.getOrElse(providedFromDefaultValue),
              addCarBenefitData.listPrice.get, addCarBenefitData.fuelType.get, addCarBenefitData.co2Figure, addCarBenefitData.engineCapacity,
              addCarBenefitData.employerPayFuel, addCarBenefitData.dateFuelWithdrawn, 100, Some(200))
              Ok(add_car_benefit_review(confirmationData)(user))
              //Ok("Calculated percentage: " + response.get.percentage.toString)
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
    def apply(action: (Request[_], User, Int, Int) => Result): (User, Request[_], Int, Int) => Result = {
      (user, request, taxYear, employmentSequenceNumber) => {
        if (TaxYearResolver.currentTaxYear != taxYear) {
          Logger.error("Adding car benefit is only allowed for the current tax year")
          BadRequest
        } else if (employmentSequenceNumber != findPrimaryEmployment(user).get.sequenceNumber) {
          Logger.error("Adding car benefit is only allowed for the primary employment")
          BadRequest
        } else {
          if (findExistingBenefit(user, employmentSequenceNumber, BenefitTypes.CAR).isDefined) {
            redirectToCarBenefitHome(request, user)
          } else {
            action(request, user, taxYear, employmentSequenceNumber)
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
                          registeredBefore98: Option[Boolean],
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
  val registeredBefore98 = "registeredBefore98"
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
