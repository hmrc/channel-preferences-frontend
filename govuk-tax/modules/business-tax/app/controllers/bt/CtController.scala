package controllers.bt

import play.api.mvc.Results
import controllers.common.{Actions, GovernmentGateway, ActionWrappers, BaseController}
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.PortalUrlBuilder
import uk.gov.hmrc.common.microservice.ct.domain.CtRegime
import views.helpers.{LinkMessage, RenderableLinkMessage, RenderableMessage}


class CtController
  extends BaseController
  with Actions
  with PortalUrlBuilder {

  def makeAPayment = ActionAuthorisedBy(GovernmentGateway)(Some(CtRegime)) {
    implicit user =>
      implicit request =>
        Results.Ok(makeAPaymentPage(LinkMessage.portalLink(buildPortalUrl("ctAccountDetails")), LinkMessage.portalLink(buildPortalUrl("btDirectDebits"))))
  }

  private[bt] def makeAPaymentPage(ctOnlineAccountLink: RenderableMessage, ctDirectDebitsLink: RenderableMessage)(implicit user: User) =
    views.html.make_a_ct_payment(ctOnlineAccountLink = ctOnlineAccountLink, ctDirectDebitsLink = ctDirectDebitsLink)
}


