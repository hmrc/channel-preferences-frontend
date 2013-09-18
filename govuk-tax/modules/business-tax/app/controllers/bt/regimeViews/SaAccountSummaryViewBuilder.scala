package controllers.bt.regimeViews

import controllers.bt.routes
import uk.gov.hmrc.common.microservice.sa.SaMicroService
import SaAccountSummaryMessageKeys._
import uk.gov.hmrc.common.microservice.sa.domain.Liability
import uk.gov.hmrc.common.microservice.sa.domain.SaRoot
import uk.gov.hmrc.common.microservice.sa.domain.SaAccountSummary
import uk.gov.hmrc.common.microservice.domain.User
import controllers.bt.AccountSummary
import views.helpers.{MoneyPounds, RenderableMessage, LinkMessage}

object SaAccountSummaryMessageKeys {

  val viewAccountDetailsLink = "sa.message.links.viewAccountDetails"
  val makeAPaymentLink = "vat.accountSummary.linkText.makeAPayment"
  val fileAReturnLink = "sa.message.links.fileAReturn"

  val nothingToPay = "sa.message.nothingToPay"
  val amountDueForPayment = "sa.message.amountDueForPayment"
  val interestApplicable = "sa.message.interestApplicable"
  val willBecomeDue = "sa.message.willBecomeDue"
  val youHaveOverpaid = "sa.message.youHaveOverpaid"
  val amountDueForRepayment = "sa.message.amountDueForRepayment"
  val smallAmountToPay = "sa.message.smallAmountToPay"
  val unableToDisplayAccount1 = "sa.message.unableToDisplayAccount.1"
  val unableToDisplayAccount2 = "sa.message.unableToDisplayAccount.2"
  val unableToDisplayAccount3 = "sa.message.unableToDisplayAccount.3"
  val unableToDisplayAccount4 = "sa.message.unableToDisplayAccount.4"

  val saRegimeName = "Self Assessment (SA)"

}

case class SaAccountSummaryViewBuilder(buildPortalUrl: String => String, user: User, saMicroService: SaMicroService) {

  def build(): Option[AccountSummary] = {

    user.regimes.sa.map {
      saRoot => getAccountSummaryData(saRoot, saMicroService) match {
        case Some(saAccountSummary) => {
          AccountSummary(
            saRegimeName,
            SaAccountSummaryMessagesBuilder(saAccountSummary).build(),
            Seq(
              LinkMessage(buildPortalUrl("home"), viewAccountDetailsLink),
              LinkMessage(routes.BusinessTaxController.makeAPaymentLanding().url, makeAPaymentLink),
              LinkMessage(buildPortalUrl("home"), fileAReturnLink))
          )
        }
        case _ =>
          AccountSummary(
            saRegimeName,
            Seq(
              (unableToDisplayAccount1, Seq.empty),
              (unableToDisplayAccount2, Seq.empty),
              (unableToDisplayAccount3, Seq.empty),
              (unableToDisplayAccount4, Seq.empty)
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
                                    Seq((youHaveOverpaid, Seq.empty),(amountDueForRepayment, Seq(MoneyPounds(amountHmrcOwe)))
                                    ), None)
      }
      case _ => {
        accountSummary.totalAmountDueToHmrc match {
          case Some(totalAmountDueToHmrc) => {
            val msgs = totalAmountDueToHmrc.requiresPayment match {
              case true =>
                Seq(
                  (amountDueForPayment, Seq[RenderableMessage](MoneyPounds(totalAmountDueToHmrc.amount))),
                  (interestApplicable, Seq.empty)
                )
              case false if totalAmountDueToHmrc.amount == BigDecimal(0) => {
                Seq(
                  (nothingToPay, Seq.empty)
                )
              }
              case false => {
                Seq(
                  (amountDueForPayment, Seq[RenderableMessage](MoneyPounds(totalAmountDueToHmrc.amount))),
                  (smallAmountToPay, Seq.empty)
                )
              }
            }
            addLiabilityMessageIfApplicable(accountSummary.nextPayment, msgs, None)
          }
          case _ => {
            val msgs = Seq(
              (nothingToPay, Seq.empty)
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
      case Some(l) => Some((willBecomeDue, Seq(MoneyPounds(liability.get.amount), liability.get.dueDate)))
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