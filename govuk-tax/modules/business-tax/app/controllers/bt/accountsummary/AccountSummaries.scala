package controllers.bt.accountsummary

import views.helpers.{Link, RenderableMessage}
import controllers.bt.accountsummary.SummaryStatus.SummaryStatus

object SummaryStatus extends Enumeration {
  type SummaryStatus = Value
  val success, default, oops = Value
}

case class AccountSummaries(regimes: Seq[AccountSummary])

case class AccountSummary(regimeName: String, manageRegimeMessage:String,  messages: Seq[Msg], addenda: Seq[Link], status: SummaryStatus)

case class Msg(messageKey: String, params: Seq[RenderableMessage] = Seq.empty)


