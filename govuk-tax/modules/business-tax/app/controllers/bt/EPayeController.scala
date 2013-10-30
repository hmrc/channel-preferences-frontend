package controllers.bt

import play.api.mvc.Results
import controllers.common.{Actions, GovernmentGateway, BaseController}
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.PortalUrlBuilder
import uk.gov.hmrc.common.microservice.ct.domain.CtRegime
import views.helpers.{LinkMessage, RenderableMessage}


class EPayeController
  extends BaseController
  with Actions
  with PortalUrlBuilder {

  def makeAPayment = ActionAuthorisedBy(GovernmentGateway)(Some(CtRegime)) {
    implicit user =>
      implicit request =>
        Results.Ok(makeAPaymentPage(LinkMessage.portalLink(buildPortalUrl("ePayeAccountDetails"))))
  }

  private[bt] def makeAPaymentPage(ePayeOnlineAccountLink: RenderableMessage)(implicit user: User) =
    views.html.make_a_epaye_payment(ePayeOnlineAccountLink)
}


