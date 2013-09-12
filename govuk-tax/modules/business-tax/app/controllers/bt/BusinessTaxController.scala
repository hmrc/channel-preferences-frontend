package controllers.bt

import org.joda.time.DateTime
import uk.gov.hmrc.microservice.auth.domain._
import uk.gov.hmrc.microservice.domain._
import controllers.common._
import uk.gov.hmrc.common.PortalDestinationUrlBuilder
import views.helpers.{ LinkMessage, StringOrLinkMessage }
import controllers.bt.regimeViews.{SaAccountSummaryViewBuilder, VatAccountSummaryViewBuilder}
import views.html.make_a_payment_landing

class BusinessTaxController extends BaseController with ActionWrappers with SessionTimeoutWrapper {

  def home = WithSessionTimeoutValidation(AuthorisedForGovernmentGatewayAction() {
    implicit user =>
      implicit request =>

        val userAuthority = user.userAuthority
        val encodedGovernmentGatewayToken = user.decryptedToken.get
        val businessUser = BusinessUser(user.regimes, userAuthority.utr, userAuthority.vrn, userAuthority.ctUtr, userAuthority.empRef, user.nameFromGovernmentGateway.getOrElse(""), userAuthority.previouslyLoggedInAt, encodedGovernmentGatewayToken)

        val buildPortalUrl = PortalDestinationUrlBuilder.build(request, user) _
        val portalHref = buildPortalUrl("home")

        val saRegime = SaAccountSummaryViewBuilder(buildPortalUrl, "some data from the SA -> CESA Hod", saMicroService).build
        val vatRegime = VatAccountSummaryViewBuilder(buildPortalUrl, user, vatMicroService).build
        val accountSummaries = AccountSummaries(Seq(saRegime, vatRegime).flatten)

        Ok(views.html.business_tax_home(businessUser, portalHref, accountSummaries))

  })

  def makeAPaymentLanding = WithSessionTimeoutValidation(AuthorisedForGovernmentGatewayAction() { user => request => makeAPaymentLandingAction() })

  private[bt] def makeAPaymentLandingAction() = {
    Ok(make_a_payment_landing())
  }


}

case class BusinessUser(regimeRoots: RegimeRoots, utr: Option[Utr], vrn: Option[Vrn], ctUtr: Option[Utr], empRef: Option[EmpRef], name: String, previouslyLoggedInAt: Option[DateTime], encodedGovernmentGatewayToken: String)

case class AccountSummaries(regimes: Seq[AccountSummary])

case class AccountSummary(regimeName: String, messages: Seq[(String, List[StringOrLinkMessage])], links: Seq[LinkMessage])
