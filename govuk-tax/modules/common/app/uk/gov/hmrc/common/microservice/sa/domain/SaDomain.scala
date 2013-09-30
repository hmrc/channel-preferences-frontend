package uk.gov.hmrc.common.microservice.sa.domain

import controllers.common.FrontEndRedirect
import uk.gov.hmrc.common.microservice.sa.SaConnector
import uk.gov.hmrc.common.microservice.domain.{ TaxRegime, RegimeRoot }
import uk.gov.hmrc.common.microservice.auth.domain.Regimes
import uk.gov.hmrc.common.microservice.sa.domain.write.SaAddressForUpdate
import org.joda.time.LocalDate
import uk.gov.hmrc.domain.SaUtr

object SaRegime extends TaxRegime {
  override def isAuthorised(regimes: Regimes) = {
    regimes.sa.isDefined
  }

  override def unauthorisedLandingPage: String = {
    FrontEndRedirect.businessTaxHome
  }
}

case class SaRoot(utr: String, links: Map[String, String]) extends RegimeRoot[SaUtr] {

  private val individualDetailsKey = "individual/details"
  private val individualMainAddressKey = "individual/details/main-address"
  private val individualAccountSummaryKey = "individual/account-summary"

  def identifier = SaUtr(utr)
  def personalDetails(implicit saConnector: SaConnector): Option[SaPerson] = {
    links.get(individualDetailsKey) match {
      case Some(uri) => saConnector.person(uri)
      case _ => None
    }
  }

  def accountSummary(implicit saConnector: SaConnector): Option[SaAccountSummary] = {
    links.get(individualAccountSummaryKey) match {
      case Some(uri) => saConnector.accountSummary(uri)
      case _ => None
    }
  }

  def updateIndividualMainAddress(address: SaAddressForUpdate)(implicit saConnector: SaConnector): Either[String, TransactionId] = {
    saConnector.updateMainAddress(uriFor(individualMainAddressKey), address)
  }

  private def uriFor(key: String): String = {
    links.getOrElse(key, throw new IllegalStateException("Missing link for key: " + key))
  }
}

case class SaPerson(utr: String, name: SaName, address: SaIndividualAddress)

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

case class TransactionId(oid: String)

case class Liability(dueDate: LocalDate, amount: BigDecimal)

case class AmountDue(amount: BigDecimal, requiresPayment: Boolean)

case class SaAccountSummary(totalAmountDueToHmrc: Option[AmountDue], nextPayment: Option[Liability], amountHmrcOwe: Option[BigDecimal])

