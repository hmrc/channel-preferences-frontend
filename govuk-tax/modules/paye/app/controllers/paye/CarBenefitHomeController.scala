package controllers.paye

import controllers.common.{CarBenefitHomeRedirect, SessionTimeoutWrapper, BaseController}
import play.api.mvc.{Result, Request}
import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import uk.gov.hmrc.common.microservice.paye.domain.Employment._
import models.paye.BenefitTypes
import uk.gov.hmrc.common.TaxYearResolver
import play.api.Logger
import uk.gov.hmrc.common.microservice.domain.User
import scala.Some
import org.joda.time.{LocalDate, DateTime}
import uk.gov.hmrc.utils.DateTimeUtils

class CarBenefitHomeController(timeSource: () => DateTime = () => DateTimeUtils.now) extends BaseController with SessionTimeoutWrapper with Benefits {

  def carBenefitHome = WithSessionTimeoutValidation {
    AuthorisedForIdaAction(taxRegime = Some(PayeRegime), redirectCommand = Some(CarBenefitHomeRedirect)) {
        user => request => carBenefitHomeAction(user, request)
    }
  }

  def startAddCarBenefit(taxYear: Int, employmentSequenceNumber: Int) =  WithSessionTimeoutValidation {
    AuthorisedForIdaAction(taxRegime = Some(PayeRegime), redirectCommand = Some(CarBenefitHomeRedirect)) {
      user => request => startAddCarBenefitAction(user, request, taxYear, employmentSequenceNumber)
    }
  }

  def saveAddCarBenefit =  WithSessionTimeoutValidation {
    AuthorisedForIdaAction(taxRegime = Some(PayeRegime)) {
        user => request => saveAddCarBenefitAction(user, request)
    }
  }

  private[paye] val carBenefitHomeAction: ((User, Request[_]) => Result) = (user, request) => {
    user.regimes.paye.get.employments(TaxYearResolver()).find(_.employmentType == primaryEmploymentType) match {
      case Some(employment) => {
        val carBenefit = findExistingBenefit(user, employment.sequenceNumber, BenefitTypes.CAR)
        val fuelBenefit = findExistingBenefit(user, employment.sequenceNumber, BenefitTypes.FUEL)

        Ok(views.html.paye.car_benefit_home(carBenefit, fuelBenefit, employment.employerName))
      }
      case None => {
        Logger.debug(s"Unable to find current employment for user ${user.oid}")
        InternalServerError
      }
    }
  }
  /*
  private val carBenefitForm = Form[CarBenefitData](
    mapping(
    )(CarBenefitData.apply)(CarBenefitData.unapply)
  )
  */

  private[paye] val startAddCarBenefitAction: ((User, Request[_], Int, Int) => Result) = (user, request, taxYear, employmentSequenceNumber) => {
    user.regimes.paye.get.employments(TaxYearResolver()).find(_.sequenceNumber == employmentSequenceNumber) match {
      case Some(employment) => {
        Ok(views.html.paye.add_car_benefit_form(employment.employerName))
      }
      case None => {
        Logger.debug(s"Unable to find employment for user ${user.oid} with sequence number ${employmentSequenceNumber}")
        BadRequest
      }
    }
  }

  private[paye] val saveAddCarBenefitAction: ((User, Request[_]) => Result) = (user, request) => {
    Ok
  }
}
case class CarBenefitData(providedFrom : Option[LocalDate], carUnavailable:  Boolean, numberOfDaysUnavailable: Option[Int], giveBackThisTaxYear: Boolean, providedTo: Option[LocalDate])

object CarBenefitFormFields {
  var providedFrom = "providedFrom"
  val carUnavailable = "carUnavailable"
  val numberOfDaysUnavailable = "numberOfDaysUnavailable"
  val giveBackThisTaxYear = "giveBackThisTaxYear"
  val providedTo = "providedTo"

}
