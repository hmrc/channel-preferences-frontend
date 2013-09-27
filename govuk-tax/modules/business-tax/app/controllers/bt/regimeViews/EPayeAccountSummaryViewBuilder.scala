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
import controllers.bt.AccountSummary
import uk.gov.hmrc.common.microservice.epaye.domain.EPayeDomain.EPayeRoot
import uk.gov.hmrc.common.microservice.epaye.domain.EPayeDomain.EPayeAccountSummary

case class EPayeAccountSummaryViewBuilder(buildPortalUrl: String => String, user: User, epayeConnector: EPayeConnector) {

  def build(): Option[AccountSummary] = {
    val epayeRootOption: Option[EPayeRoot] = user.regimes.epaye

    epayeRootOption.map {
      epayeRoot: EPayeRoot =>

        val accountSummary: Option[EPayeAccountSummary] = epayeRoot.accountSummary(epayeConnector)
        val messages : Seq[(String, Seq[RenderableMessage])] = renderEmpRefMessage  ++ messageStrategy(accountSummary)()

        val links = Seq[RenderableMessage](
        LinkMessage(buildPortalUrl(homePortalUrl), viewAccountDetailsLinkMessage),
        LinkMessage(routes.BusinessTaxController.makeAPaymentLanding().url, makeAPaymentLinkMessage),
        LinkMessage(buildPortalUrl(homePortalUrl), fileAReturnLinkMessage))

        AccountSummary(regimeName(accountSummary), messages, links)
    }
  }

  private def regimeName(accountSummary: Option[EPayeAccountSummary]) : String = {
    accountSummary match {
      case Some(summary) if summary.rti.isDefined => rtiRegimeNameMessage
      case Some(summary) if summary.nonRti.isDefined => nonRtiRegimeNameMessage
      case _ => "N/A"
    }
  }

  private def messageStrategy(accountSummary: Option[EPayeAccountSummary]) : () => Seq[(String, Seq[RenderableMessage])] = {
    accountSummary match {
      case Some(summary) if summary.rti.isDefined => createMessages(summary.rti.get) _
      case Some(summary) if summary.nonRti.isDefined => createMessages(summary.nonRti.get) _
      case _ => createNoInformationMessage _
    }
  }

  private def createNoInformationMessage() : Seq[(String, Seq[RenderableMessage])] = {
    Seq((unableToDisplayAccountInformationMessage, Seq.empty))
  }

  private def createMessages(rti: RTI)() : Seq[(String, Seq[RenderableMessage])] = {
    val balance = rti.balance
    if(balance < 0) {
      Seq((youHaveOverpaidMessage, Seq(MoneyPounds(balance.abs))), (adjustFuturePaymentsMessage, Seq.empty))
    } else if(balance > 0) {
      Seq((dueForPaymentMessage, Seq(MoneyPounds(balance))))
    }else {
      Seq((nothingToPayMessage, Seq.empty))
    }
  }

  private def renderEmpRefMessage : Seq[(String, Seq[RenderableMessage])] = Seq((empRefMessage, Seq[RenderableMessage](user.userAuthority.empRef.get.toString)))

  private def createMessages(nonRti: NonRTI)() : Seq[(String, Seq[RenderableMessage])] = {
    val amountDue = nonRti.paidToDate
    val currentTaxYear = nonRti.currentTaxYear

    val currentTaxYearWithFollowingYear = createYearDisplayText(currentTaxYear)
    Seq((paidToDateForPeriodMessage, Seq(MoneyPounds(amountDue), currentTaxYearWithFollowingYear)))
  }

  private def createYearDisplayText(currentTaxYear: Int) : String = {
    val nextTaxYear = (currentTaxYear + 1).toString.substring(2)
    s"%d - %s".format(currentTaxYear, nextTaxYear)
  }
}

object EPayePortalUrlKeys {
  val homePortalUrl = "home"
}

object EPayeMessageKeys extends CommonBusinessMessageKeys {

  val rtiRegimeNameMessage = "epaye.regimeName.rti"
  val nonRtiRegimeNameMessage = "epaye.regimeName.nonRti"

  val nothingToPayMessage = "epaye.message.nothingToPay"
  val youHaveOverpaidMessage = "epaye.message.youHaveOverPaid"
  val adjustFuturePaymentsMessage = "epaye.message.adjustFuturePayments"
  val dueForPaymentMessage = "epaye.message.dueForPayment"
  val unableToDisplayAccountInformationMessage = "epaye.message.unableToDisplayAccountInformation"
  val paidToDateForPeriodMessage = "epaye.message.paidToDateForPeriod"
  val empRefMessage = "epaye.message.empRef"
}

