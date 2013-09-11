package controllers.bt

import org.joda.time.DateTime
import uk.gov.hmrc.microservice.auth.domain._
import uk.gov.hmrc.microservice.domain._
import controllers.common._
import uk.gov.hmrc.common.PortalDestinationUrlBuilder
import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.{ VatAccountSummary, VatRoot }
import views.helpers.{ StringMessage, LinkMessage, StringOrLinkMessage }

class BusinessTaxController extends BaseController with ActionWrappers with SessionTimeoutWrapper {
  implicit def translate(value: String): StringMessage = StringMessage(value)

  def home = WithSessionTimeoutValidation(AuthorisedForGovernmentGatewayAction() {
    implicit user =>
      implicit request =>

        val userAuthority = user.userAuthority
        val encodedGovernmentGatewayToken = user.decryptedToken.get
        val businessUser = BusinessUser(user.regimes, userAuthority.utr, userAuthority.vrn, userAuthority.ctUtr, userAuthority.empRef, user.nameFromGovernmentGateway.getOrElse(""), userAuthority.previouslyLoggedInAt, encodedGovernmentGatewayToken)

        val buildPortalUrl = PortalDestinationUrlBuilder.build(request, user) _
        val portalHref = buildPortalUrl("home")

        val saRegime = buildSaAccountSummary(buildPortalUrl, "some data from the SA -> CESA Hod")
        val vatRegime = buildVatAccountSummary(buildPortalUrl, user.regimes.vat, userAuthority.vrn)

        val accountSummaries = AccountSummaries(Seq(saRegime, vatRegime).flatten)

        Ok(views.html.business_tax_home(businessUser, portalHref, accountSummaries))

  })

  def buildSaAccountSummary(buildPortalUrl: String => String, data: String): Option[AccountSummary] = {
    val links = Seq(LinkMessage(buildPortalUrl("saViewAccountDetails"), "PORTAL: Sa View Account Details"))
    Some(AccountSummary("SA", Seq.empty, links))
  }

  def buildVatAccountSummary(buildPortalUrl: String => String, vatRootOption: Option[VatRoot], vrn: Option[Vrn]): Option[AccountSummary] = {
    vatRootOption.map {
      vatRoot: VatRoot =>
        val accountSummary: Option[VatAccountSummary] = vatRootOption.get.accountSummary(vatMicroService)
        val accountValueOption: Option[BigDecimal] = for {
          accountSummaryValue <- accountSummary
          accountBalance <- accountSummaryValue.accountBalance
          amount <- accountBalance.amount
        } yield amount

        val links = Seq(LinkMessage(buildPortalUrl("vatAccountDetails"), "View your account on HMRC Portal"))

        accountValueOption match {
          case Some(accountValue) => {
            AccountSummary("VAT", Seq("vat.message.0" -> List(vrn.get.vrn),
              "vat.message.1" -> List(accountValue.toString())), links)
          }
          case None => {
            AccountSummary("VAT", Seq("vat.error.message.1" -> List.empty), Seq.empty)
          }
        }
    }
  }

}

case class BusinessUser(regimeRoots: RegimeRoots, utr: Option[Utr], vrn: Option[Vrn], ctUtr: Option[Utr], empRef: Option[EmpRef], name: String, previouslyLoggedInAt: Option[DateTime], encodedGovernmentGatewayToken: String)

case class AccountSummaries(regimes: Seq[AccountSummary])

case class AccountSummary(regimeName: String, messages: Seq[(String, List[StringOrLinkMessage])], links: Seq[LinkMessage])
