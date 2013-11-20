package controllers.bt.accountsummary

import uk.gov.hmrc.common.microservice.domain.RegimeRoot
import uk.gov.hmrc.domain.TaxIdentifier
import uk.gov.hmrc.common.microservice.domain.User
import controllers.common.actions.HeaderCarrier

abstract class AccountSummaryBuilder[I <: TaxIdentifier, R <: RegimeRoot[I]] {

  def build(buildPortalUrl: String => String, user: User)(implicit headerCarrier:HeaderCarrier): Option[AccountSummary] = {
    rootForRegime(user).map {
      regimeRoot => try {
        buildAccountSummary(regimeRoot, buildPortalUrl)
      } catch {
        case e: Throwable => oops(user)
      }
    }
  }

  private def oops(user: User): AccountSummary = {
    AccountSummary(
      regimeName = defaultRegimeNameMessageKey,
      messages = Seq(Msg(CommonBusinessMessageKeys.oopsMessage)),
      addenda = Seq.empty,
      status = SummaryStatus.oops)
  }

  protected def buildAccountSummary(regimeRoot: R, buildPortalUrl: String => String)(implicit headerCarrier:HeaderCarrier): AccountSummary

  protected def defaultRegimeNameMessageKey: String

  protected def rootForRegime(user: User): Option[R]
}

object CommonBusinessMessageKeys {
  val viewAccountDetailsLinkMessage = "common.link.message.accountSummary.viewAccountDetails"
  val makeAPaymentLinkMessage = "common.link.message.accountSummary.makeAPayment"
  val fileAReturnLinkMessage = "common.link.message.accountSummary.fileAReturn"
  val oopsMessage = "common.message.oops"
}