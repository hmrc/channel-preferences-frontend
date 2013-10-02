package controllers.paye

import controllers.common.{SessionTimeoutWrapper, BaseController}
import play.api.mvc.{Result, Request}
import uk.gov.hmrc.common.microservice.paye.domain.{Employment, PayeRegime}
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

class CarBenefitAddController(timeSource: () => DateTime, keyStoreService: KeyStoreMicroService) extends BaseController with SessionTimeoutWrapper with Benefits with Validators {

  def this() = this(() => DateTimeUtils.now, MicroServices.keyStoreMicroService)

  private[paye] def currentTaxYear = TaxYearResolver.currentTaxYear
  private[paye] def startOfCurrentTaxYear = TaxYearResolver.startOfCurrentTaxYear
  private[paye] def endOfCurrentTaxYear = TaxYearResolver.endOfCurrentTaxYear

  def startAddCarBenefit(taxYear: Int, employmentSequenceNumber: Int) = WithSessionTimeoutValidation {
    AuthorisedForIdaAction(taxRegime = Some(PayeRegime), redirectToOrigin = true) {
      user => request => startAddCarBenefitAction(user, request, taxYear, employmentSequenceNumber)
    }
  }

  def saveAddCarBenefit(taxYear: Int, employmentSequenceNumber: Int) = WithSessionTimeoutValidation {
    AuthorisedForIdaAction(taxRegime = Some(PayeRegime)) {
      user => request => saveAddCarBenefitAction(user, request, taxYear, employmentSequenceNumber)
    }
  }

  private def findPrimaryEmployment(user: User) : Option[Employment] = {
    user.regimes.paye.get.employments(currentTaxYear).find(_.employmentType == primaryEmploymentType)
  }

  private def getCarBenefitDates(request:Request[_]):CarBenefitValues = {
    datesForm(startOfCurrentTaxYear, endOfCurrentTaxYear).bindFromRequest()(request).value.get
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

  private[paye] val startAddCarBenefitAction: (User, Request[_], Int, Int) => Result = WithValidatedRequest {
    (request, user, taxYear, employmentSequenceNumber) => {
      val dates = getCarBenefitDates(request)
      user.regimes.paye.get.employments(taxYear).find(_.sequenceNumber == employmentSequenceNumber) match {
        case Some(employment) => {
          Ok(views.html.paye.add_car_benefit_form(carBenefitForm(dates), employment.employerName, taxYear, employmentSequenceNumber, TaxYearResolver.currentTaxYearYearsRange))
        }
        case None => {
          Logger.debug(s"Unable to find employment for user ${user.oid} with sequence number ${employmentSequenceNumber}")
          BadRequest
        }
      }
    }
  }

  private[paye] val saveAddCarBenefitAction: (User, Request[_], Int, Int) => Result = WithValidatedRequest {
    (request, user, taxYear, employmentSequenceNumber) => {
      user.regimes.paye.get.employments(taxYear).find(_.sequenceNumber == employmentSequenceNumber) match {
        case Some(employment) => {
          val dates = getCarBenefitDates(request)
          carBenefitForm(dates).bindFromRequest()(request).fold(
            errors => {
              BadRequest(views.html.paye.add_car_benefit_form(errors, employment.employerName, taxYear, employmentSequenceNumber, TaxYearResolver.currentTaxYearYearsRange))
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

  object WithValidatedRequest {
    def apply(action: (Request[_], User, Int, Int) => Result): (User, Request[_], Int, Int) => Result = {
      (user, request, taxYear, employmentSequenceNumber) => {
        if(TaxYearResolver.currentTaxYear != taxYear ) {
          Logger.error("Adding car benefit is only allowed for the current tax year")
          BadRequest
        } else if (employmentSequenceNumber != findPrimaryEmployment(user).get.sequenceNumber){
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

case class CarBenefitData(providedFrom: Option[LocalDate], carUnavailable: Option[Boolean], numberOfDaysUnavailable: Option[Int], giveBackThisTaxYear: Option[Boolean], providedTo: Option[LocalDate],
                          listPrice: Option[Int], employeeContributes: Option[Boolean], employeeContribution: Option[Int], employerContributes: Option[Boolean], employerContribution: Option[Int])

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
