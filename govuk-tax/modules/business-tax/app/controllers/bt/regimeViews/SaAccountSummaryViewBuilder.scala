package controllers.bt.regimeViews

import views.helpers.{RenderableStringMessage, RenderableMessage, LinkMessage}
import controllers.bt.{routes, AccountSummary}
import uk.gov.hmrc.microservice.sa.domain.{SaAccountSummary, Liability, SaRoot}
import uk.gov.hmrc.microservice.sa.SaMicroService
import uk.gov.hmrc.microservice.domain.User
import SaAccountSummaryMessageKeys._

object SaAccountSummaryMessageKeys {

  val viewAccountDetailsLink = "sa.message.links.viewAccountDetails"
  val makeAPaymentLink = "vat.accountSummary.linkText.makeAPayment"

}

case class SaAccountSummaryViewBuilder(buildPortalUrl: String => String, user: User, saMicroService: SaMicroService) {

  def build(): Option[AccountSummary] = {

    user.regimes.sa.map {
      saRoot => getAccountSummaryData(saRoot, saMicroService) match {
        case Some(saAccountSummary) => {
          AccountSummary(
            "Self Assessment (SA)",
            SaAccountSummaryMessagesBuilder(saAccountSummary).build(),
            Seq(
              LinkMessage(buildPortalUrl("home"), viewAccountDetailsLink),
              LinkMessage(routes.BusinessTaxController.makeAPaymentLanding().url, makeAPaymentLink),
              LinkMessage(buildPortalUrl("home"), "sa.message.links.fileAReturn"))
          )
        }
        case _ => AccountSummary("Self Assessment (SA)", Seq(("sa.message.unableToDisplayAccount.1", List.empty),("sa.message.unableToDisplayAccount.2", List.empty),("sa.message.unableToDisplayAccount.3", List.empty),("sa.message.unableToDisplayAccount.4", List.empty)), Seq.empty)
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
          ("sa.message.youHaveOverpaid", List.empty),
          ("sa.message.amountDueForRepayment", List(RenderableStringMessage(amountHmrcOwe.toString())))
        )
        addLiabilityMessageIfApplicable(accountSummary.nextPayment, msgs, Some(("sa.message.viewHistory", List.empty)))
      }
      case _ => {
        accountSummary.totalAmountDueToHmrc match {
          case Some(totalAmountDueToHmrc) => {
            val msgs = totalAmountDueToHmrc.requiresPayment match {
              case true =>
                Seq(
                  ("sa.message.amountDueForPayment", List(RenderableStringMessage(totalAmountDueToHmrc.amount.toString()))),
                  ("sa.message.interestApplicable", List.empty)
                )
              case false if totalAmountDueToHmrc.amount == BigDecimal(0) => {
                Seq(
                  ("sa.message.nothingToPay", List.empty),
                  ("sa.message.viewHistory", List.empty)
                )
              }
              case false => {
                Seq(
                  ("sa.message.amountDueForPayment", List(RenderableStringMessage(totalAmountDueToHmrc.amount.toString()))),
                  ("sa.message.smallAmountToPay", List.empty)
                )
              }
            }
            addLiabilityMessageIfApplicable(accountSummary.nextPayment, msgs, None)
          }
          case _ => {
            val msgs = Seq(
              ("sa.message.nothingToPay", List.empty)
            )
            addLiabilityMessageIfApplicable(accountSummary.nextPayment, msgs, Some(("sa.message.viewHistory", List.empty)))
          }
        }
      }

    }
    messages
  }

  private def getLiabilityMessage(liability: Option[Liability]): Option[(String, List[RenderableMessage])] = {
    liability match {
      case Some(l) => Some("sa.message.willBecomeDue", List(RenderableStringMessage(liability.get.amount toString()), RenderableStringMessage(liability.get.dueDate.toString())))
      case None => None
    }
  }

  private def addLiabilityMessageIfApplicable(liability: Option[Liability], msgs: Seq[(String, List[RenderableMessage])], alternativeMsg: Option[(String, List[RenderableMessage])]): Seq[(String, List[RenderableMessage])] = {
    val liabilityMessage = getLiabilityMessage(accountSummary.nextPayment)
    liabilityMessage match {
      case Some(message) => msgs ++ liabilityMessage
      case _ => {
        alternativeMsg match {
          case Some(alternativeMsg) => msgs ++ Seq(alternativeMsg)
          case None => msgs
        }
      }
    }
  }

}