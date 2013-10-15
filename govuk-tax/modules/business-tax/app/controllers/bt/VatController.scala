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
        Results.Ok(makeAPaymentPage(buildPortalUrl))
  })

  private[bt] def makeAPaymentPage(buildPortalUrl: String => String)(implicit user: User): Html = {
    val vatOnlineAccountHref: String = buildPortalUrl("vatOnlineAccount")
    views.html.make_a_vat_payment(vatOnlineAccountHref)
  }
}
