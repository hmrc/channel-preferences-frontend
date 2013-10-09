package uk.gov.hmrc.common.microservice.paye.domain

import org.joda.time.{DateTimeZone, DateTime, LocalDate}
import org.joda.time.format.DateTimeFormat
import controllers.common.routes
import uk.gov.hmrc.common.microservice.paye.PayeMicroService
import uk.gov.hmrc.common.microservice.domain.{TaxRegime, RegimeRoot}
import uk.gov.hmrc.common.microservice.auth.domain.Regimes
import uk.gov.hmrc.microservice.txqueue.{TxQueueTransaction, TxQueueMicroService}
import uk.gov.hmrc.domain.Nino

object PayeRegime extends TaxRegime {

  override def isAuthorised(regimes: Regimes) = regimes.paye.isDefined

  override def unauthorisedLandingPage: String = routes.LoginController.login().url
}

case class PayeRoot(nino: String,
                    version: Int,
                    title: String,
                    firstName: String,
                    secondName: Option[String],
                    surname: String,
                    name: String,
                    dateOfBirth: String,
                    links: Map[String, String],
                    transactionLinks: Map[String, String],
                    actions: Map[String, String]) extends RegimeRoot[Nino] {

  private val dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd")

  def identifier = Nino(nino)

  def currentDate = new DateTime(DateTimeZone.UTC)

  def taxCodes(taxYear: Int)(implicit payeMicroService: PayeMicroService) =
    valuesForTaxYear[TaxCode](resource = "taxCode", taxYear = taxYear)

  def benefits(taxYear: Int)(implicit payeMicroService: PayeMicroService) =
    valuesForTaxYear[Benefit](resource = "benefits", taxYear = taxYear)

  def employments(taxYear: Int)(implicit payeMicroService: PayeMicroService) =
    valuesForTaxYear[Employment](resource = "employments", taxYear = taxYear)

  def recentAcceptedTransactions()(implicit txQueueMicroService: TxQueueMicroService) = {
    transactionsWithStatusFromDate("accepted", currentDate.minusMonths(1))
  }

  def recentCompletedTransactions()(implicit txQueueMicroService: TxQueueMicroService) = {
    transactionsWithStatusFromDate("completed", currentDate.minusMonths(1))
  }

  private def transactionsWithStatusFromDate(status: String, date: DateTime)(implicit txQueueMicroService: TxQueueMicroService): Seq[TxQueueTransaction] =
    transactionLinks.get(status) match {
      case Some(uri) =>
        val uri = transactionLinks(status).replace("{from}", date.toString(dateFormat))
        val tx = txQueueMicroService.transaction(uri)
        tx.getOrElse(Seq.empty)
      case _ =>
        Seq.empty[TxQueueTransaction]
    }

  private def valuesForTaxYear[T](resource: String, taxYear: Int)(implicit payeMicroService: PayeMicroService, m: Manifest[T]): Seq[T] =
    links.get(resource) match {
      case Some(uri) => resourceFor[Seq[T]](uri.replace("{taxYear}", taxYear.toString)).getOrElse(Seq.empty)
      case _ => Seq.empty
    }

  private def resourceFor[T](uri: String)(implicit payeMicroService: PayeMicroService, m: Manifest[T]): Option[T] =
    payeMicroService.linkedResource[T](uri)
}

case class TaxCode(employmentSequenceNumber: Int,
                   taxYear: Int,
                   taxCode: String,
                   allowances: List[Allowance])

case class Allowance(sourceAmount: Int, adjustedAmount: Int, `type`: Int)

case class Benefit(benefitType: Int,
                   taxYear: Int,
                   grossAmount: BigDecimal,
                   employmentSequenceNumber: Int,
                   costAmount: BigDecimal,
                   amountMadeGood: BigDecimal,
                   cashEquivalent: BigDecimal,
                   expensesIncurred: BigDecimal,
                   amountOfRelief: BigDecimal,
                   paymentOrBenefitDescription: String,
                   car: Option[Car],
                   actions: Map[String, String],
                   calculations: Map[String, String]) {

  def grossAmountToString(format: String = "%.2f") = format.format(grossAmount)
}

object Benefit {
  def findByTypeAndEmploymentNumber(benefits: Seq[Benefit], employmentSequenceNumber: Int, benefitType: Int) : Option[Benefit] = {
    benefits.find(b => b.employmentSequenceNumber == employmentSequenceNumber && b.benefitType == benefitType)
  }
}

case class Car(dateCarMadeAvailable: Option[LocalDate],
               dateCarWithdrawn: Option[LocalDate],
               dateCarRegistered: Option[LocalDate],
               employeeCapitalContribution: BigDecimal,
               fuelType: Int,
               co2Emissions: Int,
               engineSize: Int,
               mileageBand: String,
               carValue: BigDecimal)

case class RevisedBenefit(benefit: Benefit, revisedAmount: BigDecimal)

case class RemoveBenefit(version: Int,
                         benefits: Seq[RevisedBenefit],
                         withdrawDate: LocalDate)

object Employment {
  val primaryEmploymentType = 1
}

case class Employment(sequenceNumber: Int,
                      startDate: LocalDate,
                      endDate: Option[LocalDate],
                      taxDistrictNumber: String,
                      payeNumber: String,
                      employerName: Option[String],
                      employmentType: Int) {

  lazy val employerNameOrReference =
    if (employerName.isDefined)
      employerName.get
    else
      taxDistrictNumber + "/" + payeNumber
}

case class TransactionId(oid: String)

case class RecentTransaction(messageCode: String,
                             txTime: LocalDate)

case class RemoveBenefitResponse(transaction: TransactionId, calculatedTaxCode: Option[String], personalAllowance: Option[Int])


