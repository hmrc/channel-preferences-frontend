package controllers.paye

import controllers.common.BaseController
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.common.microservice.paye.domain.{Employment, PayeRegime}
import uk.gov.hmrc.common.microservice.paye.domain.Employment._
import models.paye.BenefitTypes
import play.api.Logger
import uk.gov.hmrc.utils.TaxYearResolver
import controllers.common.validators.Validators
import scala.Some
import uk.gov.hmrc.common.microservice.domain.User

class CarBenefitHomeController
  extends BaseController
  with Benefits
  with Validators {

  private[paye] def currentTaxYear = TaxYearResolver.currentTaxYear

  def carBenefitHome = AuthorisedForIdaAction(taxRegime = Some(PayeRegime), redirectToOrigin = true) {
    implicit user =>
      implicit request =>
        carBenefitHomeAction
  }

  private[paye] def carBenefitHomeAction(implicit user: User, request: Request[_]): Result = {
    findPrimaryEmployment(user) match {
      case Some(employment) => {
        val carBenefit = findExistingBenefit(user, employment.sequenceNumber, BenefitTypes.CAR)
        val fuelBenefit = findExistingBenefit(user, employment.sequenceNumber, BenefitTypes.FUEL)

        Ok(views.html.paye.car_benefit_home(carBenefit, fuelBenefit, employment.employerName, employment.sequenceNumber, currentTaxYear))
      }
      case None => {
        Logger.debug(s"Unable to find current employment for user ${user.oid}")
        InternalServerError
      }
    }
  }

  private def findPrimaryEmployment(user: User): Option[Employment] = {
    user.regimes.paye.get.fetchEmployments(currentTaxYear).find(_.employmentType == primaryEmploymentType)
  }
}
