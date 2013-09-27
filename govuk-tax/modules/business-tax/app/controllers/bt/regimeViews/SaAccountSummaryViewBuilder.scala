package controllers.bt.regimeViews

import controllers.bt.routes
import uk.gov.hmrc.common.microservice.sa.SaMicroService
import SaMessageKeys._
import SaPortalUrlKeys._
import uk.gov.hmrc.common.microservice.sa.domain.Liability
import uk.gov.hmrc.common.microservice.sa.domain.SaRoot
import uk.gov.hmrc.common.microservice.sa.domain.SaAccountSummary
import uk.gov.hmrc.common.microservice.domain.User
import controllers.bt.AccountSummary
import views.helpers.{MoneyPounds, RenderableMessage, LinkMessage}

case class SaAccountSummaryViewBuilder(buildPortalUrl: String => String, user: User, saMicroService: SaMicroService) {

  def build(): Option[AccountSummary] = {

    user.regimes.sa.map {
      saRoot => getAccountSummaryData(saRoot, saMicroService) match {
        case Some(saAccountSummary) => {
          AccountSummary(
            regimeName,
            SaAccountSummaryMessagesBuilder(saAccountSummary).build(),
            Seq(
              LinkMessage(buildPortalUrl(homePortalUrl), viewAccountDetailsLinkMessage),
              LinkMessage(routes.BusinessTaxController.makeAPaymentLanding().url, makeAPaymentLinkMessage),
              LinkMessage(buildPortalUrl(homePortalUrl), fileAReturnLinkMessage))
          )
        }
        case _ =>
          AccountSummary(
            regimeName,
            Seq(
              (unableToDisplayAccountMessage1, Seq.empty),
              (unableToDisplayAccountMessage2, Seq.empty),
              (unableToDisplayAccountMessage3, Seq.empty),
              (unableToDisplayAccountMessage4, Seq.empty)
            ),
            Seq.empty
          )
      }
    }
  }

  private def getAccountSummaryData(saRoot: SaRoot, saMicroService: SaMicroService): Option[SaAccountSummary] = {
    try {
      saRoot.accountSummary(saMicroService)
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
                                    Seq((youHaveOverpaidMessage, Seq.empty),(amountDueForRepaymentMessage, Seq(MoneyPounds(amountHmrcOwe)))
                                    ), None)
      }
      case _ => {
        accountSummary.totalAmountDueToHmrc match {
          case Some(totalAmountDueToHmrc) => {
            val msgs = totalAmountDueToHmrc.requiresPayment match {
              case true =>
                Seq(
                  (amountDueForPaymentMessage, Seq[RenderableMessage](MoneyPounds(totalAmountDueToHmrc.amount))),
                  (interestApplicableMessage, Seq.empty)
                )
              case false if totalAmountDueToHmrc.amount == BigDecimal(0) => {
                Seq(
                  (nothingToPayMessage, Seq.empty)
                )
              }
              case false => {
                Seq(
                  (amountDueForPaymentMessage, Seq[RenderableMessage](MoneyPounds(totalAmountDueToHmrc.amount))),
                  (smallAmountToPayMessage, Seq.empty)
                )
              }
            }
            addLiabilityMessageIfApplicable(accountSummary.nextPayment, msgs, None)
          }
          case _ => {
            val msgs = Seq(
              (nothingToPayMessage, Seq.empty)
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
      case Some(l) => Some((willBecomeDueMessage, Seq(MoneyPounds(liability.get.amount), liability.get.dueDate)))
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
  val homePortalUrl = "home"
}

object SaMessageKeys extends CommonBusinessMessageKeys {

  val regimeName = "sa.regimeName"

  val nothingToPayMessage = "sa.message.nothingToPay"
  val amountDueForPaymentMessage = "sa.message.amountDueForPayment"
  val interestApplicableMessage = "sa.message.interestApplicable"
  val willBecomeDueMessage = "sa.message.willBecomeDue"
  val youHaveOverpaidMessage = "sa.message.youHaveOverpaid"
  val amountDueForRepaymentMessage = "sa.message.amountDueForRepayment"
  val smallAmountToPayMessage = "sa.message.smallAmountToPay"
  val unableToDisplayAccountMessage1 = "sa.message.unableToDisplayAccount.1"
  val unableToDisplayAccountMessage2 = "sa.message.unableToDisplayAccount.2"
  val unableToDisplayAccountMessage3 = "sa.message.unableToDisplayAccount.3"
  val unableToDisplayAccountMessage4 = "sa.message.unableToDisplayAccount.4"
}