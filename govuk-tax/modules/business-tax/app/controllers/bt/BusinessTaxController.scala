package controllers.bt

import org.joda.time.DateTime
import uk.gov.hmrc.microservice.auth.domain._
import uk.gov.hmrc.microservice.domain._
import controllers.common._
import uk.gov.hmrc.common.PortalDestinationUrlBuilder
import config.PortalConfig

class BusinessTaxController extends BaseController with ActionWrappers with SessionTimeoutWrapper {

  def home = WithSessionTimeoutValidation(AuthorisedForGovernmentGatewayAction() {
    implicit user =>
      implicit request =>

        val userAuthority = user.userAuthority
        val encodedGovernmentGatewayToken = user.decryptedToken.get
        val businessUser = BusinessUser(user.regimes, userAuthority.utr, userAuthority.vrn, userAuthority.ctUtr, userAuthority.empRef, user.nameFromGovernmentGateway.getOrElse(""), userAuthority.previouslyLoggedInAt, encodedGovernmentGatewayToken)

        val buildPortalUrl = PortalDestinationUrlBuilder.build(request, user) _
        val portalHref = buildPortalUrl("home")

        val saRegime = buildSaAccountSummary(buildPortalUrl, "some data from the SA -> CESA Hod")
        val vatRegime = buildVatAccountSummary(buildPortalUrl, "some data from the VAT -> VMF Hod")

        val accountSummaries = AccountSummaries(Seq(saRegime, vatRegime))

        Ok(views.html.business_tax_home(businessUser, portalHref, accountSummaries))

  })

  def buildSaAccountSummary(buildPortalUrl: String => String, data: String): AccountSummary = {
    val links = Seq(Link(buildPortalUrl("saViewAccountDetails"), "PORTAL: Sa View Account Details", Some(PortalConfig.ssoUrl)))
    AccountSummary("SA", Seq(data), links)
  }

  def buildVatAccountSummary(buildPortalUrl: String => String, data: String): AccountSummary = {
    val links = Seq(Link(buildPortalUrl("home"), "Take me to the Portal Home", Some(PortalConfig.ssoUrl)))
    AccountSummary("VAT", Seq(data), links)
  }

}

case class BusinessUser(regimeRoots: RegimeRoots, utr: Option[Utr], vrn: Option[Vrn], ctUtr: Option[Utr], empRef: Option[EmpRef], name: String, previouslyLoggedInAt: Option[DateTime], encodedGovernmentGatewayToken: String)

case class AccountSummaries(regimes: Seq[AccountSummary])
case class AccountSummary(regimeName: String, messages: Seq[String], links: Seq[Link])
case class Link(href: String, text: String, ssoUrl: Option[String])
