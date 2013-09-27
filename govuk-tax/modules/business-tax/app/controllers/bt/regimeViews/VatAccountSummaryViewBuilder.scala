package controllers.bt.regimeViews

import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.{VatAccountSummary, VatRoot}
import views.helpers.{MoneyPounds, RenderableMessage, LinkMessage}
import controllers.bt.{routes, AccountSummary}
import uk.gov.hmrc.common.microservice.vat.VatMicroService
import uk.gov.hmrc.common.microservice.domain.User

case class VatAccountSummaryViewBuilder(vatMicroService: VatMicroService) {

  import VatMessageKeys._
  import VatPortalUrls._

  def build(buildPortalUrl: String => String, user: User): Option[AccountSummary] = {
    val vatRootOption: Option[VatRoot] = user.regimes.vat

    vatRootOption.map {
      vatRoot: VatRoot =>
        val accountSummary: Option[VatAccountSummary] = vatRootOption.get.accountSummary(vatMicroService)


        val accountValueOption: Option[BigDecimal] = for {
          accountSummaryValue <- accountSummary
          accountBalance <- accountSummaryValue.accountBalance
          amount <- accountBalance.amount
        } yield amount

        val makeAPaymentUri = routes.BusinessTaxController.makeAPaymentLanding().url
        val links = Seq[RenderableMessage](
          LinkMessage(buildPortalUrl(vatAccountDetailsPortalUrl), viewAccountDetailsLinkMessage),
          LinkMessage(makeAPaymentUri, makeAPaymentLinkMessage),
          LinkMessage(buildPortalUrl(vatFileAReturnPortalUrl), fileAReturnLinkMessage)
        )

        accountValueOption match {
          case Some(accountValue) => {
            AccountSummary(vatRegimeNameMessage, Seq(vatRegistrationNumberMessage -> Seq(user.userAuthority.vrn.get.vrn),
              vatAccountBalanceMessage -> Seq(MoneyPounds(accountValue))), links)
          }
          case _ => {
            AccountSummary(vatRegimeNameMessage, Seq(vatSummaryUnavailableErrorMessage1 -> Seq.empty, vatSummaryUnavailableErrorMessage2 -> Seq.empty,
              vatSummaryUnavailableErrorMessage3 -> Seq.empty,
              vatSummaryUnavailableErrorMessage4 -> Seq(LinkMessage(vatHelpDeskPortalUrl, vatHelpDeskLinkMessage))), Seq.empty)
          }
        }
    }
  }
}

object VatPortalUrls {
  val vatAccountDetailsPortalUrl = "vatAccountDetails"
  val vatFileAReturnPortalUrl = "vatFileAReturn"
  val vatHelpDeskPortalUrl = "vatHelpDesk" // TODO [JJJS] WHAT'S THE CORRECT HELP DESK LINK - was set to "/TODO/HelpDeskLink"
}

object VatMessageKeys extends CommonBusinessMessageKeys {

  val vatRegimeNameMessage = "vat.regimeName"

  val vatRegistrationNumberMessage = "vat.message.registrationNumber"
  val vatAccountBalanceMessage = "vat.message.accountBalance"
  val vatHelpDeskLinkMessage = "vat.link.message.accountSummary.helpDesk"

  val vatSummaryUnavailableErrorMessage1 = "vat.message.summaryUnavailable.1"
  val vatSummaryUnavailableErrorMessage2 = "vat.message.summaryUnavailable.2"
  val vatSummaryUnavailableErrorMessage3 = "vat.message.summaryUnavailable.3"
  val vatSummaryUnavailableErrorMessage4 = "vat.message.summaryUnavailable.4"
}
