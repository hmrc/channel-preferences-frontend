package controllers.bt.regimeViews

import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.{VatAccountSummary, VatRoot}
import uk.gov.hmrc.microservice.auth.domain.Vrn
import views.helpers.{StringMessage, LinkMessage}
import controllers.bt.AccountSummary
import uk.gov.hmrc.common.microservice.vat.VatMicroService
import uk.gov.hmrc.microservice.sa.domain.SaRoot
import uk.gov.hmrc.microservice.sa.SaMicroService

case class SaAccountSummaryViewBuilder(buildPortalUrl: String => String, data: String, saMicroService: SaMicroService) {
  implicit def translate(value: String): StringMessage = StringMessage(value)

  def build(): Option[AccountSummary] = {
    val links = Seq(LinkMessage(buildPortalUrl("saViewAccountDetails"), "PORTAL: Sa View Account Details"))
    Some(AccountSummary("SA", Seq.empty, links))
  }
}

