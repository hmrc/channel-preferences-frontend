package controllers.bt

import org.joda.time.DateTime
import uk.gov.hmrc.common.microservice.auth.domain._
import uk.gov.hmrc.common.microservice.domain._
import controllers.common._
import uk.gov.hmrc.common.PortalDestinationUrlBuilder
import views.helpers.RenderableMessage
import views.html.make_a_payment_landing
import controllers.bt.regimeViews.{SaAccountSummaryViewBuilder, VatAccountSummaryViewBuilder}
import uk.gov.hmrc.common.microservice.sa.SaMicroService
import uk.gov.hmrc.common.microservice.vat.VatMicroService
import controllers.common.service.MicroServices

class BusinessTaxController(accountSummaryFactory : AccountSummariesFactory) extends BaseController with ActionWrappers with SessionTimeoutWrapper {

  def this() = {
    this(new AccountSummariesFactory(new SaMicroService(), new VatMicroService()))
  }

  def home = WithSessionTimeoutValidation(AuthorisedForGovernmentGatewayAction() {
    implicit user =>
      request =>

        val buildPortalUrl = PortalDestinationUrlBuilder.build(request, user) _
        val portalHref = buildPortalUrl("home")

        val accountSummaries = accountSummaryFactory.create(buildPortalUrl)

        Ok(views.html.business_tax_home(BusinessUser(user), portalHref, accountSummaries))

  })

  def makeAPaymentLanding = WithSessionTimeoutValidation(AuthorisedForGovernmentGatewayAction() {
    user => request => makeAPaymentLandingAction()
  })

  private[bt] def makeAPaymentLandingAction() = {
    Ok(make_a_payment_landing())
  }


}

case class BusinessUser(regimeRoots: RegimeRoots, utr: Option[Utr], vrn: Option[Vrn], ctUtr: Option[Utr], empRef: Option[EmpRef], name: String, previouslyLoggedInAt: Option[DateTime], encodedGovernmentGatewayToken: String)

private object BusinessUser {
  def apply(user : User) : BusinessUser = {
    val userAuthority = user.userAuthority
    new BusinessUser(user.regimes,
                      userAuthority.utr,
                      userAuthority.vrn,
                      userAuthority.ctUtr,
                      userAuthority.empRef,
                      user.nameFromGovernmentGateway.getOrElse(""),
                      userAuthority.previouslyLoggedInAt,
                      user.decryptedToken.get)
  }
}

case class AccountSummaries(regimes: Seq[AccountSummary])

class AccountSummariesFactory(saMicroService : SaMicroService, vatMicroService : VatMicroService = new VatMicroService()){

  def create(buildPortalUrl  : (String) => String)(implicit user : User) : AccountSummaries = {
    val saRegime = SaAccountSummaryViewBuilder(buildPortalUrl, user, saMicroService).build()
    val vatRegime = VatAccountSummaryViewBuilder(buildPortalUrl, user, vatMicroService).build()
    new AccountSummaries(Seq(saRegime, vatRegime).flatten)
  }
}

case class AccountSummary(regimeName: String, messages: Seq[(String, Seq[RenderableMessage])], addenda: Seq[RenderableMessage])
