package uk.gov.hmrc.common.microservice.paye.domain

import org.joda.time.{DateTimeZone, DateTime, LocalDate}
import org.joda.time.format.DateTimeFormat
import controllers.common.{Ida, routes}
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import uk.gov.hmrc.common.microservice.domain.{TaxRegime, RegimeRoot}
import uk.gov.hmrc.common.microservice.auth.domain.Accounts
import uk.gov.hmrc.common.microservice.txqueue.domain.TxQueueTransaction
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.common.microservice.txqueue.TxQueueConnector
import controllers.common.actions.HeaderCarrier
import scala.concurrent._
import ExecutionContext.Implicits.global

object PayeRegime extends TaxRegime {

  def isAuthorised(accounts: Accounts) = accounts.paye.isDefined

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

  def fetchTaxYearData(taxYear: Int)(implicit payeConnector: PayeConnector, txQueueMicroservice: TxQueueConnector, headerCarrier: HeaderCarrier): Future[TaxYearData] = {
    val f1 = fetchBenefits(taxYear)
    val f2 = fetchEmployments(taxYear)
    val f3 = fetchExpiredCarBenefits(taxYear)

    for {
      benefits <- f1
      employments <- f2
      previousCarBenefit <- f3
    } yield TaxYearData(benefits, employments, previousCarBenefit)
  }

  def fetchTaxCodes(taxYear: Int)(implicit payeConnector: PayeConnector, headerCarrier: HeaderCarrier) :Future[Seq[TaxCode]] =
    valuesForTaxYear[TaxCode](resource = "taxCode", taxYear = taxYear)

  def fetchBenefits(taxYear: Int)(implicit payeConnector: PayeConnector, headerCarrier: HeaderCarrier): Future[Seq[Benefit]] =
    valuesForTaxYear[Benefit](resource = "benefit-car", taxYear = taxYear)

  def fetchExpiredCarBenefits(taxYear: Int)(implicit payeConnector: PayeConnector, headerCarrier: HeaderCarrier): Future[Seq[CarAndFuel]] = {
    val car = Some(Car(None, Some(new LocalDate(2012, 6, 1)), Some(new LocalDate(2012, 12, 12)), Some(0), Some("diesel"), Some(124), Some(1400), Some("A"), Some(BigDecimal("12343.21")), None, None))
    val carBenefit = Benefit(31, 2013, 321.42, 2, None, None, None, None, None, None, None, car, Map(), Map("withdraw" -> "someUrl/{withdrawDate}"))
    val carBenefitTwo = Benefit(31, 2013, 321.42, 2, None, None, None, None, None, None, None, car, Map(), Map("withdraw" -> "someUrl/{withdrawDate}"))
    future( Seq(CarAndFuel(carBenefit, None), CarAndFuel(carBenefitTwo, None))) // FIXME: Stubbed
  }

  def fetchEmployments(taxYear: Int)(implicit payeConnector: PayeConnector, headerCarrier: HeaderCarrier): Future[Seq[Employment]] =
    valuesForTaxYear[Employment](resource = "employments", taxYear = taxYear)

  def fetchRecentAcceptedTransactions(implicit txQueueConnector: TxQueueConnector, hc: HeaderCarrier): Future[Seq[TxQueueTransaction]] = {
    transactionsWithStatusFromDate("accepted", currentDate.minusMonths(1))
  }

  def fetchRecentCompletedTransactions(implicit txQueueConnector: TxQueueConnector, hc: HeaderCarrier): Future[Seq[TxQueueTransaction]] = {
    transactionsWithStatusFromDate("completed", currentDate.minusMonths(1))
  }

  def addBenefitLink(taxYear: Int): Option[String] = links.get("benefits").map(_.replace("{taxYear}", taxYear.toString))

  private def transactionsWithStatusFromDate(status: String, date: DateTime)(implicit txQueueConnector: TxQueueConnector, hc: HeaderCarrier): Future[Seq[TxQueueTransaction]] =
    transactionLinks.get(status) match {
      case Some(uri) =>
        val uri = transactionLinks(status).replace("{from}", date.toString(dateFormat))
        val tx = txQueueConnector.transaction(uri)
        tx.map(_.getOrElse(Seq.empty))
      case _ =>
        Future.successful(Seq.empty[TxQueueTransaction])
    }

  private def valuesForTaxYear[T](resource: String, taxYear: Int)(implicit payeConnector: PayeConnector, m: Manifest[T], headerCarrier: HeaderCarrier): Future[Seq[T]] =
    links.get(resource) match {
      case Some(uri) => resourceFor[Seq[T]](uri.replace("{taxYear}", taxYear.toString)).map(_.getOrElse(Seq.empty))
      case _ => Future.successful(Seq.empty)
    }

  private def resourceFor[T](uri: String)(implicit payeConnector: PayeConnector, m: Manifest[T], headerCarrier: HeaderCarrier): Future[Option[T]] =
    payeConnector.linkedResource[T](uri)
}

case class TaxYearData(benefits: Seq[Benefit], employments: Seq[Employment], previousCarBenefits: Seq[CarAndFuel]) {
  def findExistingBenefit(employmentNumber: Int, benefitType: Int): Option[Benefit] = {
    benefits.find(b => b.benefitType == benefitType && b.employmentSequenceNumber == employmentNumber)
  }

  def findPrimaryEmployment: Option[Employment] = {
    employments.find(_.employmentType == Employment.primaryEmploymentType)
  }
}


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
                      employmentSequence: Int,
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

case class AddBenefitResponse(transaction: TransactionId, newTaxCode: Option[String], netCodedAllowance: Option[Int])

