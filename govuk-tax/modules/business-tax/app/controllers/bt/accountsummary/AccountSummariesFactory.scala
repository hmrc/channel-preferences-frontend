package controllers.bt.accountsummary

import uk.gov.hmrc.common.microservice.domain.User
import scala.concurrent._
import ExecutionContext.Implicits.global
import controllers.common.actions.HeaderCarrier

class AccountSummariesFactory(saRegimeAccountSummaryViewBuilder: SaAccountSummaryBuilder = new SaAccountSummaryBuilder,
                              vatRegimeAccountSummaryViewBuilder: VatAccountSummaryBuilder = new VatAccountSummaryBuilder,
                              ctRegimeAccountSummaryViewBuilder: CtAccountSummaryBuilder = new CtAccountSummaryBuilder,
                              epayeRegimeAccountSummaryViewBuilder: EpayeAccountSummaryBuilder = new EpayeAccountSummaryBuilder) {

  def create(buildPortalUrl: (String) => String)(implicit user: User, headerCarrier: HeaderCarrier): Future[AccountSummaries] = {


    val saRegimeO = saRegimeAccountSummaryViewBuilder.build(buildPortalUrl, user)
    val vatRegimeO = vatRegimeAccountSummaryViewBuilder.build(buildPortalUrl, user)
    val ctRegimeO = ctRegimeAccountSummaryViewBuilder.build(buildPortalUrl, user)
    val epayeRegimeO = epayeRegimeAccountSummaryViewBuilder.build(buildPortalUrl, user)

    val regimes = Future.sequence(List(saRegimeO, vatRegimeO, ctRegimeO, epayeRegimeO).flatten)

    regimes.map(AccountSummaries)
  }
}
