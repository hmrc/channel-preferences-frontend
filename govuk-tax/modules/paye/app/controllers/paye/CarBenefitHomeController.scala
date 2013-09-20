package controllers.paye

import controllers.common.{CarBenefitHomeRedirect, SessionTimeoutWrapper, BaseController}
import play.api.mvc.{Result, Request}
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import uk.gov.hmrc.common.microservice.paye.domain.Employment._
import models.paye.BenefitTypes
import uk.gov.hmrc.common.TaxYearResolver
import play.api.Logger

class CarBenefitHomeController extends BaseController with SessionTimeoutWrapper with Benefits {

  def carBenefitHome = WithSessionTimeoutValidation {
    AuthorisedForIdaAction(taxRegime = Some(PayeRegime), redirectCommand = Some(CarBenefitHomeRedirect)) {
        user => request => carBenefitHomeAction(user, request)
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
}
