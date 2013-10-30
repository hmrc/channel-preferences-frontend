package controllers.bt

import play.api.mvc.Results
import controllers.common.{Actions, GovernmentGateway, ActionWrappers, BaseController}
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.PortalUrlBuilder
import uk.gov.hmrc.common.microservice.ct.domain.CtRegime
import views.helpers.{LinkMessage, RenderableMessage}
import uk.gov.hmrc.common.microservice.sa.domain.SaRegime


class SaController
  extends BaseController
  with Actions
  with PortalUrlBuilder {

  def makeAPayment = ActionAuthorisedBy(GovernmentGateway)(Some(SaRegime)) {
    implicit user =>
      implicit request =>
        Results.Ok(makeAPaymentPage(LinkMessage.portalLink(buildPortalUrl("btDirectDebits")), user.getSa.utr.utr))
  }

  private[bt] def makeAPaymentPage(saDirectDebitsLink: RenderableMessage, utr: String)(implicit user: User) =
    views.html.make_a_sa_payment(saDirectDebitsLink, utr)
}


