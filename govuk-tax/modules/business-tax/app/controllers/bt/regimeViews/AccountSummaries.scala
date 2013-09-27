package controllers.bt.regimeViews

import views.helpers.RenderableMessage

case class AccountSummaries(regimes: Seq[AccountSummary])
case class AccountSummary(regimeName: String, messages: Seq[(String, Seq[RenderableMessage])], addenda: Seq[RenderableMessage])

