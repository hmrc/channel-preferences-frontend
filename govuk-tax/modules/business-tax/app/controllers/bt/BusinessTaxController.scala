package controllers.bt

import controllers.common._
import uk.gov.hmrc.common.PortalDestinationUrlBuilder
import controllers.bt.regimeViews._
import uk.gov.hmrc.common.microservice.sa.SaConnector
import uk.gov.hmrc.common.microservice.vat.VatConnector
import uk.gov.hmrc.common.microservice.epaye.EpayeConnector
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.ct.CtConnector

class BusinessTaxController(accountSummaryFactory: AccountSummariesFactory)
  extends BaseController
  with ActionWrappers
  with PortalDestinationUrlBuilder {

  def home = AuthorisedForGovernmentGatewayAction() {
    implicit user =>
      implicit request =>
        Ok(
          businessTaxHomepage(
            portalHref = buildPortalUrl("home"),
            accountSummaries = accountSummaryFactory.create(buildPortalUrl)
          )
        )
  }

  def makeAPaymentLanding = AuthorisedForGovernmentGatewayAction() {
    implicit user =>
      implicit request =>
        Ok(makeAPaymentLandingPage())
  }

  private[bt] def makeAPaymentLandingPage()(implicit user: User) =
    views.html.make_a_payment_landing()

  private[bt] def businessTaxHomepage(portalHref: String, accountSummaries: AccountSummaries)(implicit user: User) =
    views.html.business_tax_home(portalHref = portalHref, accountSummaries = accountSummaries)

  def this() = this(new AccountSummariesFactory(new SaConnector, new VatConnector, new CtConnector, new EpayeConnector)())
}
