package controllers.bt

import play.api.mvc.Request
import controllers.common.{Actions, GovernmentGateway, BaseController}
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.PortalUrlBuilder
import views.helpers.LinkMessage
import uk.gov.hmrc.common.microservice.sa.domain.SaRegime


class SaController
  extends BaseController
  with Actions
  with PortalUrlBuilder {

  def makeAPayment = ActionAuthorisedBy(GovernmentGateway)(Some(SaRegime)) {
    implicit user =>
      implicit request =>
        makeAPaymentPage
  }

  private[bt] def makeAPaymentPage(implicit user: User, request: Request[AnyRef]) = {
    val portalLink = LinkMessage.portalLink(buildPortalUrl("btDirectDebits"))
    val utr = user.getSa.utr.utr
    Ok(views.html.make_a_sa_payment(portalLink, utr))
  }
}


