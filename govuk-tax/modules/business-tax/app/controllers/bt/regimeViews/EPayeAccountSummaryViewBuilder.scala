package controllers.bt.regimeViews

import uk.gov.hmrc.common.microservice.epaye.EPayeMicroService
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
import scala.Some
import uk.gov.hmrc.common.microservice.epaye.domain.EPayeDomain.EPayeAccountSummary
import org.joda.time.LocalDate

case class EPayeAccountSummaryViewBuilder(buildPortalUrl: String => String, user: User, epayeMicroService: EPayeMicroService) {


  def build(): Option[AccountSummary] = {
    val epayeRootOption: Option[EPayeRoot] = user.regimes.epaye

    epayeRootOption.map {
      epayeRoot: EPayeRoot =>

        val accountSummary: Option[EPayeAccountSummary] = epayeRoot.accountSummary(epayeMicroService)
        val messages : Seq[(String, Seq[RenderableMessage])] = messageStrategy(accountSummary)()

        val links = Seq[RenderableMessage](
        LinkMessage(buildPortalUrl("home"), viewAccountDetailsLink),
        LinkMessage(routes.BusinessTaxController.makeAPaymentLanding().url, makeAPaymentLink),
        LinkMessage(buildPortalUrl("home"), fileAReturnLink))

        AccountSummary("Employers PAYE (RTI)", messages, links)
    }
  }

  def messageStrategy(accountSummary: Option[EPayeAccountSummary]) : () => Seq[(String, Seq[RenderableMessage])] = {
    accountSummary match {
      case Some(summary) if summary.rti.isDefined => createMessages(summary.rti.get) _
      case Some(summary) if summary.nonRti.isDefined => createMessages(summary.nonRti.get) _
      case _ => createNoInformationMessage _
    }
  }

  def createNoInformationMessage() : Seq[(String, Seq[RenderableMessage])] = {
    Seq((unableToDisplayAccountInformation, Seq.empty))
  }

  def createMessages(rti: RTI)() : Seq[(String, Seq[RenderableMessage])] = {
    val amountDue = rti.amountDue
    val amountPaidToDate = rti.amountPaidToDate
    val balance = amountDue.amount - amountPaidToDate.amount //TODO: Move the minus method to the AmountDue
    if(balance < 0) {
      Seq((youHaveOverpaid, Seq(MoneyPounds(balance))), (adjustFuturePayments, Seq.empty))
    } else if(balance > 0) {
      Seq((dueForPayment, Seq(MoneyPounds(balance))))
    }else {
      Seq((nothingToPay, Seq.empty))
    }
  }

  def createMessages(nonRti: NonRTI)() : Seq[(String, Seq[RenderableMessage])] = {
    val amountDue = nonRti.paidToDate
    val currentTaxYear = nonRti.currentTaxYear

    val currentTaxYearWithFollwingYear = createYearDisplayText(currentTaxYear)
    Seq((paidToDateForPeriod, Seq(MoneyPounds(amountDue.amount), currentTaxYearWithFollwingYear)))
  }

  def createYearDisplayText(currentTaxYear: Int) : String = {
    // convert to a day in the current tax year to use LocalDate year functions instead of String.substring
    val taxDate = new LocalDate().withYear(currentTaxYear)
    s"%d - %d".format(taxDate.year().get(), taxDate.yearOfCentury().get() + 1)
  }
}

object EPayeAccountSummaryMessageKeys {
  val nothingToPay = "epaye.message.nothingToPay"
  val youHaveOverpaid = "epaye.message.youHaveOverPaid"
  val adjustFuturePayments = "epaye.message.adjustFuturePayments"
  val dueForPayment = "epaye.message.dueForPayment"
  val unableToDisplayAccountInformation = "epaye.message.unableToDisplayAccountInformation"
  val paidToDateForPeriod = "epaye.message.paidToDateForPeriod"
  val viewAccountDetailsLink = "epaye.message.links.viewAccountDetails"
  val makeAPaymentLink = "vat.accountSummary.linkText.makeAPayment"
  val fileAReturnLink = "epaye.message.links.fileAReturn"
}

