package controllers.bt

import controllers.common.BaseController
import uk.gov.hmrc.common.PortalUrlBuilder
import uk.gov.hmrc.common.microservice.sa.domain.SaRegime
import uk.gov.hmrc.common.microservice.domain.User
import play.api.mvc.Request
import uk.gov.hmrc.common.microservice.ct.domain.CtRegime
import uk.gov.hmrc.common.microservice.vat.domain.VatRegime
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeRegime
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import controllers.common.service.Connectors
import controllers.common.actions.Actions


class PaymentController(override val auditConnector: AuditConnector)
                       (implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with BusinessTaxRegimeRoots {

  def this() = this(Connectors.auditConnector)(Connectors.authConnector)

  def makeEpayePayment = AuthorisedFor(EpayeRegime, pageVisibility = EpayePaymentPredicate) {
    user =>
      request =>
        makeEpayePaymentPage(user, request)
  }

  def makeSaPayment = AuthorisedFor(SaRegime, pageVisibility = SaPaymentPredicate) {
    user => request => makeSaPaymentPage(user, request)
  }

  def makeCtPayment = AuthorisedFor(CtRegime, pageVisibility = CtPaymentPredicate) {
    user => request => makeCtPaymentPage(user, request)
  }

  def makeVatPayment = AuthorisedFor(VatRegime, pageVisibility = VatPaymentPredicate) {
    user => request => makeVatPaymentPage(user, request)
  }

  private[bt] def makeVatPaymentPage(implicit user: User, request: Request[AnyRef]) = {
    Ok(views.html.make_a_vat_payment())
  }

  private[bt] def makeSaPaymentPage(implicit user: User, request: Request[AnyRef]) = {
    Ok(views.html.make_a_sa_payment(user.getSaUtr.utr))
  }

  private[bt] def makeEpayePaymentPage(implicit user: User, request: Request[AnyRef]) = {
    Ok(views.html.make_a_epaye_payment())
  }

  private[bt] def makeCtPaymentPage(implicit user: User, request: Request[AnyRef]) = {
    Ok(views.html.make_a_ct_payment())
  }
}
