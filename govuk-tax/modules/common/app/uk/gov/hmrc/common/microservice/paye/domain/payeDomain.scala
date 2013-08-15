package uk.gov.hmrc.microservice.paye.domain

import org.joda.time.{ DateTime, LocalDate }
import org.joda.time.format.DateTimeFormat
import controllers.common.routes
import play.api.mvc.{ AnyContent, Action }
import play.api.mvc.Call
import uk.gov.hmrc.microservice.paye.PayeMicroService
import uk.gov.hmrc.microservice.domain.{ TaxRegime, RegimeRoot }
import uk.gov.hmrc.microservice.auth.domain.Regimes
import uk.gov.hmrc.microservice.txqueue.{ TxQueueTransaction, TxQueueMicroService }

object PayeRegime extends TaxRegime {
  override def isAuthorised(regimes: Regimes) = {
    regimes.paye.isDefined
  }

  override def unauthorisedLandingPage: String = {
    routes.LoginController.login.url
  }
}

case class PayeRoot(nino: String, version: Int, title: String, firstName: String, surname: String, name: String, dateOfBirth: String, links: Map[String, String], transactionLinks: Map[String, String]) extends RegimeRoot {

  def taxCodes(taxYear: Int)(implicit payeMicroService: PayeMicroService): Seq[TaxCode] = {
    getValuesForTaxYear[TaxCode]("taxCode", taxYear)
  }

  def benefits(taxYear: Int)(implicit payeMicroService: PayeMicroService): Seq[Benefit] = {
    getValuesForTaxYear[Benefit]("benefits", taxYear)
  }

  def employments(taxYear: Int)(implicit payeMicroService: PayeMicroService): Seq[Employment] = {
    getValuesForTaxYear[Employment]("employments", taxYear)
  }

  val dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd")

  def transactionsWithStatusFromDate(status: String, date: DateTime)(implicit txQueueMicroService: TxQueueMicroService): Seq[TxQueueTransaction] = {
    transactionLinks.get(status) match {
      case Some(uri) => {
        val formattedDate = date.toString(dateFormat)
        val uri: String = transactionLinks(status).replace("{from}", formattedDate)
        txQueueMicroService.transaction(uri).getOrElse(Seq.empty)
      }
      case _ => Seq.empty[TxQueueTransaction]
    }
  }

  private def getValuesForTaxYear[T](resource: String, taxYear: Int)(implicit payeMicroService: PayeMicroService, m: Manifest[T]): Seq[T] = {
    links.get(resource) match {
      case Some(uri) => resourceFor[Seq[T]](uri.replace("{taxYear}", taxYear.toString)).getOrElse(Seq.empty)
      case _ => Seq.empty
    }
  }

  private def resourceFor[T](uri: String)(implicit payeMicroService: PayeMicroService, m: Manifest[T]): Option[T] = {
    payeMicroService.linkedResource[T](uri)
  }
}

case class TaxCode(employmentSequenceNumber: Int, taxYear: Int, taxCode: String)

case class Benefit(benefitType: Int, taxYear: Int, grossAmount: BigDecimal, employmentSequenceNumber: Int, costAmount: BigDecimal, amountMadeGood: BigDecimal, cashEquivalent: BigDecimal, expensesIncurred: BigDecimal, amountOfRelief: BigDecimal, paymentOrBenefitDescription: String, car: Option[Car], actions: Map[String, String], calculations: Map[String, String]) {
  def grossAmountToString(format: String = "%.2f") = format.format(grossAmount)
}

case class Car(dateCarMadeAvailable: Option[LocalDate], dateCarWithdrawn: Option[LocalDate], dateCarRegistered: Option[LocalDate], employeeCapitalContribution: BigDecimal, fuelType: Int, co2Emissions: Int, engineSize: Int, mileageBand: String, carValue: BigDecimal)

case class RemoveCarBenefit(version: Int, benefit: Benefit, revisedAmount: BigDecimal, withdrawDate: LocalDate)

case class Employment(sequenceNumber: Int, startDate: LocalDate, endDate: Option[LocalDate], taxDistrictNumber: String, payeNumber: String, employerName: Option[String]) {
  lazy val employerNameOrReference = if (employerName.isDefined) employerName.get else taxDistrictNumber + "/" + payeNumber
}

case class TransactionId(oid: String)

case class RecentTransaction(messageCode: String, txTime: LocalDate)

case class EmploymentData(employment: Employment, taxCode: Option[TaxCode], acceptedTransactions: Seq[RecentTransaction], completedTransactions: Seq[RecentTransaction])
