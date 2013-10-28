package controllers.bt

import play.api.mvc.Results
import controllers.common.{ActionWrappers, BaseController}
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.PortalUrlBuilder
import uk.gov.hmrc.common.microservice.ct.domain.CtDomain.CtRegime

class CtController
  extends BaseController
  with ActionWrappers
  with PortalUrlBuilder {

  def makeAPayment = ActionAuthorisedBy(GovernmentGateway)(Some(CtRegime)) {
    implicit user =>
      implicit request =>
        Results.Ok(makeAPaymentPage(buildPortalUrl("ctAccountDetails"), buildPortalUrl("ctDirectDebits")))
  }

  private[bt] def makeAPaymentPage(ctOnlineAccountHref: String, ctDirectDebitsHref: String)(implicit user: User) =
    views.html.make_a_ct_payment(ctOnlineAccountHref = ctOnlineAccountHref, ctDirectDebitsHref = ctDirectDebitsHref)
}


