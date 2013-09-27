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

trait CommonBusinessMessageKeys {
  val viewAccountDetailsLinkMessage = "common.accountSummary.message.link.viewAccountDetails"
  val makeAPaymentLinkMessage = "common.accountSummary.message.link.makeAPayment"
  val fileAReturnLinkMessage = "common.accountSummary.message.link.fileAReturn"
}
