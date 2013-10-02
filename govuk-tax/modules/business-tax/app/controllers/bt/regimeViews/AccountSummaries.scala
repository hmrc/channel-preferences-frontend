package controllers.bt.regimeViews

import views.helpers.RenderableMessage

case class AccountSummaries(regimes: Seq[AccountSummary])

case class AccountSummary(regimeName: String, messages: Seq[Msg], addenda: Seq[RenderableMessage])

case class Msg(messageKey: String, params: Seq[RenderableMessage] = Seq.empty)


