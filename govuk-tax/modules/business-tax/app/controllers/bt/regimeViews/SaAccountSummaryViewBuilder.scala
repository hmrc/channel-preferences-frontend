package controllers.bt.regimeViews

import views.helpers.{RenderableMessage, LinkMessage}
import controllers.bt.AccountSummary
import uk.gov.hmrc.microservice.sa.SaMicroService

case class SaAccountSummaryViewBuilder(buildPortalUrl: String => String, data: String, saMicroService: SaMicroService) {

  def build(): Option[AccountSummary] = {
    val links = Seq[RenderableMessage](LinkMessage(buildPortalUrl("saViewAccountDetails"), "PORTAL: Sa View Account Details"))
    Some(AccountSummary("SA", Seq.empty, links))
  }
}

