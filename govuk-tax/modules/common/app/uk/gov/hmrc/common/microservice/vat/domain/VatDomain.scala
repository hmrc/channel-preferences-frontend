package uk.gov.hmrc.common.microservice.vat.domain

import uk.gov.hmrc.common.microservice.auth.domain.Accounts
import uk.gov.hmrc.common.microservice.domain.{RegimeRoot, TaxRegime}
import controllers.common.{GovernmentGateway, FrontEndRedirect}
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.common.microservice.vat.VatConnector
import controllers.common.actions.HeaderCarrier
import scala.concurrent._
import uk.gov.hmrc.common.MdcLoggingExecutionContext.fromLoggingDetails

object VatRegime extends TaxRegime {
  def isAuthorised(accounts: Accounts) = accounts.vat.isDefined

  def unauthorisedLandingPage = FrontEndRedirect.businessTaxHome

  def authenticationType = GovernmentGateway
}

object VatRoot {
  def apply(vrn: Vrn, root: VatJsonRoot) = new VatRoot(vrn, root.links)
}

case class VatJsonRoot(links: Map[String, String])

case class VatRoot(vrn: Vrn, links: Map[String, String]) extends RegimeRoot[Vrn] {

  private val accountSummaryKey = "accountSummary"

  override val identifier = vrn

  def accountSummary(implicit vatConnector: VatConnector, hc: HeaderCarrier): Future[Option[VatAccountSummary]] = {
    links.get(accountSummaryKey).map { uri =>
      vatConnector.accountSummary(uri) map {
        case None => throw new IllegalStateException(s"Expected HOD data not found for link '$accountSummaryKey' with path: $uri")
        case summary => summary
      }
    }.getOrElse(Future.successful(None))
  }
}

case class VatAccountSummary(accountBalance: Option[VatAccountBalance], dateOfBalance: Option[String])

case class VatAccountBalance(amount: Option[BigDecimal])

