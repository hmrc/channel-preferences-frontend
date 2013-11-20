package controllers.bt.accountsummary

import uk.gov.hmrc.common.microservice.domain.User
import scala.concurrent._
import ExecutionContext.Implicits.global
import controllers.common.actions.HeaderCarrier

class AccountSummariesFactory(saRegimeAccountSummaryViewBuilder: SaAccountSummaryBuilder = new SaAccountSummaryBuilder,
                              vatRegimeAccountSummaryViewBuilder: VatAccountSummaryBuilder = new VatAccountSummaryBuilder,
                              ctRegimeAccountSummaryViewBuilder: CtAccountSummaryBuilder = new CtAccountSummaryBuilder,
                              epayeRegimeAccountSummaryViewBuilder: EpayeAccountSummaryBuilder = new EpayeAccountSummaryBuilder) {

  def create(buildPortalUrl: (String) => String)(implicit user: User, headerCarrier:HeaderCarrier): Future[AccountSummaries] = {
    val saRegimeF = Future {saRegimeAccountSummaryViewBuilder.build(buildPortalUrl, user)}
    val vatRegimeF = Future {vatRegimeAccountSummaryViewBuilder.build(buildPortalUrl, user)}
    val ctRegimeF = Future {ctRegimeAccountSummaryViewBuilder.build(buildPortalUrl, user)}
    val epayeRegimeF = Future {epayeRegimeAccountSummaryViewBuilder.build(buildPortalUrl, user)}

    for {
      saRegime <- saRegimeF
      vatRegime <- vatRegimeF
      ctRegime <- ctRegimeF
      epayeRegime <- epayeRegimeF
    } yield AccountSummaries(List(saRegime, ctRegime, vatRegime, epayeRegime).flatten)
  }
}
