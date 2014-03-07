package controllers.bt.accountsummary

import uk.gov.hmrc.common.microservice.domain.RegimeRoot
import uk.gov.hmrc.domain.TaxIdentifier
import uk.gov.hmrc.common.microservice.domain.User
import controllers.common.actions.HeaderCarrier
import scala.concurrent._
import uk.gov.hmrc.common.microservice.auth.domain.Account


abstract class AccountSummaryBuilder[I <: TaxIdentifier, R <: RegimeRoot[I], A <: Account] {

  def build(buildPortalUrl: String => String, user: User)(implicit hc: HeaderCarrier): Option[Future[AccountSummary]] = {

    accountForRegime(user).map {
      account =>
        try {
          val regimeRoot = rootForRegime(user).get
          buildAccountSummary(regimeRoot, buildPortalUrl)
        } catch {
          case e: Throwable => oops(user)
        }
    }
  }

  private def oops(user: User): Future[AccountSummary] = {
    Future.successful(AccountSummary(
      regimeName = defaultRegimeNameMessageKey,
      manageRegimeMessage= defaultManageRegimeMessageKey,
      messages = Seq(Msg(CommonBusinessMessageKeys.oopsMessage)),
      addenda = Seq.empty,
      status = SummaryStatus.oops))
  }

  protected def buildAccountSummary(regimeRoot: R, buildPortalUrl: String => String)(implicit headerCarrier: HeaderCarrier): Future[AccountSummary]

  protected def defaultRegimeNameMessageKey: String

  protected def defaultManageRegimeMessageKey: String

  protected def rootForRegime(user: User): Option[R]

  protected def accountForRegime(user: User): Option[A]
}

object CommonBusinessMessageKeys {
//  val viewAccountDetailsLinkMessage = "common.link.message.accountSummary.viewAccountDetails"
//  val makeAPaymentLinkMessage = "common.link.message.accountSummary.makeAPayment"
//  val fileAReturnLinkMessage = "common.link.message.accountSummary.fileAReturn"
  val oopsMessage = "common.message.oops"
}