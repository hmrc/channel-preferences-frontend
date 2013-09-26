package controllers.bt.regimeViews

import ct.CtMicroService
import ct.domain.CtDomain.{CtAccountSummary, CtRoot}
import views.helpers.{MoneyPounds, RenderableMessage, LinkMessage}
import controllers.bt.{AccountSummary, routes}
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.utils.DateConverter

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
      val dateOfBalanceOption:Option[String] = accountSummary flatMap (_.dateOfBalance)

        val makeAPaymentUri = routes.BusinessTaxController.makeAPaymentLanding().url
        val links = Seq[RenderableMessage](
          LinkMessage(buildPortalUrl("ctAccountDetails"), "common.accountSummary.message.link.viewAccountDetails"),
          LinkMessage(makeAPaymentUri, "common.accountSummary.message.link.makeAPayment"),
          LinkMessage(buildPortalUrl("ctFileAReturn"), "common.accountSummary.message.link.fileAReturn")



        )
        (accountValueOption, dateOfBalanceOption)  match {
          case (Some(accountValue), Some(dateOfBalance)) => {
            AccountSummary("Corporation Tax", Seq("ct.message.0" -> Seq(user.userAuthority.ctUtr.get.utr),
              "ct.message.1" -> Seq(MoneyPounds(accountValue), DateConverter.parseToLocalDate(dateOfBalance))), links)
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

