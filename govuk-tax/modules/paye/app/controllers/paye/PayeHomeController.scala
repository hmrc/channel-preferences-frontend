package controllers.paye

import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import uk.gov.hmrc.common.microservice.domain.User
import play.api.mvc.{ Result, Request }
import models.paye.{ EmploymentViews, PayeOverview, EmploymentView }
import views.html.paye.paye_home
import controllers.common.{ SessionTimeoutWrapper, ActionWrappers, BaseController }
import uk.gov.hmrc.utils.TaxYearResolver

class PayeHomeController extends BaseController with ActionWrappers with SessionTimeoutWrapper {

  def home = WithSessionTimeoutValidation { AuthorisedForIdaAction(Some(PayeRegime)) { implicit user: User => implicit request => homeAction(request)(user) } }

  private[paye] def homeAction(request: Request[_])(implicit user: User): Result = {
    val payeData = user.regimes.paye.get
    val taxYear = TaxYearResolver.currentTaxYear
    val benefits = payeData.benefits(taxYear)
    val employments = payeData.employments(taxYear)
    val taxCodes = payeData.taxCodes(taxYear)

    val employmentViews: Seq[EmploymentView] =
      EmploymentViews(employments, taxCodes, taxYear, payeData.recentAcceptedTransactions(), payeData.recentCompletedTransactions())

    Ok(paye_home(PayeOverview(payeData.name, user.userAuthority.previouslyLoggedInAt, payeData.nino, employmentViews, !benefits.isEmpty)))
  }
}
