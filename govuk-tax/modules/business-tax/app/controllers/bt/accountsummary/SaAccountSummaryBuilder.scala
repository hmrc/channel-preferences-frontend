package controllers.bt.accountsummary

import uk.gov.hmrc.common.microservice.sa.SaConnector
import CommonBusinessMessageKeys._
import SaMessageKeys._
import SaPortalUrlKeys._
import uk.gov.hmrc.common.microservice.domain.User
import views.helpers.{MoneyPounds, RenderableMessage, LinkMessage}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.common.microservice.sa.domain.{Liability, SaAccountSummary, SaRoot}
import controllers.common.actions.HeaderCarrier
import scala.concurrent._
import ExecutionContext.Implicits.global
import controllers.bt.routes


class SaAccountSummaryBuilder(saConnector: SaConnector = new SaConnector)
  extends AccountSummaryBuilder[SaUtr, SaRoot] {
  def rootForRegime(user: User): Option[SaRoot] = user.regimes.sa

  override protected val defaultRegimeNameMessageKey = saRegimeName

  def buildAccountSummary(saRoot: SaRoot, bpu: String => String)(implicit hc: HeaderCarrier): Future[AccountSummary] = {
    val builder = new SaASBuild {
      val saPaymentUrl = routes.PaymentController.makeSaPayment.url
      def buildPortalUrl(s:String) = bpu(s)
    }
    saRoot.accountSummary(saConnector, hc).map(builder.build(_, saRoot.identifier))
  }
}

trait SaASBuild {
  def buildPortalUrl(s: String): String

  def saPaymentUrl: String

  def build(os: Option[SaAccountSummary], utr: SaUtr): AccountSummary = os match {
    case Some(s) => makeSummary(s, utr)
    case _ => unavailable(utr)
  }

  def utrMessage(utr: SaUtr) = Msg(saUtrMessage, Seq(utr.utr))

  def makeSummary(saSummary: SaAccountSummary, utr: SaUtr) = AccountSummary(
    regimeName = saRegimeName,
    messages = utrMessage(utr) +: buildMessages(saSummary),
    addenda = Seq(
      LinkMessage.portalLink(buildPortalUrl(saHomePortalUrl), viewAccountDetailsLinkMessage, "saAccountDetailsHref"),
      LinkMessage.internalLink(saPaymentUrl, makeAPaymentLinkMessage, "saMakePaymentHref"),
      LinkMessage.portalLink(buildPortalUrl(saHomePortalUrl), fileAReturnLinkMessage, "saFileReturnHref")),
    status = SummaryStatus.success
  )

  def unavailable(utr: SaUtr) =
    AccountSummary(regimeName = saRegimeName,
      messages = utrMessage(utr) +: Seq(
        Msg(saSummaryUnavailableErrorMessage1),
        Msg(saSummaryUnavailableErrorMessage2),
        Msg(saSummaryUnavailableErrorMessage3),
        Msg(saSummaryUnavailableErrorMessage4)
      ),
      addenda = Seq.empty,
      status = SummaryStatus.default
    )


  def buildMessages(saSummary: SaAccountSummary): Seq[Msg] = {

    val messages = saSummary.amountHmrcOwe match {

      case Some(amountHmrcOwe) if amountHmrcOwe > 0 => {

        addLiabilityMessageIfApplicable(
          liability = saSummary.nextPayment,
          msgs = Seq(Msg(saYouHaveOverpaidMessage), Msg(saAmountDueForRepaymentMessage, Seq(MoneyPounds(amountHmrcOwe)))),
          alternativeMsg = None)
      }
      case _ => {
        saSummary.totalAmountDueToHmrc match {
          case Some(totalAmountDueToHmrc) => {
            val msgs = totalAmountDueToHmrc.requiresPayment match {
              case true =>
                Seq(
                  Msg(saAmountDueForPaymentMessage, Seq(MoneyPounds(totalAmountDueToHmrc.amount))),
                  Msg(saInterestApplicableMessage)
                )
              case false if totalAmountDueToHmrc.amount == BigDecimal(0) => {
                Seq(
                  Msg(saNothingToPayMessage)
                )
              }
              case false => {
                Seq(
                  Msg(saAmountDueForPaymentMessage, Seq[RenderableMessage](MoneyPounds(totalAmountDueToHmrc.amount))),
                  Msg(saSmallAmountToPayMessage)
                )
              }
            }
            addLiabilityMessageIfApplicable(saSummary.nextPayment, msgs, None)
          }
          case _ => {
            val msgs = Seq(
              Msg(saNothingToPayMessage)
            )
            addLiabilityMessageIfApplicable(saSummary.nextPayment, msgs, None)
          }
        }
      }

    }
    messages
  }

  def getLiabilityMessage(liability: Option[Liability]): Option[Msg] = {
    liability match {
      case Some(l) => Some(Msg(saWillBecomeDueMessage, Seq(MoneyPounds(liability.get.amount), liability.get.dueDate)))
      case None => None
    }
  }

  def addLiabilityMessageIfApplicable(liability: Option[Liability], msgs: Seq[Msg], alternativeMsg: Option[Msg]): Seq[Msg] = {
    val liabilityMessage = getLiabilityMessage(liability)
    liabilityMessage match {
      case Some(message) => msgs ++ liabilityMessage
      case _ => {
        alternativeMsg match {
          case Some(altMsg) => msgs ++ Seq(altMsg)
          case None => msgs
        }
      }
    }
  }

}

object SaPortalUrlKeys {
  val saHomePortalUrl = "home"
}

object SaMessageKeys {

  val saRegimeName = "sa.regimeName"

  val saUtrMessage = "sa.message.utr"
  val saNothingToPayMessage = "sa.message.nothingToPay"
  val saAmountDueForPaymentMessage = "sa.message.amountDueForPayment"
  val saInterestApplicableMessage = "sa.message.interestApplicable"
  val saWillBecomeDueMessage = "sa.message.willBecomeDue"
  val saYouHaveOverpaidMessage = "sa.message.youHaveOverpaid"
  val saAmountDueForRepaymentMessage = "sa.message.amountDueForRepayment"
  val saSmallAmountToPayMessage = "sa.message.smallAmountToPay"
  val saSummaryUnavailableErrorMessage1 = "sa.message.summaryUnavailable.1"
  val saSummaryUnavailableErrorMessage2 = "sa.message.summaryUnavailable.2"
  val saSummaryUnavailableErrorMessage3 = "sa.message.summaryUnavailable.3"
  val saSummaryUnavailableErrorMessage4 = "sa.message.summaryUnavailable.4"
}