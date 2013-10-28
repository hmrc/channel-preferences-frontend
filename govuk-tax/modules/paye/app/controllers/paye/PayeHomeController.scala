package controllers.paye

import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import uk.gov.hmrc.common.microservice.domain.User
import play.api.mvc.{SimpleResult, Request}
import models.paye.{EmploymentViews, PayeOverview}
import views.html.paye.paye_home
import controllers.common.BaseController
import uk.gov.hmrc.utils.TaxYearResolver

class PayeHomeController extends BaseController {

  def home = ActionAuthorisedBy(Ida)(Some(PayeRegime)) {
    implicit user =>
      implicit request =>
        homeAction(request)(user)
  }

  private[paye] def homeAction(request: Request[_])(implicit user: User): SimpleResult = {
    val payeData = user.getPaye
    val userAuthority = user.userAuthority
    val taxYear = TaxYearResolver.currentTaxYear
    val benefits = payeData.fetchBenefits(taxYear)
    val employments = payeData.fetchEmployments(taxYear)

    Ok(
      paye_home(
        PayeOverview(
          name = payeData.name,
          lastLogin = userAuthority.previouslyLoggedInAt,
          nino = payeData.nino,
          hasBenefits = !benefits.isEmpty,
          employmentViews =
            EmploymentViews(
              employments = employments,
              taxCodes = payeData.fetchTaxCodes(taxYear),
              taxYear = taxYear,
              acceptedTransactions = payeData.fetchRecentAcceptedTransactions(),
              completedTransactions = payeData.fetchRecentCompletedTransactions())
        )
      )
    )
  }
}
