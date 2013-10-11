package controllers.bt.regimeViews

import uk.gov.hmrc.common.microservice.epaye.EpayeConnector
import controllers.bt.routes
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeDomain._
import CommonBusinessMessageKeys._
import EpayeMessageKeys._
import EpayePortalUrlKeys._
import views.helpers.{RenderableMessage, LinkMessage, MoneyPounds}
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeDomain.RTI
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeDomain.EpayeRoot
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeDomain.EpayeAccountSummary
import uk.gov.hmrc.domain.EmpRef
import scala.util.Try

case class EpayeAccountSummaryBuilder(epayeConnector: EpayeConnector) extends AccountSummaryBuilder[EmpRef, EpayeRoot] {

  override def buildAccountSummary(epayeRoot: EpayeRoot, buildPortalUrl: String => String): AccountSummary = {

    val accountSummary: Option[EpayeAccountSummary] = epayeRoot.accountSummary(epayeConnector)
    val messages = renderEmpRefMessage(epayeRoot.identifier) ++ messageStrategy(accountSummary)()

    val links = createLinks(buildPortalUrl, accountSummary)

      Seq[RenderableMessage](
      LinkMessage(buildPortalUrl(epayeHomePortalUrl), viewAccountDetailsLinkMessage),
      LinkMessage(routes.BusinessTaxController.makeAPaymentLanding().url, makeAPaymentLinkMessage),
      LinkMessage(buildPortalUrl(epayeHomePortalUrl), fileAReturnLinkMessage))

    AccountSummary(regimeName(accountSummary), messages, links, SummaryStatus.success)
  }

  override def rootForRegime(user: User): Option[Try[EpayeRoot]] = user.regimes.epaye


  private def regimeName(accountSummary: Option[EpayeAccountSummary]): String = {
    accountSummary match {
      case Some(summary) if summary.rti.isDefined => epayeRtiRegimeNameMessage
      case Some(summary) if summary.nonRti.isDefined => epayeNonRtiRegimeNameMessage
      case _ => epayeUnknownRegimeName
    }
  }

  private def messageStrategy(accountSummary: Option[EpayeAccountSummary]): () => Seq[Msg] = {
    accountSummary match {
      case Some(summary) if summary.rti.isDefined => createMessages(summary.rti.get)
      case Some(summary) if summary.nonRti.isDefined => createMessages(summary.nonRti.get)
      case _ => createNoInformationMessage
    }
  }

  private def createLinks(buildPortalUrl: String => String, accountSummary: Option[EpayeAccountSummary]) : Seq[RenderableMessage] = {
    val expectedRtiLinks = Seq[RenderableMessage](
      LinkMessage(buildPortalUrl(epayeHomePortalUrl), viewAccountDetailsLinkMessage),
      LinkMessage(routes.BusinessTaxController.makeAPaymentLanding().url, makeAPaymentLinkMessage)
    )


    val expectedNonRtiLinks = Seq[RenderableMessage](
      LinkMessage(buildPortalUrl(epayeHomePortalUrl), viewAccountDetailsLinkMessage),
      LinkMessage(routes.BusinessTaxController.makeAPaymentLanding().url, makeAPaymentLinkMessage),
      LinkMessage(buildPortalUrl(epayeHomePortalUrl), fileAReturnLinkMessage))


    accountSummary match {
      case Some(summary) if summary.rti.isDefined => expectedRtiLinks
      case Some(summary) if summary.nonRti.isDefined => expectedNonRtiLinks
      case _ => expectedNonRtiLinks
    }
  }

  private def createNoInformationMessage(): Seq[Msg] = {
    Seq(Msg(epayeSummaryUnavailableErrorMessage))
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
}

object EpayeMessageKeys {

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

