package controllers.bt.regimeViews

import ct.CtConnector
import uk.gov.hmrc.common.microservice.sa.SaConnector
import uk.gov.hmrc.common.microservice.vat.VatConnector
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.epaye.EPayeConnector

class AccountSummariesFactory(saConnector : SaConnector, vatConnector : VatConnector, ctConnector : CtConnector, epayeConnector : EPayeConnector){

  private [regimeViews] val saRegimeAccountSummaryViewBuilder = SaAccountSummaryBuilder(saConnector)
  private [regimeViews] val vatRegimeAccountSummaryViewBuilder = VatAccountSummaryBuilder(vatConnector)
  private [regimeViews] val ctRegimeAccountSummaryViewBuilder = CtAccountSummaryBuilder(ctConnector)
  private [regimeViews] val epayeRegimeAccountSummaryViewBuilder = EPayeAccountSummaryBuilder(epayeConnector)

  def create(buildPortalUrl  : (String) => String)(implicit user : User) : AccountSummaries = {
    val saRegime = saRegimeAccountSummaryViewBuilder.build(buildPortalUrl, user)
    val vatRegime = vatRegimeAccountSummaryViewBuilder.build(buildPortalUrl, user)
    val ctRegime = ctRegimeAccountSummaryViewBuilder.build(buildPortalUrl, user)
    val epayeRegime = epayeRegimeAccountSummaryViewBuilder.build(buildPortalUrl, user)
    new AccountSummaries(Seq(saRegime, vatRegime, ctRegime, epayeRegime).flatten)
  }
}
