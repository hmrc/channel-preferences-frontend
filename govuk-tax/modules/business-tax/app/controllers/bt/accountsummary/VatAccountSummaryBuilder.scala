package controllers.bt.accountsummary

import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.{VatAccountSummary, VatRoot}
import views.helpers.{MoneyPounds, RenderableMessage, LinkMessage}
import controllers.bt.routes
import uk.gov.hmrc.common.microservice.vat.VatConnector
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.domain.Vrn

case class VatAccountSummaryBuilder(vatConnector: VatConnector = new VatConnector) extends AccountSummaryBuilder[Vrn, VatRoot] {

  import CommonBusinessMessageKeys._
  import VatMessageKeys._
  import VatPortalUrls._

  def rootForRegime(user: User): Option[VatRoot] = user.regimes.vat

  def buildAccountSummary(vatRoot: VatRoot, buildPortalUrl: String => String): AccountSummary = {
    val accountSummary: Option[VatAccountSummary] = vatRoot.accountSummary(vatConnector)


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
          Msg(vatSummaryUnavailableErrorMessage4, Seq(LinkMessage.portalLink(buildPortalUrl(vatHelpDeskPortalUrl), vatHelpDeskLinkMessage))))
        AccountSummary(vatRegimeNameMessage, messages, Seq.empty, SummaryStatus.default)
      }
    }
  }

  private def successLinks(buildPortalUrl: (String) => String): Seq[RenderableMessage] = {
    val makeAPaymentUri = routes.VatController.makeAPayment().url
    Seq[RenderableMessage](
      LinkMessage.portalLink(buildPortalUrl(vatAccountDetailsPortalUrl), viewAccountDetailsLinkMessage),
      LinkMessage.internalLink(makeAPaymentUri, makeAPaymentLinkMessage),
      LinkMessage.portalLink(buildPortalUrl(vatFileAReturnPortalUrl), fileAReturnLinkMessage)
    )
  }

  override protected val defaultRegimeNameMessageKey = vatRegimeNameMessage
}

object VatPortalUrls {
  val vatAccountDetailsPortalUrl = "vatAccountDetails"
  val vatFileAReturnPortalUrl = "vatFileAReturn"
  val vatHelpDeskPortalUrl = "vatHelpDesk" // TODO [JJS] WHAT'S THE CORRECT HELP DESK LINK - was set to "/TODO/HelpDeskLink"
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
