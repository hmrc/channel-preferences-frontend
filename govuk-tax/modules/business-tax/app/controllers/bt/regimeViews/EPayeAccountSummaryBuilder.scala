package controllers.bt.regimeViews

import uk.gov.hmrc.common.microservice.epaye.EPayeConnector
import controllers.bt.routes
import uk.gov.hmrc.common.microservice.epaye.domain.EPayeDomain._
import EPayeMessageKeys._
import EPayePortalUrlKeys._
import views.helpers.RenderableMessage
import views.helpers.LinkMessage
import views.helpers.MoneyPounds
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.epaye.domain.EPayeDomain.RTI
import uk.gov.hmrc.common.microservice.epaye.domain.EPayeDomain.EPayeRoot
import uk.gov.hmrc.common.microservice.epaye.domain.EPayeDomain.EPayeAccountSummary
import uk.gov.hmrc.domain.EmpRef

case class EPayeAccountSummaryBuilder(epayeConnector: EPayeConnector) extends AccountSummaryTemplate[EPayeRoot] {

  override def buildAccountSummary(epayeRoot: EPayeRoot, buildPortalUrl: String => String): AccountSummary = {


    val accountSummary: Option[EPayeAccountSummary] = epayeRoot.accountSummary(epayeConnector)
    val messages: Seq[(String, Seq[RenderableMessage])] = renderEmpRefMessage(epayeRoot.identifier) ++ messageStrategy(accountSummary)()

    val links = Seq[RenderableMessage](
      LinkMessage(buildPortalUrl(epayeHomePortalUrl), viewAccountDetailsLinkMessage),
      LinkMessage(routes.BusinessTaxController.makeAPaymentLanding().url, makeAPaymentLinkMessage),
      LinkMessage(buildPortalUrl(epayeHomePortalUrl), fileAReturnLinkMessage))

    AccountSummary(regimeName(accountSummary), messages, links)
  }

  override def rootForRegime(user: User): Option[EPayeRoot] = user.regimes.epaye


  private def regimeName(accountSummary: Option[EPayeAccountSummary]): String = {
    accountSummary match {
      case Some(summary) if summary.rti.isDefined => epayeRtiRegimeNameMessage
      case Some(summary) if summary.nonRti.isDefined => epayeNonRtiRegimeNameMessage
      case _ => epayeUnknownRegimeName
    }
  }

  private def messageStrategy(accountSummary: Option[EPayeAccountSummary]): () => Seq[(String, Seq[RenderableMessage])] = {
    accountSummary match {
      case Some(summary) if summary.rti.isDefined => createMessages(summary.rti.get)
      case Some(summary) if summary.nonRti.isDefined => createMessages(summary.nonRti.get)
      case _ => createNoInformationMessage
    }
  }

  private def createNoInformationMessage(): Seq[(String, Seq[RenderableMessage])] = {
    Seq((epayeSummaryUnavailableErrorMessage, Seq.empty))
  }

  private def createMessages(rti: RTI)(): Seq[(String, Seq[RenderableMessage])] = {
    val balance = rti.balance
    if (balance < 0) {
      Seq((epayeYouHaveOverpaidMessage, Seq(MoneyPounds(balance.abs))), (epayeAdjustFuturePaymentsMessage, Seq.empty))
    } else if (balance > 0) {
      Seq((epayeDueForPaymentMessage, Seq(MoneyPounds(balance))))
    } else {
      Seq((epayeNothingToPayMessage, Seq.empty))
    }
  }

  private def renderEmpRefMessage(empRef: EmpRef): Seq[(String, Seq[RenderableMessage])] = Seq((epayeEmpRefMessage, Seq[RenderableMessage](empRef.toString)))

  private def createMessages(nonRti: NonRTI)(): Seq[(String, Seq[RenderableMessage])] = {
    val amountDue = nonRti.paidToDate
    val currentTaxYear = nonRti.currentTaxYear

    val currentTaxYearWithFollowingYear = createYearDisplayText(currentTaxYear)
    Seq((epayePaidToDateForPeriodMessage, Seq(MoneyPounds(amountDue), currentTaxYearWithFollowingYear)))
  }

  private def createYearDisplayText(currentTaxYear: Int): String = {
    val nextTaxYear = (currentTaxYear + 1).toString.substring(2)
    s"%d - %s".format(currentTaxYear, nextTaxYear)
  }
}

object EPayePortalUrlKeys {
  val epayeHomePortalUrl = "home"
}

object EPayeMessageKeys extends CommonBusinessMessageKeys {

  val epayeRtiRegimeNameMessage = "epaye.regimeName.rti"
  val epayeNonRtiRegimeNameMessage = "epaye.regimeName.nonRti"
  val epayeUnknownRegimeName = "epaye.regimeName.unknown"

  val epayeNothingToPayMessage = "epaye.message.nothingToPay"
  val epayeYouHaveOverpaidMessage = "epaye.message.youHaveOverPaid"
  val epayeAdjustFuturePaymentsMessage = "epaye.message.adjustFuturePayments"
  val epayeDueForPaymentMessage = "epaye.message.dueForPayment"
  val epayeSummaryUnavailableErrorMessage = "epaye.message.summaryUnavailable"
  val epayePaidToDateForPeriodMessage = "epaye.message.paidToDateForPeriod"
  val epayeEmpRefMessage = "epaye.message.empRef"
}

