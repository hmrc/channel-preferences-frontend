package controllers.bt.regimeViews

import controllers.bt.AccountSummary
import uk.gov.hmrc.common.microservice.domain.{User, RegimeRoots}
import views.helpers.RenderableMessage

abstract class AccountSummaryTemplate[T] {

  def build(regimeRoots: RegimeRoots, buildPortalUrl: String => String) : AccountSummary = {
      val hodAccountSummary: Option[T] = regimeAccountSummary(regimeRoots)
      AccountSummary(regimeTitle, messages(hodAccountSummary), links(buildPortalUrl))
  }

  def regimeAccountSummary(userRegimes: RegimeRoots) : Option[T]
  def messages(regimeModel: Option[T]) : Seq[(String, Seq[RenderableMessage])]
  def links(buildPortalUrl: String => String) : Seq[RenderableMessage]
  def regimeTitle : String
}

object CommonLinkTextKeys {
  val viewAccountDetailsLink = "common.accountSummary.message.link.viewAccountDetails"
  val makeAPaymentLink = "common.accountSummary.message.link.makeAPayment"
  val fileAReturnLink = "common.accountSummary.message.link.fileAReturn"
}
