package controllers.bt.regimeViews

import ct.CtMicroService
import ct.domain.CtDomain.{CtAccountSummary, CtRoot}
import views.helpers.{MoneyPounds, RenderableMessage, LinkMessage}
import controllers.bt.{routes, AccountSummary}
import uk.gov.hmrc.common.microservice.domain.User

case class CtAccountSummaryViewBuilder(buildPortalUrl: String => String, user: User, ctMicroService: CtMicroService) {

  def build(): Option[AccountSummary] = {
    val ctRootOption: Option[CtRoot] = user.regimes.ct

    ctRootOption.map {
      ctRoot: CtRoot =>
        val accountSummary: Option[CtAccountSummary] = ctRootOption.get.accountSummary(ctMicroService)


        val accountValueOption: Option[BigDecimal] = for {
          accountSummaryValue <- accountSummary
          accountBalance <- accountSummaryValue.accountBalance
          amount <- accountBalance.amount
        } yield amount

        val makeAPaymentUri = routes.BusinessTaxController.makeAPaymentLanding().url
        val links = Seq[RenderableMessage](
          LinkMessage(buildPortalUrl("ctAccountDetails"), "common.accountSummary.message.link.viewAccountDetails"),
          LinkMessage(makeAPaymentUri, "common.accountSummary.message.link.makeAPayment"),
          LinkMessage(buildPortalUrl("ctFileAReturn"), "common.accountSummary.message.link.fileAReturn")



        )
        accountValueOption match {
          case Some(accountValue) => {
            AccountSummary("Corporation Tax", Seq("ct.message.0" -> Seq(user.userAuthority.ctUtr.get.utr),
              "ct.message.1" -> Seq(MoneyPounds(accountValue))), links)
          }
          case _ => {
            AccountSummary("Corporation Tax", Seq("ct.error.message.summaryUnavailable.1" -> Seq.empty, "ct.error.message.summaryUnavailable.2" -> Seq.empty,
              "ct.error.message.summaryUnavailable.3" -> Seq.empty,
              "ct.error.message.summaryUnavailable.4" -> Seq.empty), Seq.empty)
          }
        }
    }
  }
}

