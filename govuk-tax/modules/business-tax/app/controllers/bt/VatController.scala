package controllers.bt

import play.api.mvc.Results
import controllers.common.{GovernmentGateway, ActionWrappers, BaseController}
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.PortalUrlBuilder
import uk.gov.hmrc.common.microservice.vat.domain.VatRegime
import views.helpers.{LinkMessage, RenderableMessage}

class VatController
  extends BaseController
  with ActionWrappers
  with PortalUrlBuilder {

  def makeAPayment = ActionAuthorisedBy(GovernmentGateway)(Some(VatRegime)) {
    implicit user =>
      implicit request =>
        Results.Ok(makeAPaymentPage(LinkMessage.portalLink(buildPortalUrl("vatOnlineAccount"))))
  }

  private[bt] def makeAPaymentPage(vatOnlineAccountLink: RenderableMessage)(implicit user: User) =
    views.html.make_a_vat_payment(vatOnlineAccountLink = vatOnlineAccountLink)
}


