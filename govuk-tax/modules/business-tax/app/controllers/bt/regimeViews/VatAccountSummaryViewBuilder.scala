package controllers.bt.regimeViews

import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.{VatAccountSummary, VatRoot}
import views.helpers.{MoneyPounds, RenderableMessage, LinkMessage}
import controllers.bt.{routes, AccountSummary}
import uk.gov.hmrc.common.microservice.vat.VatMicroService
import uk.gov.hmrc.microservice.domain.User

case class VatAccountSummaryViewBuilder(buildPortalUrl: String => String, user: User, vatMicroService: VatMicroService) {

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
          LinkMessage(buildPortalUrl("vatAccountDetails"), "vat.accountSummary.linkText.accountDetails"),
          LinkMessage(makeAPaymentUri, "vat.accountSummary.linkText.makeAPayment"),
          LinkMessage(buildPortalUrl("vatFileAReturn"), "vat.accountSummary.linkText.fileAReturn")
        )

        accountValueOption match {
          case Some(accountValue) => {
            AccountSummary("VAT", Seq("vat.message.0" -> Seq(user.userAuthority.vrn.get.vrn),
              "vat.message.1" -> Seq(MoneyPounds(accountValue))), links)
          }
          case _ => {
            AccountSummary("VAT", Seq("vat.error.message.summaryUnavailable.1" -> Seq.empty, "vat.error.message.summaryUnavailable.2" -> Seq.empty,
              "vat.error.message.summaryUnavailable.3" -> Seq.empty,
              "vat.error.message.summaryUnavailable.4" -> Seq(LinkMessage("/TODO/HelpDeskLink", "vat.accountSummary.linkText.helpDesk"))), Seq.empty)
          }
        }
    }
  }
}

