package controllers.paye

import controllers.common.{Ida, BaseController}
import play.api.mvc.{SimpleResult, Request}
import uk.gov.hmrc.common.microservice.paye.domain.{TaxYearData, Employment, PayeRegime}
import uk.gov.hmrc.common.microservice.paye.domain.Employment._
import models.paye.EmploymentViews
import uk.gov.hmrc.common.microservice.paye.domain.BenefitTypes
import play.api.Logger
import uk.gov.hmrc.utils.TaxYearResolver
import controllers.common.validators.Validators
import uk.gov.hmrc.common.microservice.domain.User
import controllers.common.service.Connectors
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import uk.gov.hmrc.common.microservice.txqueue.TxQueueConnector
import controllers.common.actions.{HeaderCarrier, Actions}

class CarBenefitHomeController(override val auditConnector: AuditConnector, override val authConnector: AuthConnector)(implicit payeService: PayeConnector, txQueueMicroservice: TxQueueConnector) extends BaseController
  with Actions
  with Validators
  with PayeRegimeRoots {

  private[paye] def currentTaxYear = TaxYearResolver.currentTaxYear
  val interestingBenefitTypes = Set(BenefitTypes.CAR, BenefitTypes.FUEL)

  def this() = this(Connectors.auditConnector, Connectors.authConnector)(Connectors.payeConnector, Connectors.txQueueConnector)

  def carBenefitHome = AuthorisedFor(account = PayeRegime, redirectToOrigin = true) {
    implicit user =>
      implicit request =>
        carBenefitHomeAction
  }

  private[paye] def carBenefitHomeAction(implicit user: User, request: Request[_]): SimpleResult = {

    implicit val hc = HeaderCarrier(request)

    val payeRoot = user.getPaye
    val currentTaxYearData = payeRoot.fetchTaxYearData(currentTaxYear)

    findPrimaryEmployment(currentTaxYearData) match {
      case Some(employment) => {

        val carBenefit = currentTaxYearData.findExistingBenefit(employment.sequenceNumber, BenefitTypes.CAR)
        val fuelBenefit = currentTaxYearData.findExistingBenefit(employment.sequenceNumber, BenefitTypes.FUEL)

        val employmentViews = EmploymentViews.createEmploymentViews(payeRoot.fetchEmployments(currentTaxYear), payeRoot.fetchTaxCodes(currentTaxYear), currentTaxYear,
          interestingBenefitTypes, payeRoot.fetchRecentAcceptedTransactions(), payeRoot.fetchRecentCompletedTransactions())

        Ok(views.html.paye.car_benefit_home(carBenefit, fuelBenefit, employment.employerName, employment.sequenceNumber, currentTaxYear, employmentViews))
      }
      case None => {
        val errorMessage = s"Unable to find current employment for user ${user.oid}"
        Logger.error(errorMessage)
        InternalServerError(views.html.paye.error_no_data_car_benefit_home(errorMessage))
      }
    }
  }

  private def findPrimaryEmployment(payeRootData: TaxYearData): Option[Employment] = {
    payeRootData.employments.find(_.employmentType == primaryEmploymentType)
  }
}
