package controllers.paye

import controllers.common.{CarBenefitHomeRedirect, SessionTimeoutWrapper, BaseController}
import play.api.mvc.{Result, Request}
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import models.paye.BenefitTypes
import uk.gov.hmrc.common.TaxYearResolver

class CarBenefitHomeController extends BaseController with SessionTimeoutWrapper with Benefits {

  def carBenefitHome = WithSessionTimeoutValidation {
    AuthorisedForIdaAction(taxRegime = Some(PayeRegime), redirectCommand = Some(CarBenefitHomeRedirect)) {
        user => request => carBenefitHomeAction(user, request)
    }
  }

  private[paye] val carBenefitHomeAction: ((User, Request[_]) => Result) = (user, request) => {
    val employerSequenceNumber = 1 // this is an assumption for the case of a single supported employment for Beta
    val carBenefit = findExistingBenefit(user, employerSequenceNumber, BenefitTypes.CAR)
    val fuelBenefit = findExistingBenefit(user, employerSequenceNumber, BenefitTypes.FUEL)
    val employerName = user.regimes.paye.get.employments(TaxYearResolver()).find(_.sequenceNumber == employerSequenceNumber).flatMap(_.employerName)

    Ok(views.html.paye.car_benefit_home(carBenefit, fuelBenefit, employerName))
  }
}
