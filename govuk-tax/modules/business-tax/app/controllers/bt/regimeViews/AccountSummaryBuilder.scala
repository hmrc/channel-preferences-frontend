package controllers.bt.regimeViews

import uk.gov.hmrc.common.microservice.domain.{User, RegimeRoot}
import scala.util.{Success, Failure, Try}
import uk.gov.hmrc.domain.TaxIdentifier

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

  protected def buildAccountSummary(regimeRoot: R, buildPortalUrl: String => String): AccountSummary

  protected def oops(user: User): AccountSummary

  protected def rootForRegime(user: User): Option[Try[R]]
}

trait CommonBusinessMessageKeys {
  val viewAccountDetailsLinkMessage = "common.link.message.accountSummary.viewAccountDetails"
  val makeAPaymentLinkMessage = "common.link.message.accountSummary.makeAPayment"
  val fileAReturnLinkMessage = "common.link.message.accountSummary.fileAReturn"
}
