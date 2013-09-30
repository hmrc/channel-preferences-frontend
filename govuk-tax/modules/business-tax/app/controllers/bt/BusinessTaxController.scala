package controllers.bt

import ct.CtConnector
import org.joda.time.DateTime
import controllers.common._
import uk.gov.hmrc.common.PortalDestinationUrlBuilder
import views.html.make_a_payment_landing
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

class BusinessTaxController(accountSummaryFactory : AccountSummariesFactory) extends BaseController with ActionWrappers with SessionTimeoutWrapper {

  def this() = {
    this(new AccountSummariesFactory(new SaConnector, new VatConnector, new CtConnector, new EPayeConnector))
  }

  def home = WithSessionTimeoutValidation(AuthorisedForGovernmentGatewayAction() {
    implicit user: User =>
      request =>

        //TODO: leverage implicit user and request so this function is just available rather than an explicit call with params
        val buildPortalUrl = PortalDestinationUrlBuilder.build(request, user) _
        val portalHref = buildPortalUrl("home")

        val accountSummaries = accountSummaryFactory.create(buildPortalUrl)
        Ok(views.html.business_tax_home(BusinessUser(), portalHref, accountSummaries))

  })

  def makeAPaymentLanding = WithSessionTimeoutValidation(AuthorisedForGovernmentGatewayAction() {
    user => request => makeAPaymentLandingAction()
  })

  private[bt] def makeAPaymentLandingAction() = {
    Ok(make_a_payment_landing())
  }


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



