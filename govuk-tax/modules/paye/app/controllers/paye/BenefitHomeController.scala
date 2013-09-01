package controllers.paye

import uk.gov.hmrc.microservice.paye.domain.PayeRegime
import play.api.mvc.{ Result, Request }
import views.html.paye._
import scala.Some
import uk.gov.hmrc.microservice.domain.User
import models.paye.DisplayBenefits

class BenefitHomeController extends PayeController {

  def listBenefits = WithSessionTimeoutValidation(AuthorisedForIdaAction(Some(PayeRegime)) {
    user => request => listBenefitsAction(user, request)
  })

  private[paye] val listBenefitsAction: (User, Request[_]) => Result = (user, request) => {
    val taxYear = currentTaxYear
    val benefits = user.regimes.paye.get.benefits(taxYear)
    val employments = user.regimes.paye.get.employments(taxYear)
    val transactions = user.regimes.paye.get.recentAcceptedTransactions ++
      user.regimes.paye.get.recentCompletedTransactions()

    Ok(paye_benefit_home(DisplayBenefits(benefits, employments, transactions)))
  }
}
