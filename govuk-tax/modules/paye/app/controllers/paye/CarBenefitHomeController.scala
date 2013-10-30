package controllers.paye

import controllers.common.{Ida, Actions, BaseController2}
import play.api.mvc.{SimpleResult, Request}
import uk.gov.hmrc.common.microservice.paye.domain.{PayeRootData, Employment, PayeRegime}
import uk.gov.hmrc.common.microservice.paye.domain.Employment._
import models.paye.BenefitTypes
import play.api.Logger
import uk.gov.hmrc.utils.TaxYearResolver
import controllers.common.validators.Validators
import uk.gov.hmrc.common.microservice.domain.User
import controllers.common.service.MicroServices
import uk.gov.hmrc.common.microservice.audit.AuditMicroService
import uk.gov.hmrc.common.microservice.auth.AuthMicroService
import uk.gov.hmrc.common.microservice.paye.PayeMicroService
import uk.gov.hmrc.microservice.txqueue.TxQueueMicroService

class CarBenefitHomeController(override val auditMicroService: AuditMicroService, override val authMicroService: AuthMicroService)(implicit payeService: PayeMicroService, txQueueMicroservice: TxQueueMicroService) extends BaseController2
  with Actions
  with Benefits
  with Validators {

  private[paye] def currentTaxYear = TaxYearResolver.currentTaxYear

  def this() = this(MicroServices.auditMicroService, MicroServices.authMicroService)(MicroServices.payeMicroService, MicroServices.txQueueMicroService)

  def carBenefitHome = ActionAuthorisedBy(Ida)(taxRegime = Some(PayeRegime), redirectToOrigin = true) {
    implicit user =>
      implicit request =>
        carBenefitHomeAction
  }

  private[paye] def carBenefitHomeAction(implicit user: User, request: Request[_]): SimpleResult = {
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
