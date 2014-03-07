package uk.gov.hmrc.common.microservice.ct.domain

import uk.gov.hmrc.common.microservice.domain.{RegimeRoot, TaxRegime}
import uk.gov.hmrc.common.microservice.auth.domain.Accounts
import controllers.common.{GovernmentGateway, FrontEndRedirect}
import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.common.microservice.ct.CtConnector
import scala.concurrent._

import controllers.common.actions.HeaderCarrier

object CtRegime extends TaxRegime {
  def isAuthorised(accounts: Accounts) = accounts.ct.isDefined

  def unauthorisedLandingPage = FrontEndRedirect.businessTaxHome

  def authenticationType = GovernmentGateway
}

object CtRoot {
  def apply(utr: CtUtr, root: CtJsonRoot): CtRoot = new CtRoot(utr, root.links)
}

case class CtJsonRoot(links: Map[String, String])

case class CtRoot(utr: CtUtr, links: Map[String, String]) extends RegimeRoot[CtUtr] {

  import uk.gov.hmrc.common.MdcLoggingExecutionContext.fromLoggingDetails

  private val accountSummaryKey = "accountSummary"

  override val identifier = utr

  def accountSummary(implicit ctConnector: CtConnector, headerCarrier: HeaderCarrier): Future[Option[CtAccountSummary]] = {
    links.get(accountSummaryKey).map { uri =>
      ctConnector.accountSummary(uri).map {
        case None => throw new IllegalStateException(s"Expected HOD data not found for link '$accountSummaryKey' with path: $uri")
        case summary => summary
      }
    }.getOrElse(Future.successful(None))
  }
}

case class CtAccountSummary(accountBalance: Option[CtAccountBalance], dateOfBalance: Option[String])

case class CtAccountBalance(amount: Option[BigDecimal])