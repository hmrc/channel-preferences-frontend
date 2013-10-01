package controllers.bt.regimeViews

import controllers.bt.routes
import uk.gov.hmrc.common.microservice.sa.SaConnector
import SaMessageKeys._
import SaPortalUrlKeys._
import uk.gov.hmrc.common.microservice.sa.domain.{SaAccountSummary, Liability, SaRoot}
import uk.gov.hmrc.common.microservice.domain.User
import views.helpers.{MoneyPounds, RenderableMessage, LinkMessage}

case class SaAccountSummaryBuilder(saConnector: SaConnector) {

  def build(buildPortalUrl: String => String, user: User): Option[AccountSummary] = {

    user.regimes.sa.map {
      saRoot => getAccountSummaryData(saRoot, saConnector) match {
        case Some(saAccountSummary) => {
          AccountSummary(
            saRegimeName,
            SaAccountSummaryMessagesBuilder(saAccountSummary).build(),
            Seq(
              LinkMessage(buildPortalUrl(saHomePortalUrl), viewAccountDetailsLinkMessage, Some("portalLink")),
              LinkMessage(routes.BusinessTaxController.makeAPaymentLanding().url, makeAPaymentLinkMessage),
              LinkMessage(buildPortalUrl(saHomePortalUrl), fileAReturnLinkMessage))
          )
        }
        case _ =>
          AccountSummary(
            saRegimeName,
            Seq(
              (saSummaryUnavailableErrorMessage1, Seq.empty),
              (saSummaryUnavailableErrorMessage2, Seq.empty),
              (saSummaryUnavailableErrorMessage3, Seq.empty),
              (saSummaryUnavailableErrorMessage4, Seq.empty)
            ),
            Seq.empty
          )
      }
    }
  }

  private def getAccountSummaryData(saRoot: SaRoot, saConnector: SaConnector): Option[SaAccountSummary] = {
    try {
      saRoot.accountSummary(saConnector)
    } catch {
      case e: Exception => None
    }
  }
}

case class SaAccountSummaryMessagesBuilder(accountSummary: SaAccountSummary) {

  def build(): Seq[(String, Seq[RenderableMessage])] = {

    val messages = accountSummary.amountHmrcOwe match {

      case Some(amountHmrcOwe) if amountHmrcOwe > 0 => {

        addLiabilityMessageIfApplicable(accountSummary.nextPayment,
                                    Seq((saYouHaveOverpaidMessage, Seq.empty),(saAmountDueForRepaymentMessage, Seq(MoneyPounds(amountHmrcOwe)))
                                    ), None)
      }
      case _ => {
        accountSummary.totalAmountDueToHmrc match {
          case Some(totalAmountDueToHmrc) => {
            val msgs = totalAmountDueToHmrc.requiresPayment match {
              case true =>
                Seq(
                  (saAmountDueForPaymentMessage, Seq[RenderableMessage](MoneyPounds(totalAmountDueToHmrc.amount))),
                  (saInterestApplicableMessage, Seq.empty)
                )
              case false if totalAmountDueToHmrc.amount == BigDecimal(0) => {
                Seq(
                  (saNothingToPayMessage, Seq.empty)
                )
              }
              case false => {
                Seq(
                  (saAmountDueForPaymentMessage, Seq[RenderableMessage](MoneyPounds(totalAmountDueToHmrc.amount))),
                  (saSmallAmountToPayMessage, Seq.empty)
                )
              }
            }
            addLiabilityMessageIfApplicable(accountSummary.nextPayment, msgs, None)
          }
          case _ => {
            val msgs = Seq(
              (saNothingToPayMessage, Seq.empty)
            )
            addLiabilityMessageIfApplicable(accountSummary.nextPayment, msgs, None)
          }
        }
      }

    }
    messages
  }

  private def getLiabilityMessage(liability: Option[Liability]): Option[(String, Seq[RenderableMessage])] = {
    liability match {
      case Some(l) => Some((saWillBecomeDueMessage, Seq(MoneyPounds(liability.get.amount), liability.get.dueDate)))
      case None => None
    }
  }

  private def addLiabilityMessageIfApplicable(liability: Option[Liability], msgs: Seq[(String, Seq[RenderableMessage])], alternativeMsg: Option[(String, Seq[RenderableMessage])]): Seq[(String, Seq[RenderableMessage])] = {
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

object SaMessageKeys extends CommonBusinessMessageKeys {

  val saRegimeName = "sa.regimeName"

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