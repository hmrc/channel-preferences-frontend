package uk.gov.hmrc.microservice.sa.domain

import controllers.common.RedirectUtils
import uk.gov.hmrc.microservice.sa.SaMicroService
import uk.gov.hmrc.microservice.domain.{ TaxRegime, RegimeRoot }
import uk.gov.hmrc.microservice.auth.domain.Regimes
import uk.gov.hmrc.common.microservice.sa.domain.write.SaAddressForUpdate
import org.joda.time.LocalDate

case class SaPreference(utr: String, digital: Boolean, email: Option[String] = None)

object SaRegime extends TaxRegime {
  override def isAuthorised(regimes: Regimes) = {
    regimes.sa.isDefined
  }

  override def unauthorisedLandingPage: String = {
    RedirectUtils.businessTaxHome
  }
}

case class SaRoot(utr: String, links: Map[String, String]) extends RegimeRoot {

  private val individualDetailsKey = "individual/details"
  private val individualMainAddressKey = "individual/details/main-address"
  private val individualAccountSummaryKey = "individual/account-summary"

  def personalDetails(implicit saMicroService: SaMicroService): Option[SaPerson] = {
    links.get(individualDetailsKey) match {
      case Some(uri) => saMicroService.person(uri)
      case _ => None
    }
  }

  def accountSummary(implicit saMicroService: SaMicroService): Option[SaAccountSummary] = {
    links.get(individualAccountSummaryKey) match {
      case Some(uri) => saMicroService.accountSummary(uri)
      case _ => None
    }
  }

  def updateIndividualMainAddress(address: SaAddressForUpdate)(implicit saMicroService: SaMicroService): Either[String, TransactionId] = {
    saMicroService.updateMainAddress(uriFor(individualMainAddressKey), address)
  }

  private def uriFor(key: String): String = {
    links.getOrElse(key, throw new IllegalStateException("Missing link for key: " + key))
  }
}

case class SaPerson(name: String, utr: String, address: SaIndividualAddress)

case class SaIndividualAddress(
  addressLine1: String,
  addressLine2: String,
  addressLine3: String,
  addressLine4: String,
  addressLine5: String,
  postcode: String,
  foreignCountry: String,
  additionalDeliveryInformation: String)

case class TransactionId(oid: String)

case class Liability(dueDate: LocalDate, amount: BigDecimal)

case class AmountDue(amount: BigDecimal, requiresPayment: Boolean)

case class SaAccountSummary(totalAmountDueToHmrc: AmountDue, nextPayment: Option[Liability], amountHmrcOwe: BigDecimal)

