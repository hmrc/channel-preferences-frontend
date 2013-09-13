package controllers.bt.regimeViews

import views.helpers.{RenderableLinkMessage, RenderableStringMessage, LinkMessage}
import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.{ VatAccountSummary, VatRoot }
import uk.gov.hmrc.microservice.auth.domain.Vrn
import views.helpers.{ StringMessage, LinkMessage }
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

