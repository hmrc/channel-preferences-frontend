package controllers.bt

import play.api.mvc.{AnyContent, Results, Action}
import play.api.templates.Html
import controllers.common.{SessionTimeoutWrapper, ActionWrappers, BaseController}
import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.VatRegime


class VatController extends BaseController with ActionWrappers with SessionTimeoutWrapper {
  def makeAPayment: Action[AnyContent] = WithSessionTimeoutValidation(AuthorisedForGovernmentGatewayAction(Some(VatRegime)) {
    implicit user =>
      request =>
        Results.Ok(makeAPaymentPage)
  })

  private[bt] def makeAPaymentPage: Html = {
    views.html.make_a_vat_payment()
  }
}
