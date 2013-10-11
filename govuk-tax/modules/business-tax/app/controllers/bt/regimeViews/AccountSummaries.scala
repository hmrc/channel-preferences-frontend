package controllers.bt.regimeViews

import views.helpers.RenderableMessage
import controllers.bt.regimeViews.SummaryStatus.SummaryStatus

object SummaryStatus extends Enumeration {
  type SummaryStatus = Value
  val success, default, oops = Value
}


case class AccountSummaries(regimes: Seq[AccountSummary])

case class AccountSummary(regimeName: String, messages: Seq[Msg], addenda: Seq[RenderableMessage], status: SummaryStatus)

case class Msg(messageKey: String, params: Seq[RenderableMessage] = Seq.empty)


