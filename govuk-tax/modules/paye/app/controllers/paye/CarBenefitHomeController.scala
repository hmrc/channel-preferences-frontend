package controllers.paye

import controllers.common.BaseController
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.common.microservice.paye.domain.{PayeRootData, Employment, PayeRegime}
import uk.gov.hmrc.common.microservice.paye.domain.Employment._
import models.paye.BenefitTypes
import play.api.Logger
import uk.gov.hmrc.utils.TaxYearResolver
import controllers.common.validators.Validators
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
    val payeRootData = user.regimes.paye.get.fetchTaxYearData(currentTaxYear)

    findPrimaryEmployment(payeRootData) match {
      case Some(employment) => {

        val carBenefit = findExistingBenefit(employment.sequenceNumber, BenefitTypes.CAR, payeRootData)
        val fuelBenefit = findExistingBenefit(employment.sequenceNumber, BenefitTypes.FUEL, payeRootData)

        Ok(views.html.paye.car_benefit_home(carBenefit, fuelBenefit, employment.employerName, employment.sequenceNumber, currentTaxYear))
      }
      case None => {
        Logger.debug(s"Unable to find current employment for user ${user.oid}")
        InternalServerError
      }
    }
  }

  private def findPrimaryEmployment(payeRootData: PayeRootData): Option[Employment] = {
    payeRootData.taxYearEmployments.find(_.employmentType == primaryEmploymentType)
  }
}
