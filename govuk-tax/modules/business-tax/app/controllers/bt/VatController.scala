package controllers.bt

import play.api.mvc.{AnyContent, Results, Action}
import play.api.templates.Html
import controllers.common.{SessionTimeoutWrapper, ActionWrappers, BaseController}
import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.VatRegime
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.PortalDestinationUrlBuilder


class VatController extends BaseController with ActionWrappers with SessionTimeoutWrapper with PortalDestinationUrlBuilder {
  def makeAPayment: Action[AnyContent] = WithSessionTimeoutValidation(AuthorisedForGovernmentGatewayAction(Some(VatRegime)) {
    implicit user =>
      implicit request =>
        val vatOnlineAccountHref: String = buildPortalUrl("vatOnlineAccount")
        Results.Ok(makeAPaymentPage(vatOnlineAccountHref))
  })

  private[bt] def makeAPaymentPage(vatOnlineAccountHref: String)(implicit user: User): Html = {
    views.html.make_a_vat_payment(vatOnlineAccountHref)
  }
}
