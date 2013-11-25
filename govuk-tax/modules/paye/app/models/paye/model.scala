package models.paye

import org.joda.time.{ DateTime, LocalDate }
import uk.gov.hmrc.common.microservice.paye.domain.Employment
import uk.gov.hmrc.common.microservice.paye.domain.Car
import uk.gov.hmrc.common.microservice.paye.domain.Benefit
import uk.gov.hmrc.common.microservice.paye.domain.TaxCode
import uk.gov.hmrc.common.microservice.txqueue.domain.TxQueueTransaction

case class BenefitInfo(startDate:String, withdrawDate:String, apportionedValue:BigDecimal)


case class DisplayBenefit(employment: Employment,
    benefits: Seq[Benefit],
    car: Option[Car],
    benefitsInfo:Map[String, BenefitInfo] = Map.empty) {

  lazy val benefit = benefits(0)

  lazy val allBenefitsToString = DisplayBenefit.allBenefitsAsString(benefits.map(_.benefitType))
}

object DisplayBenefit {
  private val sep: Char = ','

  private def allBenefitsAsString(values: Seq[Int]) = values.mkString(sep.toString)

  def fromStringAllBenefit(input: String): Seq[Int] = input.split(sep).map(_.toInt)
}

object DisplayBenefits {

  import models.paye.Matchers.transactions.matchesBenefitWithMessageCode

  def apply(benefits: Seq[Benefit], employments: Seq[Employment]): Seq[DisplayBenefit] = {
    val matchedBenefits = benefits.filter {
      benefit => employments.exists(_.sequenceNumber == benefit.employmentSequenceNumber)
    }

    matchedBenefits.map {
      benefit =>
        DisplayBenefit(employments.find(_.sequenceNumber == benefit.employmentSequenceNumber).get,
          Seq(benefit),
          benefit.car)
    }
  }
}

case class RemoveBenefitFormData(withdrawDate: LocalDate, removeCar: Boolean, fuelDateChoice:Option[String], fuelWithdrawDate:Option[LocalDate] )

case class PayeOverview(name: String, lastLogin: Option[DateTime], nino: String, employmentViews: Seq[EmploymentView], hasBenefits: Boolean)

case class EmploymentView(companyName: String, startDate: LocalDate, endDate: Option[LocalDate], taxCode: String,
  recentChanges: Seq[RecentChange], taxCodeChange: Option[RecentChange] = None)

object EmploymentViews {

  val TRANSACTION_STATUS_ACCEPTED = "accepted"
  val TRANSACTION_STATUS_COMPLETED = "completed"

  import Matchers.transactions.matchesBenefitWithMessageCode

  def apply(employments: Seq[Employment],
            taxCodes: Seq[TaxCode],
            taxYear: Int,
            acceptedTransactions: Seq[TxQueueTransaction],
            completedTransactions: Seq[TxQueueTransaction]): Seq[EmploymentView] = employments.map(e => EmploymentView(
    e.employerNameOrReference, e.startDate, e.endDate, TaxCodeResolver.currentTaxCode(taxCodes, e.sequenceNumber),
    recentChanges(e.sequenceNumber, taxYear, acceptedTransactions, completedTransactions),
    taxCodeChange(e.sequenceNumber, taxYear, acceptedTransactions, completedTransactions)))

  def hasPendingTransactionsOfType(employmentViews : Seq[EmploymentView], transactionType  :String) : Boolean =  employmentViews.flatMap(_.recentChanges).exists(_.messageCode.endsWith(s"$transactionType.$TRANSACTION_STATUS_ACCEPTED") )

  private def recentChanges(sequenceNumber:Int, taxYear:Int, acceptedTransactions:Seq[TxQueueTransaction], completedTransactions: Seq[TxQueueTransaction]) =
    transactionsWithEmploymentNumber(sequenceNumber, taxYear, acceptedTransactions, s".$TRANSACTION_STATUS_ACCEPTED") ++
    transactionsWithEmploymentNumber(sequenceNumber, taxYear, completedTransactions, s".$TRANSACTION_STATUS_COMPLETED")

  private def transactionsWithEmploymentNumber(employmentSequenceNumber: Int, taxYear: Int, transactions: Seq[TxQueueTransaction],
    messageCodePostfix: String): Seq[RecentChange] = {
    transactions.filter(matchesBenefitWithMessageCode(_, employmentSequenceNumber, taxYear)).map {
      tx =>
        RecentChange(
          tx.tags.get.find(_.startsWith("message.code.")).get.replace("message.code.", "") + messageCodePostfix,
          tx.createdAt.toLocalDate, tx.properties("benefitTypes").split(',').toSeq)
    }
  }

  private def taxCodeChange(employmentSequenceNumber: Int, taxYear: Int, acceptedTransactions: Seq[TxQueueTransaction],
    completedTransactions: Seq[TxQueueTransaction]): Option[RecentChange] = {
    val accepted = findTaxCodeChange(employmentSequenceNumber, taxYear, acceptedTransactions, s"taxcode.$TRANSACTION_STATUS_ACCEPTED")
    if (accepted.isEmpty) {
      findTaxCodeChange(employmentSequenceNumber, taxYear, completedTransactions, s"taxcode.$TRANSACTION_STATUS_COMPLETED")
    } else {
      accepted
    }
  }

  private def findTaxCodeChange(employmentSequenceNumber: Int, taxYear: Int, transactions: Seq[TxQueueTransaction],
    messageCode: String): Option[RecentChange] = {
    transactions.find(matchesBenefitWithMessageCode(_, employmentSequenceNumber, taxYear)).map {
      tx =>
        RecentChange(messageCode, tx.statusHistory(0).createdAt.toLocalDate, tx.properties("benefitTypes").split(',').toSeq)
    }
  }
}

case class RecentChange(messageCode: String, timeUserRequestedChange: LocalDate, types:Seq[String] = Seq.empty)

case class CarFuelBenefitDates(carDate: Option[LocalDate], fuelDateType: Option[String])

case class BenefitUpdatedConfirmationData(oldTaxCode: String, newTaxCode: Option[String], personalAllowance: Option[Int], taxYearStart: LocalDate, taxYearEnd: LocalDate)
