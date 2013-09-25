package controllers.bt.regimeViews

import uk.gov.hmrc.common.microservice.epaye.EPayeConnector
import controllers.bt.routes
import uk.gov.hmrc.common.microservice.epaye.domain.EPayeDomain._
import EPayeAccountSummaryMessageKeys._
import views.helpers.RenderableMessage
import views.helpers.LinkMessage
import views.helpers.MoneyPounds
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.epaye.domain.EPayeDomain.RTI
import controllers.bt.AccountSummary
import uk.gov.hmrc.common.microservice.epaye.domain.EPayeDomain.EPayeRoot
import scala.{Option, Some}
import uk.gov.hmrc.common.microservice.epaye.domain.EPayeDomain.EPayeAccountSummary

case class EPayeAccountSummaryViewBuilder(buildPortalUrl: String => String, user: User, epayeConnector: EPayeConnector) {


  def build(): Option[AccountSummary] = {
    val epayeRootOption: Option[EPayeRoot] = user.regimes.epaye

    epayeRootOption.map {
      epayeRoot: EPayeRoot =>

        val accountSummary: Option[EPayeAccountSummary] = epayeRoot.accountSummary(epayeConnector)
        val messages : Seq[(String, Seq[RenderableMessage])] = empRefMessage  ++ messageStrategy(accountSummary)()

        val links = Seq[RenderableMessage](
        LinkMessage(buildPortalUrl("home"), viewAccountDetailsLink),
        LinkMessage(routes.BusinessTaxController.makeAPaymentLanding().url, makeAPaymentLink),
        LinkMessage(buildPortalUrl("home"), fileAReturnLink))

        AccountSummary(regimeName(accountSummary), messages, links)
    }
  }

  private def regimeName(accountSummary: Option[EPayeAccountSummary]) : String = {
    accountSummary match {
      case Some(summary) if summary.rti.isDefined => rtiRegimeNameKey
      case Some(summary) if summary.nonRti.isDefined => nonRtiRegimeNameKey
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
    Seq((unableToDisplayAccountInformation, Seq.empty))
  }

  private def createMessages(rti: RTI)() : Seq[(String, Seq[RenderableMessage])] = {
    val balance = rti.balance
    if(balance < 0) {
      Seq((youHaveOverpaid, Seq(MoneyPounds(balance.abs))), (adjustFuturePayments, Seq.empty))
    } else if(balance > 0) {
      Seq((dueForPayment, Seq(MoneyPounds(balance))))
    }else {
      Seq((nothingToPay, Seq.empty))
    }
  }

  val empRefMessage : Seq[(String, Seq[RenderableMessage])] = Seq((empRef, Seq[RenderableMessage](user.userAuthority.empRef.get.toString)))

  private def createMessages(nonRti: NonRTI)() : Seq[(String, Seq[RenderableMessage])] = {
    val amountDue = nonRti.paidToDate
    val currentTaxYear = nonRti.currentTaxYear

    val currentTaxYearWithFollowingYear = createYearDisplayText(currentTaxYear)
    Seq((paidToDateForPeriod, Seq(MoneyPounds(amountDue), currentTaxYearWithFollowingYear)))
  }

  private def createYearDisplayText(currentTaxYear: Int) : String = {
    val nextTaxYear = (currentTaxYear + 1).toString.substring(2)
    s"%d - %s".format(currentTaxYear, nextTaxYear)
  }
}

object EPayeAccountSummaryMessageKeys {
  val rtiRegimeNameKey = "epaye.message.regimeNameRti"
  val nonRtiRegimeNameKey = "epaye.message.regimeNameNonRti"
  val nothingToPay = "epaye.message.nothingToPay"
  val youHaveOverpaid = "epaye.message.youHaveOverPaid"
  val adjustFuturePayments = "epaye.message.adjustFuturePayments"
  val dueForPayment = "epaye.message.dueForPayment"
  val unableToDisplayAccountInformation = "epaye.message.unableToDisplayAccountInformation"
  val paidToDateForPeriod = "epaye.message.paidToDateForPeriod"
  val viewAccountDetailsLink = "common.accountSummary.message.link.viewAccountDetails"
  val makeAPaymentLink = "common.accountSummary.message.link.makeAPayment"
  val fileAReturnLink = "common.accountSummary.message.link.fileAReturn"
  val empRef = "epaye.message.empRef"
}

