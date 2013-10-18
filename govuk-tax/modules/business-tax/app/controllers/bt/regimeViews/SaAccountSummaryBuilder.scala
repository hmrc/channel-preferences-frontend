package controllers.bt.regimeViews

import controllers.bt.routes
import uk.gov.hmrc.common.microservice.sa.SaConnector
import CommonBusinessMessageKeys._
import SaMessageKeys._
import SaPortalUrlKeys._
import uk.gov.hmrc.common.microservice.sa.domain.SaDomain.{SaAccountSummary, Liability, SaRoot}
import uk.gov.hmrc.common.microservice.domain.User
import views.helpers.{MoneyPounds, RenderableMessage, LinkMessage}
import uk.gov.hmrc.domain.SaUtr

case class SaAccountSummaryBuilder(saConnector: SaConnector = new SaConnector) extends AccountSummaryBuilder[SaUtr, SaRoot] {


  private def utrMessage(utr: SaUtr) : Msg = {
    Msg(saUtrMessage, Seq(utr.utr))
  }

  def rootForRegime(user: User): Option[SaRoot] = user.regimes.sa

  def buildAccountSummary(saRoot: SaRoot, buildPortalUrl: String => String): AccountSummary = {
    saRoot.accountSummary(saConnector) match {
      case Some(saAccountSummary) => {
        AccountSummary(
          regimeName = saRegimeName,
          messages = utrMessage(saRoot.identifier) +: SaAccountSummaryMessagesBuilder(saAccountSummary).build(),
          addenda = Seq(
            LinkMessage(buildPortalUrl(saHomePortalUrl), viewAccountDetailsLinkMessage, Some("portalLink")),
            LinkMessage(routes.BusinessTaxController.makeAPaymentLanding().url, makeAPaymentLinkMessage),
            LinkMessage(buildPortalUrl(saHomePortalUrl), fileAReturnLinkMessage)),
          status = SummaryStatus.success
        )
      }
      case _ =>
        AccountSummary(
          regimeName = saRegimeName,
          messages = utrMessage(saRoot.identifier) +: Seq(
            Msg(saSummaryUnavailableErrorMessage1),
            Msg(saSummaryUnavailableErrorMessage2),
            Msg(saSummaryUnavailableErrorMessage3),
            Msg(saSummaryUnavailableErrorMessage4)
          ),
          addenda = Seq.empty,
          status = SummaryStatus.default
        )
    }
  }

  override protected val defaultRegimeNameMessageKey = saRegimeName
}

case class SaAccountSummaryMessagesBuilder(accountSummary: SaAccountSummary) {

  def build(): Seq[Msg] = {

    val messages = accountSummary.amountHmrcOwe match {

      case Some(amountHmrcOwe) if amountHmrcOwe > 0 => {

        addLiabilityMessageIfApplicable(
          liability = accountSummary.nextPayment,
          msgs = Seq(Msg(saYouHaveOverpaidMessage), Msg(saAmountDueForRepaymentMessage, Seq(MoneyPounds(amountHmrcOwe)))),
          alternativeMsg = None)
      }
      case _ => {
        accountSummary.totalAmountDueToHmrc match {
          case Some(totalAmountDueToHmrc) => {
            val msgs = totalAmountDueToHmrc.requiresPayment match {
              case true =>
                Seq(
                  Msg(saAmountDueForPaymentMessage, Seq(MoneyPounds(totalAmountDueToHmrc.amount))),
                  Msg(saInterestApplicableMessage)
                )
              case false if totalAmountDueToHmrc.amount == BigDecimal(0) => {
                Seq(
                  Msg(saNothingToPayMessage)
                )
              }
              case false => {
                Seq(
                  Msg(saAmountDueForPaymentMessage, Seq[RenderableMessage](MoneyPounds(totalAmountDueToHmrc.amount))),
                  Msg(saSmallAmountToPayMessage)
                )
              }
            }
            addLiabilityMessageIfApplicable(accountSummary.nextPayment, msgs, None)
          }
          case _ => {
            val msgs = Seq(
              Msg(saNothingToPayMessage)
            )
            addLiabilityMessageIfApplicable(accountSummary.nextPayment, msgs, None)
          }
        }
      }

    }
    messages
  }

  private def getLiabilityMessage(liability: Option[Liability]): Option[Msg] = {
    liability match {
      case Some(l) => Some(Msg(saWillBecomeDueMessage, Seq(MoneyPounds(liability.get.amount), liability.get.dueDate)))
      case None => None
    }
  }

  private def addLiabilityMessageIfApplicable(liability: Option[Liability], msgs: Seq[Msg], alternativeMsg: Option[Msg]): Seq[Msg] = {
    val liabilityMessage = getLiabilityMessage(accountSummary.nextPayment)
    liabilityMessage match {
      case Some(message) => msgs ++ liabilityMessage
      case _ => {
        alternativeMsg match {
          case Some(altMsg) => msgs ++ Seq(altMsg)
          case None => msgs
        }
      }
    }
  }

}

object SaPortalUrlKeys {
  val saHomePortalUrl = "home"
}

object SaMessageKeys {

  val saRegimeName = "sa.regimeName"

  val saUtrMessage = "sa.message.utr"
  val saNothingToPayMessage = "sa.message.nothingToPay"
  val saAmountDueForPaymentMessage = "sa.message.amountDueForPayment"
  val saInterestApplicableMessage = "sa.message.interestApplicable"
  val saWillBecomeDueMessage = "sa.message.willBecomeDue"
  val saYouHaveOverpaidMessage = "sa.message.youHaveOverpaid"
  val saAmountDueForRepaymentMessage = "sa.message.amountDueForRepayment"
  val saSmallAmountToPayMessage = "sa.message.smallAmountToPay"
  val saSummaryUnavailableErrorMessage1 = "sa.message.summaryUnavailable.1"
  val saSummaryUnavailableErrorMessage2 = "sa.message.summaryUnavailable.2"
  val saSummaryUnavailableErrorMessage3 = "sa.message.summaryUnavailable.3"
  val saSummaryUnavailableErrorMessage4 = "sa.message.summaryUnavailable.4"
}