package controllers.bt.accountsummary

import uk.gov.hmrc.common.microservice.epaye.EpayeConnector
import controllers.bt.routes
import CommonBusinessMessageKeys._
import EpayeMessageKeys._
import EpayePortalUrlKeys._
import views.helpers.MoneyPounds
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.common.microservice.epaye.domain.{RTI, NonRTI, EpayeAccountSummary, EpayeRoot}
import controllers.common.actions.HeaderCarrier
import scala.concurrent._
import uk.gov.hmrc.common.MdcLoggingExecutionContext.fromLoggingDetails

case class EpayeAccountSummaryBuilder(epayeConnector: EpayeConnector = new EpayeConnector) extends AccountSummaryBuilder[EmpRef, EpayeRoot] {

  override def buildAccountSummary(epayeRoot: EpayeRoot, buildPortalUrl: String => String)(implicit headerCarrier: HeaderCarrier): Future[AccountSummary] = {
    val accountSummaryOF = epayeRoot.accountSummary(epayeConnector, headerCarrier)
    accountSummaryOF map { accountSummary =>
      val messages = renderEmpRefMessage(epayeRoot.identifier) ++ messageStrategy(accountSummary)()
      val links = createLinks(buildPortalUrl, accountSummary)
      AccountSummary(epayeRegimeNameMessage, messages, links, SummaryStatus.success)
    }
  }

  override def rootForRegime(user: User): Option[EpayeRoot] = user.regimes.epaye

  private def messageStrategy(accountSummary: Option[EpayeAccountSummary]): () => Seq[Msg] = {
    accountSummary match {
      case Some(EpayeAccountSummary(Some(rti), None)) => createMessages(rti)
      case Some(EpayeAccountSummary(None, Some(nonRti))) => createMessages(nonRti)
      case _ => createNoInformationMessage
    }
  }

  private def createLinks(buildPortalUrl: String => String, accountSummary: Option[EpayeAccountSummary]): Seq[AccountSummaryLink] = {

    def links = Seq[AccountSummaryLink](
      AccountSummaryLink("epaye-account-details-href", buildPortalUrl(epayeAccountDetailsPortalUrl), epayeViewAccountDetailsLinkMessage, sso = true),
      AccountSummaryLink("epaye-make-payment-href", routes.PaymentController.makeEpayePayment().url, epayeMakeAPaymentLinkMessage, sso = false)
    )

    accountSummary match {
      case Some(EpayeAccountSummary(Some(rti), None)) => links
      case Some(EpayeAccountSummary(None, Some(nonRti))) => links
      case _ => Seq.empty
    }
  }

  private def createNoInformationMessage(): Seq[Msg] = {
    Seq(Msg(epayeSummaryUnavailableErrorMessage1),
      Msg(epayeSummaryUnavailableErrorMessage2),
      Msg(epayeSummaryUnavailableErrorMessage3),
      Msg(epayeSummaryUnavailableErrorMessage4))
  }

  private def createMessages(rti: RTI)(): Seq[Msg] = {
    val balance = rti.balance
    if (balance < 0) {
      Seq(Msg(epayeYouHaveOverpaidMessage, Seq(MoneyPounds(balance.abs))), Msg(epayeAdjustFuturePaymentsMessage))
    } else if (balance > 0) {
      Seq(Msg(epayeDueForPaymentMessage, Seq(MoneyPounds(balance))))
    } else {
      Seq(Msg(epayeNothingToPayMessage))
    }
  }

  private def renderEmpRefMessage(empRef: EmpRef): Seq[Msg] = Seq(Msg(epayeEmpRefMessage, Seq(empRef.toString)))

  private def createMessages(nonRti: NonRTI)(): Seq[Msg] = {
    val amountDue = nonRti.paidToDate
    val currentTaxYear = nonRti.currentTaxYear

    val currentTaxYearWithFollowingYear = createYearDisplayText(currentTaxYear)
    Seq(Msg(epayePaidToDateForPeriodMessage, Seq(MoneyPounds(amountDue), currentTaxYearWithFollowingYear)))
  }

  private def createYearDisplayText(currentTaxYear: Int): String = {
    val nextTaxYear = (currentTaxYear + 1).toString.substring(2)
    s"%d - %s".format(currentTaxYear, nextTaxYear)
  }

  override protected val defaultRegimeNameMessageKey = epayeUnknownRegimeName
}

object EpayePortalUrlKeys {
  val epayeHomePortalUrl = "home"
  val epayeAccountDetailsPortalUrl = "epayeAccountDetails"
}

object EpayeMessageKeys {

  val epayeRegimeNameMessage = "epaye.regimeName"
  val epayeUnknownRegimeName = "epaye.regimeName.unknown"

  val epayeNothingToPayMessage = "epaye.message.nothingToPay"
  val epayeYouHaveOverpaidMessage = "epaye.message.youHaveOverPaid"
  val epayeAdjustFuturePaymentsMessage = "epaye.message.adjustFuturePayments"
  val epayeDueForPaymentMessage = "epaye.message.dueForPayment"
  val epayePaidToDateForPeriodMessage = "epaye.message.paidToDateForPeriod"
  val epayeEmpRefMessage = "epaye.message.empRef"
  val epayeSummaryUnavailableErrorMessage1 = "epaye.message.summaryUnavailable.1"
  val epayeSummaryUnavailableErrorMessage2 = "epaye.message.summaryUnavailable.2"
  val epayeSummaryUnavailableErrorMessage3 = "epaye.message.summaryUnavailable.3"
  val epayeSummaryUnavailableErrorMessage4 = "epaye.message.summaryUnavailable.4"
  val epayeViewAccountDetailsLinkMessage = "epaye.link.message.accountSummary.viewAccountDetails"
  val epayeMakeAPaymentLinkMessage = "epaye.link.message.accountSummary.makeAPayment"
}

