package controllers.bt.accountsummary

import views.helpers.{Link, MoneyPounds}
import controllers.bt.routes
import uk.gov.hmrc.common.microservice.vat.VatConnector
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.common.microservice.vat.domain.{VatAccountSummary, VatRoot}
import controllers.common.actions.HeaderCarrier
import scala.concurrent._
import uk.gov.hmrc.common.MdcLoggingExecutionContext.fromLoggingDetails
import uk.gov.hmrc.common.microservice.auth.domain.VatAccount
import play.api.i18n.Messages

case class VatAccountSummaryBuilder(vatConnector: VatConnector = new VatConnector) extends AccountSummaryBuilder[Vrn, VatRoot, VatAccount] {

  import VatMessageKeys._
  import VatPortalUrls._

  def rootForRegime(user: User): Option[VatRoot] = user.regimes.vat

  def accountForRegime(user: User): Option[VatAccount] = user.userAuthority.accounts.vat

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
          val messages = Seq(Msg(vatAccountBalanceMessage, Seq(MoneyPounds(accountValue))))
          AccountSummary(vatRegimeNameMessage, vatManageHeading, messages, links, SummaryStatus.success)
        }
        case _ => {
          val messages = Seq(Msg(vatSummaryUnavailableErrorMessage1), Msg(vatSummaryUnavailableErrorMessage2),
            Msg(vatSummaryUnavailableErrorMessage3),
            //TODO: To be updated once the customer support model has been finalised (see: HMTB-1914)
            Msg(vatSummaryUnavailableErrorMessage4))
          AccountSummary(vatRegimeNameMessage, vatManageHeading, messages, Seq.empty, SummaryStatus.default)
        }
      }
    }
  }

  private def successLinks(buildPortalUrl: (String) => String): Seq[Link] = {
    val makeAPaymentUri = routes.PaymentController.makeVatPayment().url
    Seq[Link](
      Link.toPortalPage(id = Some("vat-account-details-href"), url = buildPortalUrl(vatAccountDetailsPortalUrl), value = Some(Messages(vatViewAccountDetailsLinkMessage))),
      Link.toInternalPage(id = Some("vat-make-payment-href"), url = makeAPaymentUri, value = Some(Messages(vatMakeAPaymentLinkMessage))),
      Link.toPortalPage(id = Some("vat-file-return-href"), url = buildPortalUrl(vatFileAReturnPortalUrl), value = Some(Messages(vatFileAReturnLinkMessage)))
    )
  }

  override protected val defaultRegimeNameMessageKey = vatRegimeNameMessage
  override protected val defaultManageRegimeMessageKey = vatManageHeading
}

object VatPortalUrls {
  val vatAccountDetailsPortalUrl = "vatAccountDetails"
  val vatFileAReturnPortalUrl = "vatFileAReturn"
}

object VatMessageKeys {

  val vatRegimeNameMessage = "vat.regimeName"

  val vatAccountBalanceMessage = "vat.message.accountBalance"
  val vatHelpDeskLinkMessage = "vat.link.message.accountSummary.helpDesk"

  val vatSummaryUnavailableErrorMessage1 = "vat.message.summaryUnavailable.1"
  val vatSummaryUnavailableErrorMessage2 = "vat.message.summaryUnavailable.2"
  val vatSummaryUnavailableErrorMessage3 = "vat.message.summaryUnavailable.3"
  val vatSummaryUnavailableErrorMessage4 = "vat.message.summaryUnavailable.4"
  val vatManageHeading = "vat.manage.heading"
  val vatViewAccountDetailsLinkMessage = "vat.link.message.accountSummary.viewAccountDetails"
  val vatMakeAPaymentLinkMessage = "vat.link.message.accountSummary.makeAPayment"
  val vatFileAReturnLinkMessage = "vat.link.message.accountSummary.fileAReturn"
}
