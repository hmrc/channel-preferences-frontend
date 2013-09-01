package controllers.paye

import uk.gov.hmrc.microservice.paye.domain.PayeRegime
import uk.gov.hmrc.microservice.domain.User
import play.api.mvc.{ Result, Request }
import models.paye.{ EmploymentViews, PayeOverview, EmploymentView }
import views.html.paye.paye_home

class PayeHomeController extends PayeController {

  def home = WithSessionTimeoutValidation { AuthorisedForIdaAction(Some(PayeRegime)) { user => request => homeAction(user, request) } }

  private[paye] val homeAction: (User, Request[_]) => Result = (user, request) => {
    val payeData = user.regimes.paye.get
    val taxYear = currentTaxYear
    val benefits = payeData.benefits(taxYear)
    val employments = payeData.employments(taxYear)
    val taxCodes = payeData.taxCodes(taxYear)

    val employmentViews: Seq[EmploymentView] =
      EmploymentViews(employments, taxCodes, taxYear, payeData.recentAcceptedTransactions(), payeData.recentCompletedTransactions())

    Ok(paye_home(PayeOverview(payeData.name, user.userAuthority.previouslyLoggedInAt, payeData.nino, employmentViews, !benefits.isEmpty)))
  }
}
