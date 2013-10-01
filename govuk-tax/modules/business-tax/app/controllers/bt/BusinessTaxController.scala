package controllers.bt

import controllers.common._
import uk.gov.hmrc.common.PortalDestinationUrlBuilder
import controllers.bt.regimeViews._
import uk.gov.hmrc.common.microservice.sa.SaConnector
import uk.gov.hmrc.common.microservice.vat.VatConnector
import uk.gov.hmrc.common.microservice.epaye.EpayeConnector
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.ct.CtConnector

class BusinessTaxController(accountSummaryFactory : AccountSummariesFactory) extends BaseController with ActionWrappers with SessionTimeoutWrapper with PortalDestinationUrlBuilder {

  private[bt] def makeAPaymentLandingPage()(implicit user: User) = views.html.make_a_payment_landing()
  private[bt] def businessTaxHomepage(portalHref: String, accountSummaries: AccountSummaries)(implicit user: User) = views.html.business_tax_home(portalHref, accountSummaries)

  def this() = {
    this(new AccountSummariesFactory(new SaConnector, new VatConnector, new CtConnector, new EpayeConnector))
  }

  def home = WithSessionTimeoutValidation(AuthorisedForGovernmentGatewayAction() {
    implicit user: User =>
      implicit request =>

        val portalHref: String = buildPortalUrl("home")

        val accountSummaries = accountSummaryFactory.create(buildPortalUrl)
        Ok(businessTaxHomepage(portalHref, accountSummaries))

  })

  def makeAPaymentLanding = WithSessionTimeoutValidation(AuthorisedForGovernmentGatewayAction() {
    implicit user =>
      request =>
        Ok(makeAPaymentLandingPage())
  })
}
