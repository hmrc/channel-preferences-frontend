package controllers.bt.regimeViews

import views.helpers.{RenderableLinkMessage, RenderableStringMessage }
import views.helpers.LinkMessage
import controllers.bt.AccountSummary
import uk.gov.hmrc.microservice.sa.SaMicroService

case class SaAccountSummaryViewBuilder(buildPortalUrl: String => String, data: String, saMicroService: SaMicroService) {
  implicit def translateStrings(value: String): RenderableStringMessage = RenderableStringMessage(value)
  implicit def translateLinks(link: LinkMessage): RenderableLinkMessage = RenderableLinkMessage(link)

  def build(): Option[AccountSummary] = {
    val links = Seq[RenderableLinkMessage](LinkMessage(buildPortalUrl("saViewAccountDetails"), "PORTAL: Sa View Account Details"))
    Some(AccountSummary("SA", Seq.empty, links))
  }
}

