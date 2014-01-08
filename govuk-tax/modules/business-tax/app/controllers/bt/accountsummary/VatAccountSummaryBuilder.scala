package controllers.bt.accountsummary

import views.helpers.MoneyPounds
import controllers.bt.routes
import uk.gov.hmrc.common.microservice.vat.VatConnector
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.common.microservice.vat.domain.{VatAccountSummary, VatRoot}
import controllers.common.actions.HeaderCarrier
import scala.concurrent._
import uk.gov.hmrc.common.MdcLoggingExecutionContext.fromLoggingDetails

case class VatAccountSummaryBuilder(vatConnector: VatConnector = new VatConnector) extends AccountSummaryBuilder[Vrn, VatRoot] {

  import CommonBusinessMessageKeys._
  import VatMessageKeys._
  import VatPortalUrls._

  def rootForRegime(user: User): Option[VatRoot] = user.regimes.vat

  def buildAccountSummary(vatRoot: VatRoot, buildPortalUrl: String => String)(implicit headerCarrier: HeaderCarrier): Future[AccountSummary] = {
    val accountSummaryF: Future[Option[VatAccountSummary]] = vatRoot.accountSummary(vatConnector, headerCarrier)

    accountSummaryF.map { accountSummary =>
      val accountValueOption: Option[BigDecimal] = for {
        accountSummaryValue <- accountSummary
        accountBalance <- accountSummaryValue.accountBalance
        amount <- accountBalance.amount
      } yield amount

      accountValueOption match {
        case Some(accountValue) => {
          val links = successLinks(buildPortalUrl)
          val messages = Seq(Msg(vatRegistrationNumberMessage, Seq(vatRoot.identifier.vrn)), Msg(vatAccountBalanceMessage, Seq(MoneyPounds(accountValue))))
          AccountSummary(vatRegimeNameMessage, messages, links, SummaryStatus.success)
        }
        case _ => {
          val messages = Seq(Msg(vatRegistrationNumberMessage, Seq(vatRoot.identifier.vrn)), Msg(vatSummaryUnavailableErrorMessage1), Msg(vatSummaryUnavailableErrorMessage2),
            Msg(vatSummaryUnavailableErrorMessage3),
            //TODO: To be updated once the customer support model has been finalised (see: HMTB-1914)
            Msg(vatSummaryUnavailableErrorMessage4))
          AccountSummary(vatRegimeNameMessage, messages, Seq.empty, SummaryStatus.default)
        }
      }
    }
  }

  private def successLinks(buildPortalUrl: (String) => String): Seq[AccountSummaryLink] = {
    val makeAPaymentUri = routes.PaymentController.makeVatPayment().url
    Seq[AccountSummaryLink](
      AccountSummaryLink("vat-account-details-href", buildPortalUrl(vatAccountDetailsPortalUrl), viewAccountDetailsLinkMessage, sso = true),
      AccountSummaryLink("vat-make-payment-href", makeAPaymentUri, makeAPaymentLinkMessage, sso = false),
      AccountSummaryLink("vat-file-return-href", buildPortalUrl(vatFileAReturnPortalUrl), fileAReturnLinkMessage, sso = true)
    )
  }

  override protected val defaultRegimeNameMessageKey = vatRegimeNameMessage
}

object VatPortalUrls {
  val vatAccountDetailsPortalUrl = "vatAccountDetails"
  val vatFileAReturnPortalUrl = "vatFileAReturn"
}

object VatMessageKeys {

  val vatRegimeNameMessage = "vat.regimeName"

  val vatRegistrationNumberMessage = "vat.message.registrationNumber"
  val vatAccountBalanceMessage = "vat.message.accountBalance"
  val vatHelpDeskLinkMessage = "vat.link.message.accountSummary.helpDesk"

  val vatSummaryUnavailableErrorMessage1 = "vat.message.summaryUnavailable.1"
  val vatSummaryUnavailableErrorMessage2 = "vat.message.summaryUnavailable.2"
  val vatSummaryUnavailableErrorMessage3 = "vat.message.summaryUnavailable.3"
  val vatSummaryUnavailableErrorMessage4 = "vat.message.summaryUnavailable.4"
}
