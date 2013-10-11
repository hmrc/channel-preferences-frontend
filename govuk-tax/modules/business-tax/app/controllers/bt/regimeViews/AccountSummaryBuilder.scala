package controllers.bt.regimeViews

import uk.gov.hmrc.common.microservice.domain.RegimeRoot
import scala.util.Try
import uk.gov.hmrc.domain.TaxIdentifier
import scala.util.Failure
import uk.gov.hmrc.common.microservice.domain.User
import scala.util.Success

abstract class AccountSummaryBuilder[I <: TaxIdentifier, R <: RegimeRoot[I]] {

  def build(buildPortalUrl: String => String, user: User): Option[AccountSummary] = {
    rootForRegime(user).map {
      case Failure(_) => oops(user)
      case Success(regimeRoot) => try {
        buildAccountSummary(regimeRoot, buildPortalUrl)
      } catch {
        case e: Throwable => oops(user)
      }
    }
  }

  private def oops(user: User): AccountSummary = {
    AccountSummary(defaultRegimeNameMessageKey, Seq(Msg(CommonBusinessMessageKeys.oopsMessage)), Seq.empty, SummaryStatus.oops)
  }

  protected def buildAccountSummary(regimeRoot: R, buildPortalUrl: String => String): AccountSummary

  protected def defaultRegimeNameMessageKey: String

  protected def rootForRegime(user: User): Option[Try[R]]
}

object CommonBusinessMessageKeys {
  val viewAccountDetailsLinkMessage = "common.link.message.accountSummary.viewAccountDetails"
  val makeAPaymentLinkMessage = "common.link.message.accountSummary.makeAPayment"
  val fileAReturnLinkMessage = "common.link.message.accountSummary.fileAReturn"
  val oopsMessage = "common.message.oops"
}