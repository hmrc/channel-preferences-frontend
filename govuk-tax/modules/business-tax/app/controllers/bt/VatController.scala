package controllers.bt

import play.api.mvc.Results
import controllers.common.{ActionWrappers, BaseController}
import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.VatRegime
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.PortalUrlBuilder

class VatController
  extends BaseController
  with ActionWrappers
  with PortalUrlBuilder {

  def makeAPayment = AuthorisedForGovernmentGatewayAction(Some(VatRegime)) {
    implicit user =>
      implicit request =>
        Results.Ok(makeAPaymentPage(buildPortalUrl("vatOnlineAccount")))
  }

  private[bt] def makeAPaymentPage(vatOnlineAccountHref: String)(implicit user: User) =
    views.html.make_a_vat_payment(vatOnlineAccountHref = vatOnlineAccountHref)
}


