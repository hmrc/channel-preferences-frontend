package controllers.bt.regimeViews

import views.helpers.{RenderableStringMessage, RenderableMessage, LinkMessage}
import controllers.bt.AccountSummary
import uk.gov.hmrc.microservice.sa.domain.{SaAccountSummary, Liability, SaRoot}
import uk.gov.hmrc.microservice.sa.SaMicroService
import uk.gov.hmrc.microservice.domain.User

case class SaAccountSummaryViewBuilder(buildPortalUrl: String => String, user: User, saMicroService: SaMicroService) {

  def build(): Option[AccountSummary] = {

    user.regimes.sa.map {
      saRoot => getAccountSummaryData(saRoot, saMicroService) match {
        case Some(saAccountSummary) => {
          AccountSummary(
            "SA",
            SaAccountSummaryMessagesBuilder(saAccountSummary).build(),
            Seq(LinkMessage(buildPortalUrl("saViewAccountDetails"), "sa.message.links.view-account-details"), LinkMessage(buildPortalUrl("saFileAReturn"), "sa.message.links.file-a-return"))
          )
        }
        case _ => AccountSummary("SA", Seq(("sa.message.unable-to-display-account", List.empty)), Seq.empty)
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
          ("sa.message.you-have-overpaid", List.empty),
          ("sa.message.amount-due-for-repayment", List(RenderableStringMessage(amountHmrcOwe.toString())))
        )
        addLiabilityMessageIfApplicable(accountSummary.nextPayment, msgs, Some(("sa.message.view-history", List.empty)))
      }
      case _ => {
        accountSummary.totalAmountDueToHmrc match {
          case Some(totalAmountDueToHmrc) => {
            val msgs = totalAmountDueToHmrc.requiresPayment match {
              case true =>
                Seq(
                  ("sa.message.amount-due-for-payment", List(RenderableStringMessage(totalAmountDueToHmrc.amount.toString()))),
                  ("sa.message.interest-applicable", List.empty)
                )
              case false if totalAmountDueToHmrc.amount == BigDecimal(0) => {
                Seq(
                  ("sa.message.nothing-to-pay", List.empty),
                  ("sa.message.view-history", List.empty)
                )
              }
              case false => {
                Seq(
                  ("sa.message.amount-due-for-payment", List(RenderableStringMessage(totalAmountDueToHmrc.amount.toString()))),
                  ("sa.message.small-amount-to-pay", List.empty)
                )
              }
            }
            addLiabilityMessageIfApplicable(accountSummary.nextPayment, msgs, None)
          }
          case _ => {
            val msgs = Seq(
              ("sa.message.nothing-to-pay", List.empty)
            )
            addLiabilityMessageIfApplicable(accountSummary.nextPayment, msgs, Some(("sa.message.view-history", List.empty)))
          }
        }
      }

    }
    messages
  }

  private def getLiabilityMessage(liability: Option[Liability]): Option[(String, List[RenderableMessage])] = {
    liability match {
      case Some(l) => Some("sa.message.will-become-due", List(RenderableStringMessage(liability.get.amount toString()), RenderableStringMessage(liability.get.dueDate.toString())))
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