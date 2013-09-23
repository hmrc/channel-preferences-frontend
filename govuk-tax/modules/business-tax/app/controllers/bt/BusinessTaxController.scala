package controllers.bt

import org.joda.time.DateTime
import uk.gov.hmrc.common.microservice.domain._
import controllers.common._
import uk.gov.hmrc.common.PortalDestinationUrlBuilder
import views.helpers.RenderableMessage
import views.html.make_a_payment_landing
import controllers.bt.regimeViews.{SaAccountSummaryViewBuilder, VatAccountSummaryViewBuilder}
import uk.gov.hmrc.common.microservice.sa.SaMicroService
import uk.gov.hmrc.common.microservice.vat.VatMicroService
import uk.gov.hmrc.domain.{SaUtr, EmpRef, Vrn, CtUtr}

class BusinessTaxController(accountSummaryFactory : AccountSummariesFactory) extends BaseController with ActionWrappers with SessionTimeoutWrapper {

  def this() = {
    this(new AccountSummariesFactory(new SaMicroService(), new VatMicroService()))
  }

  def home = WithSessionTimeoutValidation(AuthorisedForGovernmentGatewayAction() {
    implicit user =>
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
                        encodedGovernmentGatewayToken: String) //extends LoggedInUser

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

abstract class LoggedInUser(implicit user: User) {
  val hasBusinessTaxRegime: Boolean = user.regimes.hasBusinessTaxRegime
}


case class AccountSummaries(regimes: Seq[AccountSummary])

class AccountSummariesFactory(saMicroService : SaMicroService, vatMicroService : VatMicroService){

  def create(buildPortalUrl  : (String) => String)(implicit user : User) : AccountSummaries = {
    val saRegime = SaAccountSummaryViewBuilder(buildPortalUrl, user, saMicroService).build()
    val vatRegime = VatAccountSummaryViewBuilder(buildPortalUrl, user, vatMicroService).build()
    new AccountSummaries(Seq(saRegime, vatRegime).flatten)
  }
}

case class AccountSummary(regimeName: String, messages: Seq[(String, Seq[RenderableMessage])], addenda: Seq[RenderableMessage])
