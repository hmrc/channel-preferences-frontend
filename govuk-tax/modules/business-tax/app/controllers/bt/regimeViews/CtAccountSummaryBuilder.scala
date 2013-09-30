package controllers.bt.regimeViews

import views.helpers.{MoneyPounds, RenderableMessage, LinkMessage}
import controllers.bt.routes
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.utils.DateConverter
import uk.gov.hmrc.common.microservice.ct.CtConnector
import uk.gov.hmrc.common.microservice.ct.domain.CtDomain.{CtAccountSummary, CtRoot}


case class CtAccountSummaryBuilder(ctConnector: CtConnector) {

  import CtMessageKeys._
  import CtPortalUrlKeys._

  def build(buildPortalUrl: String => String, user: User): Option[AccountSummary] = {
    val ctRootOption: Option[CtRoot] = user.regimes.ct

    ctRootOption.map {
      ctRoot: CtRoot =>
        val accountSummary: Option[CtAccountSummary] = ctRootOption.get.accountSummary(ctConnector)


        val accountValueOption: Option[BigDecimal] = for {
          accountSummaryValue <- accountSummary
          accountBalance <- accountSummaryValue.accountBalance
          amount <- accountBalance.amount
        } yield amount
      val dateOfBalanceOption:Option[String] = accountSummary flatMap (_.dateOfBalance)

        val makeAPaymentUri = routes.BusinessTaxController.makeAPaymentLanding().url
        val links = Seq[RenderableMessage](
          LinkMessage(buildPortalUrl(ctAccountDetailsPortalUrl), viewAccountDetailsLinkMessage),
          LinkMessage(makeAPaymentUri, makeAPaymentLinkMessage),
          LinkMessage(buildPortalUrl(ctFileAReturnPortalUrl), fileAReturnLinkMessage)



        )
        (accountValueOption, dateOfBalanceOption)  match {
          case (Some(accountValue), Some(dateOfBalance)) => {
            AccountSummary(ctRegimeNameMessage, Seq(ctUtrMessage -> Seq(user.userAuthority.ctUtr.get.utr),
              ctAmountAsOfDateMessage -> Seq(MoneyPounds(accountValue), DateConverter.parseToLocalDate(dateOfBalance))), links)
          }
          case _ => {
            AccountSummary(ctRegimeNameMessage, Seq(ctUtrMessage -> Seq(user.userAuthority.ctUtr.get.utr),
              ctSummaryUnavailableErrorMessage1 -> Seq.empty,
              ctSummaryUnavailableErrorMessage2 -> Seq.empty,
              ctSummaryUnavailableErrorMessage3 -> Seq.empty,
              ctSummaryUnavailableErrorMessage4 -> Seq.empty), Seq.empty)
          }
        }
    }
  }
}

object CtPortalUrlKeys {
  val ctAccountDetailsPortalUrl = "ctAccountDetails"
  val ctFileAReturnPortalUrl = "ctFileAReturn"
}

object CtMessageKeys extends CommonBusinessMessageKeys {

  val ctRegimeNameMessage = "ct.regimeName"

  val ctUtrMessage = "ct.message.utr"
  val ctSummaryUnavailableErrorMessage1 = "ct.message.summaryUnavailable.1"
  val ctSummaryUnavailableErrorMessage2 = "ct.message.summaryUnavailable.2"
  val ctSummaryUnavailableErrorMessage3 = "ct.message.summaryUnavailable.3"
  val ctSummaryUnavailableErrorMessage4 = "ct.message.summaryUnavailable.4"
  val ctAmountAsOfDateMessage = "ct.message.amountAsOfDate"
}
