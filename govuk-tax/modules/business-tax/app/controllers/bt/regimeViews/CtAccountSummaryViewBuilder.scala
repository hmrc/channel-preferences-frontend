package controllers.bt.regimeViews

import ct.CtMicroService
import ct.domain.CtDomain.{CtAccountSummary, CtRoot}
import views.helpers.{MoneyPounds, RenderableMessage, LinkMessage}
import controllers.bt.{AccountSummary, routes}
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.utils.DateConverter

case class CtAccountSummaryViewBuilder(buildPortalUrl: String => String, user: User, ctMicroService: CtMicroService) {

  import CtMessageKeys._
  import CtPortalUrlKeys._

  def build(): Option[AccountSummary] = {
    val ctRootOption: Option[CtRoot] = user.regimes.ct

    ctRootOption.map {
      ctRoot: CtRoot =>
        val accountSummary: Option[CtAccountSummary] = ctRootOption.get.accountSummary(ctMicroService)


        val accountValueOption: Option[BigDecimal] = for {
          accountSummaryValue <- accountSummary
          accountBalance <- accountSummaryValue.accountBalance
          amount <- accountBalance.amount
        } yield amount
      val dateOfBalanceOption:Option[String] = accountSummary flatMap (_.dateOfBalance)

        val makeAPaymentUri = routes.BusinessTaxController.makeAPaymentLanding().url
        val links = Seq[RenderableMessage](
          LinkMessage(buildPortalUrl(accountDetailsPortalUrl), viewAccountDetailsLinkMessage),
          LinkMessage(makeAPaymentUri, makeAPaymentLinkMessage),
          LinkMessage(buildPortalUrl(fileAReturnPortalUrl), fileAReturnLinkMessage)



        )
        (accountValueOption, dateOfBalanceOption)  match {
          case (Some(accountValue), Some(dateOfBalance)) => {
            AccountSummary(regimeNameMessage, Seq(utrMessage -> Seq(user.userAuthority.ctUtr.get.utr),
              amountAsOfDateMessage -> Seq(MoneyPounds(accountValue), DateConverter.parseToLocalDate(dateOfBalance))), links)
          }
          case _ => {
            AccountSummary(regimeNameMessage, Seq(summaryUnavailableErrorMessage1 -> Seq.empty, summaryUnavailableErrorMessage2 -> Seq.empty,
              summaryUnavailableErrorMessage3 -> Seq.empty,
              summaryUnavailableErrorMessage4 -> Seq.empty), Seq.empty)
          }
        }
    }
  }
}

object CtPortalUrlKeys {
  val accountDetailsPortalUrl = "ctAccountDetails"
  val fileAReturnPortalUrl = "ctFileAReturn"
}

object CtMessageKeys extends CommonBusinessMessageKeys {

  val regimeNameMessage = "ct.regimeName"

  val utrMessage = "ct.message.utr"
  val summaryUnavailableErrorMessage1 = "ct.error.message.summaryUnavailable.1"
  val summaryUnavailableErrorMessage2 = "ct.error.message.summaryUnavailable.2"
  val summaryUnavailableErrorMessage3 = "ct.error.message.summaryUnavailable.3"
  val summaryUnavailableErrorMessage4 = "ct.error.message.summaryUnavailable.4"
  val amountAsOfDateMessage = "ct.message.amountAsOfDate"
}
