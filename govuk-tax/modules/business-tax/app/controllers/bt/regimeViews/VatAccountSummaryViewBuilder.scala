package controllers.bt.regimeViews

import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.{VatAccountSummary, VatRoot}
import views.helpers.{StringMessage, LinkMessage}
import controllers.bt.{routes, AccountSummary}
import uk.gov.hmrc.common.microservice.vat.VatMicroService
import uk.gov.hmrc.microservice.domain.User

case class VatAccountSummaryViewBuilder(buildPortalUrl: String => String, user: User, vatMicroService: VatMicroService) {
  implicit def translate(value: String): StringMessage = StringMessage(value)

  def build: Option[AccountSummary] = {
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
        val links = Seq(LinkMessage(buildPortalUrl("vatAccountDetails"), "vat.accountSummary.linkText.accountDetails"),
          LinkMessage(makeAPaymentUri, "vat.accountSummary.linkText.makeAPayment"), LinkMessage(buildPortalUrl("vatFileAReturn"), "vat.accountSummary.linkText.fileAReturn"))

        accountValueOption match {
          case Some(accountValue) => {
            AccountSummary("VAT", Seq("vat.message.0" -> List(user.userAuthority.vrn.get.vrn),
              "vat.message.1" -> List(accountValue.toString())), links)
          }
          case _ => {
            AccountSummary("VAT", Seq("vat.error.message.summaryUnavailable.1" -> List.empty, "vat.error.message.summaryUnavailable.2" -> List.empty,
              "vat.error.message.summaryUnavailable.3" -> List.empty,
              "vat.error.message.summaryUnavailable.4" -> List(LinkMessage("/TODO/HelpDeskLink", "vat.accountSummary.linkText.helpDesk"))), Seq.empty)
          }
        }
    }
  }
}

