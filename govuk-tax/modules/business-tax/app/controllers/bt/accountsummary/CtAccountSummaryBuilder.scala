package controllers.bt.accountsummary

import views.helpers.MoneyPounds
import controllers.bt.routes
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.utils.DateConverter
import uk.gov.hmrc.common.microservice.ct.CtConnector
import uk.gov.hmrc.common.microservice.ct.domain.{CtAccountSummary, CtRoot}
import uk.gov.hmrc.domain.CtUtr
import controllers.common.actions.HeaderCarrier
import scala.concurrent._
import uk.gov.hmrc.common.StickyMdcExecutionContext.global

case class CtAccountSummaryBuilder(ctConnector: CtConnector = new CtConnector) extends AccountSummaryBuilder[CtUtr, CtRoot] {

  import CommonBusinessMessageKeys._
  import CtMessageKeys._
  import CtPortalUrlKeys._

  override protected def buildAccountSummary(ctRoot: CtRoot, buildPortalUrl: (String) => String)(implicit headerCarrier: HeaderCarrier): Future[AccountSummary] = {
    val accountSummaryF = ctRoot.accountSummary(ctConnector, headerCarrier)

    accountSummaryF.map { accountSummary =>
      val accountValueOption: Option[BigDecimal] = accountValueIfPresent(accountSummary)
      val dateOfBalanceOption: Option[String] = accountSummary flatMap (_.dateOfBalance)

      (accountValueOption, dateOfBalanceOption) match {
        case (Some(accountValue), Some(dateOfBalance)) => {
          accountSummaryWithDetails(buildPortalUrl, ctRoot, accountValue, dateOfBalance)
        }
        case _ => defaultAccountSummary(ctRoot)
      }
    }
  }

  override protected def defaultRegimeNameMessageKey = ctRegimeNameMessage

  override protected def rootForRegime(user: User): Option[CtRoot] = user.regimes.ct

  private def defaultAccountSummary(ctRoot: CtRoot): AccountSummary = {
    val messages = Seq(
      Msg(ctUtrMessage, Seq(ctRoot.identifier.utr)),
      Msg(ctSummaryUnavailableErrorMessage1),
      Msg(ctSummaryUnavailableErrorMessage2),
      Msg(ctSummaryUnavailableErrorMessage3),
      Msg(ctSummaryUnavailableErrorMessage4))

    AccountSummary(
      regimeName = ctRegimeNameMessage,
      messages = messages,
      addenda = Seq.empty,
      status = SummaryStatus.default)
  }

  private def accountSummaryWithDetails(buildPortalUrl: (String) => String,
                                        ctRoot: CtRoot,
                                        accountValue: BigDecimal,
                                        dateOfBalance: String): AccountSummary = {
    val makeAPaymentUri = routes.PaymentController.makeCtPayment().url

    val links = Seq[AccountSummaryLink](
      AccountSummaryLink("ct-account-details-href", buildPortalUrl(ctAccountDetailsPortalUrl), viewAccountDetailsLinkMessage, true),
      AccountSummaryLink("ct-make-payment-href", makeAPaymentUri, makeAPaymentLinkMessage, false),
      AccountSummaryLink("ct-file-return-href", buildPortalUrl(ctFileAReturnPortalUrl), fileAReturnLinkMessage, true)
    )

    val messages = Seq(
      Msg(ctUtrMessage, Seq(ctRoot.identifier.utr)),
      Msg(ctAmountAsOfDateMessage, Seq(MoneyPounds(accountValue), DateConverter.parseToLocalDate(dateOfBalance))))

    AccountSummary(ctRegimeNameMessage, messages, links, SummaryStatus.success)
  }

  private def accountValueIfPresent(accountSummary: Option[CtAccountSummary]): Option[BigDecimal] = {
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

object CtMessageKeys {

  val ctRegimeNameMessage = "ct.regimeName"

  val ctUtrMessage = "ct.message.utr"
  val ctSummaryUnavailableErrorMessage1 = "ct.message.summaryUnavailable.1"
  val ctSummaryUnavailableErrorMessage2 = "ct.message.summaryUnavailable.2"
  val ctSummaryUnavailableErrorMessage3 = "ct.message.summaryUnavailable.3"
  val ctSummaryUnavailableErrorMessage4 = "ct.message.summaryUnavailable.4"
  val ctAmountAsOfDateMessage = "ct.message.amountAsOfDate"
}
