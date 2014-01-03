package controllers.bt.accountsummary

import uk.gov.hmrc.common.microservice.domain.User
import scala.concurrent._
import uk.gov.hmrc.common.StickyMdcExecutionContext.global
import controllers.common.actions.HeaderCarrier

class AccountSummariesFactory(saRegimeAccountSummaryViewBuilder: SaAccountSummaryBuilder = new SaAccountSummaryBuilder,
                              vatRegimeAccountSummaryViewBuilder: VatAccountSummaryBuilder = new VatAccountSummaryBuilder,
                              ctRegimeAccountSummaryViewBuilder: CtAccountSummaryBuilder = new CtAccountSummaryBuilder,
                              epayeRegimeAccountSummaryViewBuilder: EpayeAccountSummaryBuilder = new EpayeAccountSummaryBuilder) {

  def create(buildPortalUrl: (String) => String)(implicit user: User, headerCarrier: HeaderCarrier): Future[AccountSummaries] = {
    val sa = saRegimeAccountSummaryViewBuilder.build(buildPortalUrl, user)

    val ct = ctRegimeAccountSummaryViewBuilder.build(buildPortalUrl, user)
    val vat = vatRegimeAccountSummaryViewBuilder.build(buildPortalUrl, user)
    val epaye = epayeRegimeAccountSummaryViewBuilder.build(buildPortalUrl, user)

    val regimes = Future.sequence(List(sa, ct, vat, epaye).flatten)

    regimes.map(AccountSummaries)
  }
}
