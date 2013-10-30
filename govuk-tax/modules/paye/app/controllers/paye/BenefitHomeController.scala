package controllers.paye

import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import play.api.mvc.{SimpleResult, Request}
import views.html.paye._
import scala.Some
import uk.gov.hmrc.common.microservice.domain.User
import models.paye.DisplayBenefits
import controllers.common.{Ida, SessionTimeoutWrapper, ActionWrappers, BaseController}
import uk.gov.hmrc.utils.TaxYearResolver
import uk.gov.hmrc.common.microservice.paye.PayeMicroService
import controllers.common.service.MicroServices

class BenefitHomeController(payeService: PayeMicroService) extends BaseController with ActionWrappers with SessionTimeoutWrapper {

  def this() = this(MicroServices.payeMicroService)

  def listBenefits = WithSessionTimeoutValidation(ActionAuthorisedBy(Ida)(Some(PayeRegime)) {
    implicit user: User => implicit request => listBenefitsAction(user, request)
  })

  private[paye] def listBenefitsAction(implicit user: User, request: Request[_]): SimpleResult = {
    val taxYear = TaxYearResolver.currentTaxYear
    val benefits = user.regimes.paye.get.fetchBenefits(taxYear)
    val employments = user.regimes.paye.get.fetchEmployments(taxYear)
    val transactions = user.regimes.paye.get.fetchRecentAcceptedTransactions ++
      user.regimes.paye.get.fetchRecentCompletedTransactions()

    Ok(paye_benefit_home(DisplayBenefits(benefits, employments, transactions)))
  }
}
