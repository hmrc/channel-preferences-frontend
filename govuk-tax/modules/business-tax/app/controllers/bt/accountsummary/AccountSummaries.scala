package controllers.bt.accountsummary

import views.helpers.RenderableMessage
import controllers.bt.accountsummary.SummaryStatus.SummaryStatus

object SummaryStatus extends Enumeration {
  type SummaryStatus = Value
  val success, default, oops = Value
}

case class AccountSummaries(regimes: Seq[AccountSummary])

case class AccountSummary(regimeName: String, manageRegimeMessage:String,  messages: Seq[Msg], addenda: Seq[AccountSummaryLink], status: SummaryStatus)

case class Msg(messageKey: String, params: Seq[RenderableMessage] = Seq.empty)

case class AccountSummaryLink(id: String, url: String, text: String, sso: Boolean, newWindow: Boolean = false)


