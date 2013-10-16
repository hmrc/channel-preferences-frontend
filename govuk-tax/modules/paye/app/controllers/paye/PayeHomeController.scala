package controllers.paye

import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import uk.gov.hmrc.common.microservice.domain.User
import play.api.mvc.{Result, Request}
import models.paye.{EmploymentViews, PayeOverview}
import views.html.paye.paye_home
import controllers.common.BaseController
import uk.gov.hmrc.utils.TaxYearResolver

class PayeHomeController extends BaseController {

  def home = AuthorisedForIdaAction(Some(PayeRegime)) {
    implicit user =>
      implicit request =>
        homeAction(request)(user)
  }

  private[paye] def homeAction(request: Request[_])(implicit user: User): Result = {
    val payeData = user.getPaye
    val userAuthority = user.userAuthority
    val taxYear = TaxYearResolver.currentTaxYear
    val benefits = payeData.get.benefits(taxYear)
    val employments = payeData.get.employments(taxYear)

    Ok(
      paye_home(
        PayeOverview(
          name = payeData.get.name,
          lastLogin = userAuthority.previouslyLoggedInAt,
          nino = payeData.get.nino,
          hasBenefits = !benefits.isEmpty,
          employmentViews =
            EmploymentViews(
              employments = employments,
              taxCodes = payeData.get.taxCodes(taxYear),
              taxYear = taxYear,
              acceptedTransactions = payeData.get.recentAcceptedTransactions(),
              completedTransactions = payeData.get.recentCompletedTransactions())
        )
      )
    )
  }
}
