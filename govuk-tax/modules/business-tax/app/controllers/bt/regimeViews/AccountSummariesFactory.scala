package controllers.bt.regimeViews

import ct.CtMicroService
import uk.gov.hmrc.common.microservice.sa.SaMicroService
import uk.gov.hmrc.common.microservice.vat.VatMicroService
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.epaye.EPayeConnector

class AccountSummariesFactory(saMicroService : SaMicroService, vatMicroService : VatMicroService, ctMicroService : CtMicroService, epayeConnector : EPayeConnector){

  val saRegimeAccountSummaryViewBuilder = SaAccountSummaryViewBuilder(saMicroService)
  val vatRegimeAccountSummaryViewBuilder = VatAccountSummaryViewBuilder(vatMicroService)
  val ctRegimeAccountSummaryViewBuilder = CtAccountSummaryViewBuilder(ctMicroService)
  val epayeRegimeAccountSummaryViewBuilder = EPayeAccountSummaryViewBuilder(epayeConnector)

  def create(buildPortalUrl  : (String) => String)(implicit user : User) : AccountSummaries = {
    val saRegime = saRegimeAccountSummaryViewBuilder.build(buildPortalUrl, user)
    val vatRegime = vatRegimeAccountSummaryViewBuilder.build(buildPortalUrl, user)
    val ctRegime = ctRegimeAccountSummaryViewBuilder.build(buildPortalUrl, user)
    val epayeRegime = epayeRegimeAccountSummaryViewBuilder.build(buildPortalUrl, user)
    new AccountSummaries(Seq(saRegime, vatRegime, ctRegime, epayeRegime).flatten)
  }
}
