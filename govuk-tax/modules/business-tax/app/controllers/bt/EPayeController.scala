package controllers.bt

import play.api.mvc.{Request, Results}
import controllers.common.{Actions, GovernmentGateway, BaseController}
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.PortalUrlBuilder
import uk.gov.hmrc.common.microservice.ct.domain.CtRegime
import views.helpers.{LinkMessage, RenderableMessage}


class EpayeController
  extends BaseController
  with Actions
  with PortalUrlBuilder {

  def makeAPayment = ActionAuthorisedBy(GovernmentGateway)(Some(CtRegime)) {
    implicit user =>
      implicit request =>
        makeAPaymentPage
  }

  private[bt] def makeAPaymentPage(implicit user: User, request: Request[AnyRef]) = {
    val portalLink = LinkMessage.portalLink(buildPortalUrl("btDirectDebits"))
    Ok(views.html.make_a_epaye_payment(portalLink))
  }
}


