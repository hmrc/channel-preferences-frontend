package controllers.bt

import org.joda.time.DateTime
import controllers.common._
import uk.gov.hmrc.common.PortalDestinationUrlBuilder
import controllers.bt.regimeViews._
import uk.gov.hmrc.common.microservice.sa.SaConnector
import uk.gov.hmrc.common.microservice.vat.VatConnector
import uk.gov.hmrc.common.microservice.epaye.EPayeConnector
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.common.microservice.ct.CtConnector

class BusinessTaxController(accountSummaryFactory : AccountSummariesFactory) extends BaseController with ActionWrappers with SessionTimeoutWrapper with PortalDestinationUrlBuilder {

  private[bt] def makeAPaymentLandingPage()(implicit user: User) = views.html.make_a_payment_landing()
  private[bt] def businessTaxHomepage(businessUser: BusinessUser, portalHref: String, accountSummaries: AccountSummaries)(implicit user: User) = views.html.business_tax_home(businessUser, portalHref, accountSummaries)

  def this() = {
    this(new AccountSummariesFactory(new SaConnector, new VatConnector, new CtConnector, new EPayeConnector))
  }

  def home = WithSessionTimeoutValidation(AuthorisedForGovernmentGatewayAction() {
    implicit user: User =>
      implicit request =>

        val portalHref: String = buildPortalUrl("home")

        val accountSummaries = accountSummaryFactory.create(buildPortalUrl)
        Ok(businessTaxHomepage(BusinessUser(), portalHref, accountSummaries))

  })

  def makeAPaymentLanding = WithSessionTimeoutValidation(AuthorisedForGovernmentGatewayAction() {
    implicit user =>
      request =>
        Ok(makeAPaymentLandingPage())
  })
}

case class BusinessUser(regimeRoots: RegimeRoots, saUtr: Option[SaUtr], vrn: Option[Vrn],
                        ctUtr: Option[CtUtr], empRef: Option[EmpRef], name: String, previouslyLoggedInAt: Option[DateTime],
                        encodedGovernmentGatewayToken: String)

private object BusinessUser {
  def apply()(implicit user : User) : BusinessUser = {
    val userAuthority = user.userAuthority
    new BusinessUser(user.regimes,
                      userAuthority.saUtr,
                      userAuthority.vrn,
                      userAuthority.ctUtr,
                      userAuthority.empRef,
                      user.nameFromGovernmentGateway.getOrElse(""),
                      userAuthority.previouslyLoggedInAt,
                      user.decryptedToken.get) {
    }
  }
}
