package controllers.bt.regimeViews

import views.helpers.{MoneyPounds, RenderableMessage, LinkMessage}
import controllers.bt.routes
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.utils.DateConverter
import uk.gov.hmrc.common.microservice.ct.CtConnector
import uk.gov.hmrc.common.microservice.ct.domain.CtDomain.{CtAccountSummary, CtRoot}
import uk.gov.hmrc.common.microservice.ct.domain.CtDomain


case class CtAccountSummaryBuilder(ctConnector: CtConnector) extends AccountSummaryTemplate[CtRoot]{

  import CtMessageKeys._
  import CtPortalUrlKeys._

  def buildAccountSummary(ctRoot : CtRoot, buildPortalUrl: (String) => String) : AccountSummary = {

        val accountSummary: Option[CtAccountSummary] = ctRoot.accountSummary(ctConnector)
        val accountValueOption: Option[BigDecimal] = accountValueIfPresent(accountSummary)
        val dateOfBalanceOption: Option[String] = accountSummary flatMap (_.dateOfBalance)



        (accountValueOption, dateOfBalanceOption) match {

          case (Some(accountValue), Some(dateOfBalance)) => {
            accountSummaryWithDetails(buildPortalUrl, ctRoot, accountValue, dateOfBalance)
          }
          case _ => {
            emptyAccountSummary(ctRoot)
          }
    }
  }

  def rootForRegime(user : User): Option[CtRoot]= user.regimes.ct

  private def emptyAccountSummary(ctRoot : CtRoot): AccountSummary = {
    val messages: Seq[(String, Seq[RenderableMessage])] = Seq(ctUtrMessage -> Seq(ctRoot.identifier.utr),
      ctSummaryUnavailableErrorMessage1 -> Seq.empty,
      ctSummaryUnavailableErrorMessage2 -> Seq.empty,
      ctSummaryUnavailableErrorMessage3 -> Seq.empty,
      ctSummaryUnavailableErrorMessage4 -> Seq.empty)
    AccountSummary(ctRegimeNameMessage, messages, Seq.empty)
  }

  private def accountSummaryWithDetails(buildPortalUrl: (String) => String, ctRoot : CtRoot, accountValue: BigDecimal, dateOfBalance: String): AccountSummary = {
    val makeAPaymentUri = routes.BusinessTaxController.makeAPaymentLanding().url
    val links = Seq[RenderableMessage](
      LinkMessage(buildPortalUrl(ctAccountDetailsPortalUrl), viewAccountDetailsLinkMessage),
      LinkMessage(makeAPaymentUri, makeAPaymentLinkMessage),
      LinkMessage(buildPortalUrl(ctFileAReturnPortalUrl), fileAReturnLinkMessage)
    )
    val messages: Seq[(String, Seq[RenderableMessage])] = Seq(ctUtrMessage -> Seq(ctRoot.identifier.utr),
      ctAmountAsOfDateMessage -> Seq(MoneyPounds(accountValue), DateConverter.parseToLocalDate(dateOfBalance)))
    AccountSummary(ctRegimeNameMessage, messages, links)
  }

  private def accountValueIfPresent(accountSummary: Option[CtDomain.CtAccountSummary]): Option[BigDecimal] = {
    val accountValueOption: Option[BigDecimal] = for {
      accountSummaryValue <- accountSummary
      accountBalance <- accountSummaryValue.accountBalance
      amount <- accountBalance.amount
    } yield amount
    accountValueOption
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
