package controllers.bt.regimeViews

import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.{VatAccountSummary, VatRoot}
import views.helpers.{MoneyPounds, RenderableMessage, LinkMessage}
import controllers.bt.{routes, AccountSummary}
import uk.gov.hmrc.common.microservice.vat.VatMicroService
import uk.gov.hmrc.common.microservice.domain.User

case class VatAccountSummaryViewBuilder(buildPortalUrl: String => String, user: User, vatMicroService: VatMicroService) {

  import VatMessageKeys._
  import VatPortalUrls._

  def build(): Option[AccountSummary] = {
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
          LinkMessage(buildPortalUrl(accountDetailsPortalUrl), viewAccountDetailsLinkMessage),
          LinkMessage(makeAPaymentUri, makeAPaymentLinkMessage),
          LinkMessage(buildPortalUrl(fileAReturnPortalUrl), fileAReturnLinkMessage)
        )

        accountValueOption match {
          case Some(accountValue) => {
            AccountSummary(regimeNameMessage, Seq(vatRegistrationNumberMessage -> Seq(user.userAuthority.vrn.get.vrn),
              accountBalanceMessage -> Seq(MoneyPounds(accountValue))), links)
          }
          case _ => {
            AccountSummary(regimeNameMessage, Seq(summaryUnavailableErrorMessage1 -> Seq.empty, summaryUnavailableErrorMessage2 -> Seq.empty,
              summaryUnavailableErrorMessage3 -> Seq.empty,
              summaryUnavailableErrorMessage4 -> Seq(LinkMessage(helpDeskPortalUrl, helpDeskLinkMessage))), Seq.empty)
          }
        }
    }
  }
}

object VatPortalUrls {
  val accountDetailsPortalUrl = "vatAccountDetails"
  val fileAReturnPortalUrl = "vatFileAReturn"
  val helpDeskPortalUrl = "vatHelpDesk" // TODO [JJJS] WHAT'S THE CORRECT HELP DESK LINK - was set to "/TODO/HelpDeskLink"
}

object VatMessageKeys extends CommonBusinessMessageKeys {

  val regimeNameMessage = "vat.regimeName"

  val vatRegistrationNumberMessage = "vat.registrationNumber.message"
  val accountBalanceMessage = "vat.accountBalance.message"
  val helpDeskLinkMessage = "vat.accountSummary.linkText.helpDesk"

  val summaryUnavailableErrorMessage1 = "vat.error.message.summaryUnavailable.1"
  val summaryUnavailableErrorMessage2 = "vat.error.message.summaryUnavailable.2"
  val summaryUnavailableErrorMessage3 = "vat.error.message.summaryUnavailable.3"
  val summaryUnavailableErrorMessage4 = "vat.error.message.summaryUnavailable.4"
}
