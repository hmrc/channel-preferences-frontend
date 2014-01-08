package uk.gov.hmrc.common.microservice.paye.domain

import org.joda.time.{DateTimeZone, DateTime, LocalDate}
import controllers.common.{Ida, routes}
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import uk.gov.hmrc.common.microservice.domain.{TaxRegime, RegimeRoot}
import uk.gov.hmrc.common.microservice.auth.domain.Accounts
import uk.gov.hmrc.common.microservice.txqueue.domain.TxQueueTransaction
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.common.microservice.txqueue.TxQueueConnector
import controllers.common.actions.HeaderCarrier
import scala.concurrent._
import uk.gov.hmrc.common.MdcLoggingExecutionContext.fromLoggingDetails
import play.api.Logger
import uk.gov.hmrc.utils.{DateConverter, DateTimeUtils}
import DateTimeZone._

object PayeRegime extends TaxRegime {

  def isAuthorised(accounts: Accounts) = accounts.paye.isDefined

  def unauthorisedLandingPage: String = routes.LoginController.login().url

  def authenticationType = Ida
}

case class PayeRoot(nino: String,
                    title: String,
                    firstName: String,
                    secondName: Option[String],
                    surname: String,
                    name: String,
                    dateOfBirth: String,
                    links: Map[String, String],
                    transactionLinks: Map[String, String],
                    actions: Map[String, String]) extends RegimeRoot[Nino] {

  def identifier = Nino(nino)

  def currentDate = new DateTime(DateTimeZone.UTC)

  def fetchTaxYearData(taxYear: Int)(implicit payeConnector: PayeConnector, txQueueMicroservice: TxQueueConnector, headerCarrier: HeaderCarrier): Future[TaxYearData] = {
    val f1 = fetchCars(taxYear)
    val f2 = fetchEmployments(taxYear)

    for {
      cars <- f1
      employments <- f2
    } yield TaxYearData(cars, employments)
  }

  def fetchTaxCodes(taxYear: Int)(implicit payeConnector: PayeConnector, headerCarrier: HeaderCarrier): Future[Seq[TaxCode]] =
    valuesForTaxYear[TaxCode](resource = "taxCode", taxYear = taxYear)

  def fetchCars(taxYear: Int)(implicit payeConnector: PayeConnector, headerCarrier: HeaderCarrier): Future[Seq[CarBenefit]] =
    valuesForTaxYear[CarAndFuel](resource = "benefit-cars", taxYear = taxYear).map(_.map(CarBenefit(_)))

  def fetchEmployments(taxYear: Int)(implicit payeConnector: PayeConnector, headerCarrier: HeaderCarrier): Future[Seq[Employment]] =
    valuesForTaxYear[Employment](resource = "employments", taxYear = taxYear)

  def fetchTransactionHistory(txQueueConnector: TxQueueConnector, now: () => DateTime = () => DateTimeUtils.now)(implicit hc: HeaderCarrier): Future[Seq[TxQueueTransaction]] = {
    lookupTransactionHistory(txQueueConnector, now().minusDays(30), 0).flatMap { monthOfTransactions =>
      if (monthOfTransactions.size < 3)
        lookupTransactionHistory(txQueueConnector, new DateTime(0, UTC), 3)
      else
        Future.successful(monthOfTransactions)
    }
  }

  def version(implicit payeConnector: PayeConnector, headerCarrier: HeaderCarrier): Future[Int] =
    links.get("version").map(payeConnector.version).getOrElse(throw new IllegalStateException(s"Could not find 'version' link for $nino"))


  private def lookupTransactionHistory(txQueueConnector: TxQueueConnector, forDate: DateTime, maxResults: Int)(implicit hc: HeaderCarrier): Future[Seq[TxQueueTransaction]] = {
    transactionLinks.get("history") map {
      uri =>
        val filledInUri = uri
          .replace("{from}", DateConverter.formatToString(forDate))
          .replace("{statuses}", "ACCEPTED,COMPLETED")
          .replace("{maxResults}", s"$maxResults")
        val tx = txQueueConnector.transaction(filledInUri)
        tx.map(_.getOrElse(Seq.empty))
    } getOrElse Future.successful(Seq.empty)
  }

  def addBenefitLink(taxYear: Int): Option[String] = links.get("benefits").map(_.replace("{taxYear}", taxYear.toString))

  private def valuesForTaxYear[T](resource: String, taxYear: Int)(implicit payeConnector: PayeConnector, m: Manifest[T], headerCarrier: HeaderCarrier): Future[Seq[T]] =
    links.get(resource) match {
      case Some(uri) => resourceFor[Seq[T]](uri.replace("{taxYear}", taxYear.toString)).map(_.getOrElse(Seq.empty))
      case _ => {
        Logger.warn(s"no link found for resource $resource");
        Future.successful(Seq.empty)
      }
    }

  private def resourceFor[T](uri: String)(implicit payeConnector: PayeConnector, m: Manifest[T], headerCarrier: HeaderCarrier): Future[Option[T]] =
    payeConnector.linkedResource[T](uri)
}

case class TaxYearData(cars: Seq[CarBenefit], employments: Seq[Employment]) {

  def findActiveCarBenefit(employmentNumber: Int): Option[CarBenefit] = {
    cars.find(c => c.employmentSequenceNumber == employmentNumber && c.isActive)
  }

  def findActiveFuelBenefit(employmentNumber: Int): Option[FuelBenefit] = {
    cars.find(c => c.employmentSequenceNumber == employmentNumber && c.hasActiveFuel).flatMap(_.fuelBenefit)
  }

  def findPrimaryEmployment: Option[Employment] = {
    employments.find(_.employmentType == Employment.primaryEmploymentType)
  }

  def hasActiveBenefit(employmentSequenceNumber: Int, benefitType: Int): Boolean = {
    findActiveCarBenefit(employmentSequenceNumber).map { carBenefit =>
      if (benefitType == BenefitTypes.FUEL) carBenefit.hasActiveFuel
      else benefitType == BenefitTypes.CAR
    }
  }.getOrElse(false)
}


case class TaxCode(employmentSequenceNumber: Int,
                   codingSequenceNumber: Option[Int],
                   taxYear: Int,
                   taxCode: String,
                   allowances: List[Allowance])

case class Allowance(sourceAmount: Int, adjustedAmount: Int, `type`: Int)

case class WithdrawnBenefitRequest(version: Int, car: Option[WithdrawnCarBenefit], fuel: Option[WithdrawnFuelBenefit])

case class WithdrawnCarBenefit(withdrawDate: LocalDate, numberOfDaysUnavailable: Option[Int] = None,
                               employeeContribution: Option[Int] = None)

case class WithdrawnFuelBenefit(withdrawDate: LocalDate)

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

