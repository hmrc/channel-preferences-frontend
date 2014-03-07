package uk.gov.hmrc.common.microservice.sa.domain

import uk.gov.hmrc.common.microservice.domain.{RegimeRoot, TaxRegime}
import uk.gov.hmrc.common.microservice.auth.domain.Accounts
import controllers.common.{GovernmentGateway, FrontEndRedirect}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.common.microservice.sa.SaConnector
import uk.gov.hmrc.common.microservice.sa.domain.write.TransactionId
import org.joda.time.LocalDate
import scala.concurrent._
import uk.gov.hmrc.common.MdcLoggingExecutionContext.fromLoggingDetails
import controllers.common.actions.HeaderCarrier

object SaRegime extends TaxRegime {

  def isAuthorised(accounts: Accounts) = accounts.sa.isDefined

  def unauthorisedLandingPage = FrontEndRedirect.businessTaxHome

  def authenticationType = GovernmentGateway
}

object SaRoot {
  def apply(utr: SaUtr, root: SaJsonRoot) = new SaRoot(utr, root.links)
}

case class SaJsonRoot(links: Map[String, String])

case class SaRoot(utr: SaUtr, links: Map[String, String]) extends RegimeRoot[SaUtr] {

  private val individualDetailsKey = "individual/details"
  private val individualAccountSummaryKey = "individual/account-summary"

  override val identifier = utr

  def personalDetails(implicit saConnector: SaConnector, hc: HeaderCarrier): Future[Option[SaPerson]] =
    links.get(individualDetailsKey) match {
      case Some(uri) => saConnector.person(uri)
      case _ => Future.successful(None)
    }

  def accountSummary(implicit saConnector: SaConnector, hc: HeaderCarrier): Future[Option[SaAccountSummary]] =
    links.get(individualAccountSummaryKey).map { uri =>
      saConnector.accountSummary(uri) map {
        case None => throw new IllegalStateException(s"Expected HOD data not found for link '$individualAccountSummaryKey' with path: $uri")
        case summary => summary
      }
    }.getOrElse(Future.successful(None))
}

case class SaPerson(name: SaName, address: SaIndividualAddress)

case class SaIndividualAddress(
                                addressLine1: String,
                                addressLine2: String,
                                addressLine3: Option[String],
                                addressLine4: Option[String],
                                addressLine5: Option[String],
                                postcode: Option[String],
                                foreignCountry: Option[String],
                                additionalDeliveryInformation: Option[String])

case class SaName(
                   title: String,
                   forename: String,
                   secondForename: Option[String],
                   surname: String,
                   honours: Option[String])

case class Liability(dueDate: LocalDate, amount: BigDecimal)

case class AmountDue(amount: BigDecimal, requiresPayment: Boolean)

case class SaAccountSummary(totalAmountDueToHmrc: Option[AmountDue], nextPayment: Option[Liability], amountHmrcOwe: Option[BigDecimal])

