package controllers.bt.regimeViews

import uk.gov.hmrc.common.microservice.domain.RegimeRoots
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

trait CommonBusinessMessageKeys {
  val viewAccountDetailsLinkMessage = "common.link.message.accountSummary.viewAccountDetails"
  val makeAPaymentLinkMessage = "common.link.message.accountSummary.makeAPayment"
  val fileAReturnLinkMessage = "common.link.message.accountSummary.fileAReturn"
}
