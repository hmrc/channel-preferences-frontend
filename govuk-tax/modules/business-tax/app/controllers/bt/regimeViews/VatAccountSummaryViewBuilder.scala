package controllers.bt.regimeViews

import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.{VatAccountSummary, VatRoot}
import uk.gov.hmrc.microservice.auth.domain.Vrn
import views.helpers.{StringMessage, LinkMessage}
import controllers.bt.AccountSummary
import uk.gov.hmrc.common.microservice.vat.VatMicroService

case class VatAccountSummaryViewBuilder(buildPortalUrl: String => String, vatRootOption: Option[VatRoot], vrn: Option[Vrn], vatMicroService: VatMicroService) {
  implicit def translate(value: String): StringMessage = StringMessage(value)

  def build: Option[AccountSummary] = {
    vatRootOption.map {
      vatRoot: VatRoot =>
        val accountSummary: Option[VatAccountSummary] = vatRootOption.get.accountSummary(vatMicroService)
        val accountValueOption: Option[BigDecimal] = for {
          accountSummaryValue <- accountSummary
          accountBalance <- accountSummaryValue.accountBalance
          amount <- accountBalance.amount
        } yield amount

        val links = Seq(LinkMessage(buildPortalUrl("vatAccountDetails"), "View account details"))

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

