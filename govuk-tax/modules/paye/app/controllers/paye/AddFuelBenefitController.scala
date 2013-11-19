package controllers.paye

import controllers.common.BaseController
import uk.gov.hmrc.common.microservice.paye.domain._
import controllers.common.validators.Validators
import controllers.common.service.Connectors
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import play.api.mvc.Request
import controllers.paye.validation.WithValidatedFuelRequest
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import uk.gov.hmrc.common.microservice.txqueue.TxQueueConnector
import play.api.Logger
import play.api.data.Forms._
import play.api.data.Form
import FuelBenefitFormFields._
import controllers.paye.validation.AddCarBenefitValidator._
import org.joda.time.LocalDate
import play.api.mvc.SimpleResult
import uk.gov.hmrc.common.microservice.domain.User
import controllers.paye.validation.AddCarBenefitValidator.CarBenefitValues
import controllers.common.actions.Actions
import uk.gov.hmrc.common.microservice.paye.domain.AddFuelBenefitConfirmationData
import uk.gov.hmrc.common.microservice.paye.domain.TaxYearData
import scala.Some
import uk.gov.hmrc.common.microservice.paye.domain.BenefitValue
import play.api.mvc.SimpleResult
import controllers.paye.EmployerPayeFuelString
import uk.gov.hmrc.common.microservice.domain.User
import controllers.paye.validation.AddCarBenefitValidator.CarBenefitValues
import controllers.paye.FuelBenefitData
import models.paye.{CarBenefitDataAndCalculations, CarAndFuelBuilder}


class AddFuelBenefitController(override val auditConnector: AuditConnector, override val authConnector: AuthConnector)
                              (implicit payeConnector: PayeConnector, txQueueConnector: TxQueueConnector) extends BaseController
with Actions
with Validators
with TaxYearSupport {

  def this() = this(Connectors.auditConnector, Connectors.authConnector)(Connectors.payeConnector, Connectors.txQueueConnector)

  def startAddFuelBenefit(taxYear: Int, employmentSequenceNumber: Int) =
    AuthorisedFor(account = PayeRegime, redirectToOrigin = true) {
      user => request => startAddFuelBenefitAction(user, request, taxYear, employmentSequenceNumber)
    }

  def reviewAddFuelBenefit(taxYear: Int, employmentSequenceNumber: Int) =
    AuthorisedFor(PayeRegime) {
      user => request => reviewAddFuelBenefitAction(user, request, taxYear, employmentSequenceNumber)
    }

  def confirmAddingBenefit(taxYear: Int, employmentSequenceNumber: Int) =
    AuthorisedFor(PayeRegime) {
      user => request => Ok
    }

  private def fuelBenefitForm(values: CarBenefitValues) = Form[FuelBenefitData](
    mapping(
      employerPayFuel -> validateEmployerPayFuel(values),
      dateFuelWithdrawn -> validateDateFuelWithdrawn(values, taxYearInterval)
    )(FuelBenefitData.apply)(FuelBenefitData.unapply)
  )

  private def validationLessForm() = Form[EmployerPayeFuelString](
    mapping(
      employerPayFuel -> optional(text)
    )(EmployerPayeFuelString.apply)(EmployerPayeFuelString.unapply)
  )

  private[paye] def startAddFuelBenefitAction: (User, Request[_], Int, Int) => SimpleResult = WithValidatedFuelRequest {
    (request, user, taxYear, employmentSequenceNumber, payeRootData) => {

      findEmployment(employmentSequenceNumber, payeRootData) match {
        case Some(employment) => {
          Ok(views.html.paye.add_fuel_benefit_form(fuelBenefitForm(CarBenefitValues()), taxYear, employmentSequenceNumber, employment.employerName)(user))
        }
        case None => {
          Logger.debug(s"Unable to find employment for user ${user.oid} with sequence number $employmentSequenceNumber")
          BadRequest
        }
      }
    }
  }

  private[paye] def reviewAddFuelBenefitAction: (User, Request[_], Int, Int) => SimpleResult = WithValidatedFuelRequest {
    (request, user, taxYear, employmentSequenceNumber, payeRootData) => {
      findEmployment(employmentSequenceNumber, payeRootData) match {
        case Some(employment) => {
          val validationLesForm = validationLessForm.bindFromRequest()(request)

          //TODO: Why is the car provided from the start of the tax year?
          val values = CarBenefitValues(providedFromVal = Some(startOfCurrentTaxYear), employerPayFuel = validationLesForm.get.employerPayFuel)
          fuelBenefitForm(values).bindFromRequest()(request).fold(
            errors => {
              BadRequest(views.html.paye.add_fuel_benefit_form(errors, taxYear, employmentSequenceNumber, employment.employerName)(user))
            },
            (addFuelBenefitData: FuelBenefitData) => {
              val carBenefit = payeRootData.findExistingBenefit(employmentSequenceNumber, BenefitTypes.CAR)
              val benefitStartDate = getDateInTaxYear(carBenefit.get.car.flatMap(_.dateCarMadeAvailable))
              val payeRoot = user.regimes.paye.get
              val uri = payeRoot.actions.getOrElse("calculateBenefitValue", throw new IllegalArgumentException(s"No calculateBenefitValue action uri found"))

              val benefitCalculations = payeConnector.calculateBenefitValue(uri, CarAndFuelBuilder(addFuelBenefit = addFuelBenefitData, carBenefit.get, taxYear, employmentSequenceNumber)).get
              val fuelBenefitValue = benefitCalculations.fuelBenefitValue.map(BenefitValue)
              val fuelData = AddFuelBenefitConfirmationData(employment.employerName, benefitStartDate, addFuelBenefitData.employerPayFuel.get, addFuelBenefitData.dateFuelWithdrawn, carFuelBenefitValue = fuelBenefitValue)
              Ok(views.html.paye.add_fuel_benefit_review(fuelData, request.uri, currentTaxYearYearsRange, taxYear, employmentSequenceNumber, user))
            })
        }
        case None => {
          Logger.debug(s"Unable to find employment for user ${user.oid} with sequence number $employmentSequenceNumber")
          BadRequest
        }
      }
    }
  }

  private def getDateInTaxYear(benefitDate: Option[LocalDate]) = {
    benefitDate match {
      case Some(date) if date.isAfter(startOfCurrentTaxYear) => benefitDate
      case _ => Some(startOfCurrentTaxYear)
    }
  }

  private def findEmployment(employmentSequenceNumber: Int, payeRootData: TaxYearData) = {
    payeRootData.employments.find(_.sequenceNumber == employmentSequenceNumber)
  }
}

case class FuelBenefitData(employerPayFuel: Option[String], dateFuelWithdrawn: Option[LocalDate])

case class EmployerPayeFuelString(employerPayFuel: Option[String])

object FuelBenefitFormFields {
  val employerPayFuel = "employerPayFuel"
  val dateFuelWithdrawn = "dateFuelWithdrawn"
}