package controllers.bt.regimeViews

import views.helpers._
import controllers.bt.routes
import uk.gov.hmrc.microservice.sa.SaMicroService
import SaAccountSummaryMessageKeys._
import views.helpers.LinkMessage
import views.helpers.RenderableDateMessage
import uk.gov.hmrc.microservice.sa.domain.Liability
import uk.gov.hmrc.microservice.sa.domain.SaRoot
import scala.Some
import uk.gov.hmrc.microservice.sa.domain.SaAccountSummary
import uk.gov.hmrc.microservice.domain.User
import controllers.bt.AccountSummary

object SaAccountSummaryMessageKeys {

  val viewAccountDetailsLink = "sa.message.links.viewAccountDetails"
  val makeAPaymentLink = "vat.accountSummary.linkText.makeAPayment"
  val fileAReturnLink = "sa.message.links.fileAReturn"

  val nothingToPay = "sa.message.nothingToPay"
  val viewHistory = "sa.message.viewHistory"
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
              (unableToDisplayAccount1, List.empty),
              (unableToDisplayAccount2, List.empty),
              (unableToDisplayAccount3, List.empty),
              (unableToDisplayAccount4, List.empty)
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

  def build(): Seq[(String, List[RenderableMessage])] = {

    val messages = accountSummary.amountHmrcOwe match {

      case Some(amountHmrcOwe) if amountHmrcOwe > 0 => {
        val msgs = Seq(
          (youHaveOverpaid, List.empty),
          (amountDueForRepayment, List(RenderableMoneyMessage(amountHmrcOwe)))
        )
        addLiabilityMessageIfApplicable(accountSummary.nextPayment, msgs, Some((viewHistory, List.empty)))
      }
      case _ => {
        accountSummary.totalAmountDueToHmrc match {
          case Some(totalAmountDueToHmrc) => {
            val msgs = totalAmountDueToHmrc.requiresPayment match {
              case true =>
                Seq(
                  (amountDueForPayment, List(RenderableMoneyMessage(totalAmountDueToHmrc.amount))),
                  (interestApplicable, List.empty)
                )
              case false if totalAmountDueToHmrc.amount == BigDecimal(0) => {
                Seq(
                  (nothingToPay, List.empty),
                  (viewHistory, List.empty)
                )
              }
              case false => {
                Seq(
                  (amountDueForPayment, List(RenderableMoneyMessage(totalAmountDueToHmrc.amount))),
                  (smallAmountToPay, List.empty)
                )
              }
            }
            addLiabilityMessageIfApplicable(accountSummary.nextPayment, msgs, None)
          }
          case _ => {
            val msgs = Seq(
              (nothingToPay, List.empty)
            )
            addLiabilityMessageIfApplicable(accountSummary.nextPayment, msgs, Some((viewHistory, List.empty)))
          }
        }
      }

    }
    messages
  }

  private def getLiabilityMessage(liability: Option[Liability]): Option[(String, List[RenderableMessage])] = {
    liability match {
      case Some(l) => Some(willBecomeDue, List(RenderableMoneyMessage(liability.get.amount), RenderableDateMessage(liability.get.dueDate)))
      case None => None
    }
  }

  private def addLiabilityMessageIfApplicable(liability: Option[Liability], msgs: Seq[(String, List[RenderableMessage])], alternativeMsg: Option[(String, List[RenderableMessage])]): Seq[(String, List[RenderableMessage])] = {
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