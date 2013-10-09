package controllers.bt.regimeViews

import uk.gov.hmrc.common.microservice.domain.{User, RegimeRoot}
import scala.util.Try

abstract class AccountSummaryBuilder[R <: RegimeRoot[_]] {

  def build(buildPortalUrl: String => String, user: User) : Option[AccountSummary] = {
    rootForRegime(user).map {
      regimeRoot => buildAccountSummary(regimeRoot.get, buildPortalUrl)
    }
  }

  def buildAccountSummary(regimeRoot : R, buildPortalUrl: String => String) : AccountSummary

  // def oops() : AccountSummary

  def rootForRegime(user : User): Option[Try[R]]
}

trait CommonBusinessMessageKeys {
  val viewAccountDetailsLinkMessage = "common.link.message.accountSummary.viewAccountDetails"
  val makeAPaymentLinkMessage = "common.link.message.accountSummary.makeAPayment"
  val fileAReturnLinkMessage = "common.link.message.accountSummary.fileAReturn"
}
