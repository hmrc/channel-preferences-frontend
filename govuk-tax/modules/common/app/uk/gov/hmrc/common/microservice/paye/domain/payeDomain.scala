package uk.gov.hmrc.common.microservice.paye.domain

import org.joda.time.{DateTimeZone, DateTime, LocalDate}
import org.joda.time.format.DateTimeFormat
import controllers.common.{Ida, AuthenticationType, routes}
import uk.gov.hmrc.common.microservice.paye.PayeMicroService
import uk.gov.hmrc.common.microservice.domain.{TaxRegime, RegimeRoot}
import uk.gov.hmrc.common.microservice.auth.domain.Regimes
import uk.gov.hmrc.microservice.txqueue.{TxQueueTransaction, TxQueueMicroService}
import uk.gov.hmrc.domain.Nino

object PayeRegime extends TaxRegime {

  def isAuthorised(regimes: Regimes) = regimes.paye.isDefined

  def unauthorisedLandingPage: String = routes.LoginController.login().url

  def authenticationType = Ida
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

  def fetchTaxYearData(taxYear: Int)(implicit payeMicroService: PayeMicroService, txQueueMicroservice: TxQueueMicroService) = PayeRootData(
    fetchRecentAcceptedTransactions,
    fetchRecentCompletedTransactions,
    fetchBenefits(taxYear),
    fetchEmployments(taxYear))

  def fetchTaxCodes(taxYear: Int)(implicit payeMicroService: PayeMicroService) =
    valuesForTaxYear[TaxCode](resource = "taxCode", taxYear = taxYear)

  def fetchBenefits(taxYear: Int)(implicit payeMicroService: PayeMicroService) : Seq[Benefit] =
    valuesForTaxYear[Benefit](resource = "benefits", taxYear = taxYear)

  def fetchEmployments(taxYear: Int)(implicit payeMicroService: PayeMicroService) : Seq[Employment] =
    valuesForTaxYear[Employment](resource = "employments", taxYear = taxYear)

  def fetchRecentAcceptedTransactions()(implicit txQueueMicroService: TxQueueMicroService) : Seq[TxQueueTransaction] = {
    transactionsWithStatusFromDate("accepted", currentDate.minusMonths(1))
  }

  def fetchRecentCompletedTransactions()(implicit txQueueMicroService: TxQueueMicroService) : Seq[TxQueueTransaction]  = {
    transactionsWithStatusFromDate("completed", currentDate.minusMonths(1))
  }

  def addBenefitLink : Option[String] = links.get("addBenefits")

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

case class PayeRootData(acceptedTransactions: Seq[TxQueueTransaction], completedTransactions: Seq[TxQueueTransaction],
                        taxYearBenefits: Seq[Benefit], taxYearEmployments: Seq[Employment])


case class TaxCode(employmentSequenceNumber: Int,
                   codingSequenceNumber: Option[Int],
                   taxYear: Int,
                   taxCode: String,
                   allowances: List[Allowance])

case class Allowance(sourceAmount: Int, adjustedAmount: Int, `type`: Int)


case class RevisedBenefit(benefit: Benefit, revisedAmount: BigDecimal)

case class RemoveBenefit(version: Int,
                         benefits: Seq[RevisedBenefit],
                         withdrawDate: LocalDate)

case class AddBenefit(version: Int,
                      employmentSequence:Int,
                      benefits: Seq[Benefit])

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

case class AddBenefitResponse(calculatedTaxCode: Option[String], personalAllowance: Option[Int])


