package controllers.bt

import controllers.common.{GovernmentGateway, Actions, BaseController}
import uk.gov.hmrc.common.PortalUrlBuilder
import uk.gov.hmrc.common.microservice.sa.domain.SaRegime
import uk.gov.hmrc.common.microservice.domain.User
import play.api.mvc.Request
import views.helpers.LinkMessage
import uk.gov.hmrc.common.microservice.ct.domain.CtRegime
import uk.gov.hmrc.common.microservice.vat.domain.VatRegime
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeRegime


class PaymentController extends BaseController
with Actions
with PortalUrlBuilder {

  def makeEpayePayment = ActionAuthorisedByWithVisibility(GovernmentGateway)(Some(EpayeRegime))(EpayePaymentPredicate) {
      user => request =>
        makeEpayePaymentPage(user, request)
  }

  def makeSaPayment = ActionAuthorisedBy(GovernmentGateway)(Some(SaRegime)) {
     user => request => makeSaPaymentPage(user, request)
  }

  def makeCtPayment = ActionAuthorisedBy(GovernmentGateway)(Some(CtRegime)) {
    user => request => makeCtPaymentPage(user, request)
  }

  def makeVatPayment = ActionAuthorisedBy(GovernmentGateway)(Some(VatRegime)) {
    user => request => makeVatPaymentPage(user, request)
  }

  private[bt] def makeVatPaymentPage(implicit user: User, request: Request[AnyRef]) = {
    val portalLink = LinkMessage.portalLink(buildPortalUrl("vatOnlineAccount"))
    Ok(views.html.make_a_vat_payment(portalLink))
  }

  private[bt] def makeSaPaymentPage(implicit user: User, request: Request[AnyRef]) = {
    val portalLink = LinkMessage.portalLink(buildPortalUrl("btDirectDebits"))
    val utr = user.getSa.utr.utr
    Ok(views.html.make_a_sa_payment(portalLink, utr))
  }

  private[bt] def makeEpayePaymentPage(implicit user: User, request: Request[AnyRef]) = {
    val portalLink = LinkMessage.portalLink(buildPortalUrl("btDirectDebits"))
    Ok(views.html.make_a_epaye_payment(portalLink))
  }

  private[bt] def makeCtPaymentPage(implicit user: User, request: Request[AnyRef]) = {
    val accountDetailsLink = buildPortalUrl("ctAccountDetails")
    val directDebitsLink = buildPortalUrl("btDirectDebits")
    Ok(views.html.make_a_ct_payment(LinkMessage.portalLink(accountDetailsLink), LinkMessage.portalLink(directDebitsLink)))
  }
}
