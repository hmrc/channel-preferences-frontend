package controllers.paye

import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import play.api.mvc.{ Result, Request }
import views.html.paye._
import scala.Some
import uk.gov.hmrc.common.microservice.domain.User
import models.paye.DisplayBenefits
import controllers.common.{ SessionTimeoutWrapper, ActionWrappers, BaseController }
import uk.gov.hmrc.utils.TaxYearResolver
import uk.gov.hmrc.common.microservice.paye.PayeMicroService
import controllers.common.service.MicroServices

class BenefitHomeController(payeService : PayeMicroService) extends BaseController with ActionWrappers with SessionTimeoutWrapper {

  def this() = this(MicroServices.payeMicroService)

  def listBenefits = WithSessionTimeoutValidation(AuthorisedForIdaAction(Some(PayeRegime)) {
    implicit user: User => implicit request => listBenefitsAction(user, request)
  })

  private[paye] def listBenefitsAction(implicit user: User, request: Request[_]): Result = {
    val taxYear = TaxYearResolver.currentTaxYear
    val benefits = user.regimes.paye.get.get.benefits(taxYear)
    val employments = user.regimes.paye.get.get.employments(taxYear)
    val transactions = user.regimes.paye.get.get.recentAcceptedTransactions ++
      user.regimes.paye.get.get.recentCompletedTransactions()

    Ok(paye_benefit_home(DisplayBenefits(benefits, employments, transactions)))
  }
}
