package models.paye

import org.joda.time.{ DateTime, LocalDate }
import uk.gov.hmrc.microservice.paye.domain.Employment
import uk.gov.hmrc.microservice.paye.domain.Car
import uk.gov.hmrc.microservice.txqueue.TxQueueTransaction
import uk.gov.hmrc.microservice.paye.domain.Benefit
import uk.gov.hmrc.microservice.paye.domain.TaxCode

object BenefitTypes {

  val FUEL = 29
  val CAR = 31

}

case class DisplayBenefit(employment: Employment,
    benefits: Seq[Benefit],
    car: Option[Car],
    transaction: Option[TxQueueTransaction]) {

  lazy val benefit = benefits(0)

  lazy val allBenefitsToString = DisplayBenefit.allBenefitsAsString(benefits.map(_.benefitType))
}

object DisplayBenefit {
  private val sep: Char = ','

  private def allBenefitsAsString(values: Seq[Int]) = values.mkString(sep.toString)

  def fromStringAllBenefit(input: String): Seq[Int] = input.split(sep).map(_.toInt)
}

object DisplayBenefits {

  import models.paye.matchers.transactions.matchesBenefitWithMessageCode

  def apply(benefits: Seq[Benefit], employments: Seq[Employment], transactions: Seq[TxQueueTransaction]): Seq[DisplayBenefit] = {
    val matchedBenefits = benefits.filter {
      benefit => employments.exists(_.sequenceNumber == benefit.employmentSequenceNumber)
    }

    matchedBenefits.map {
      benefit =>
        DisplayBenefit(employments.find(_.sequenceNumber == benefit.employmentSequenceNumber).get,
          Seq(benefit),
          benefit.car,
          transactions.find(matchesBenefitWithMessageCode(_, benefit))
        )
    }
  }
}

case class RemoveBenefitFormData(withdrawDate: LocalDate,
  agreement: Boolean)

case class PayeOverview(name: String, lastLogin: Option[DateTime], nino: String, employmentViews: Seq[EmploymentView], hasBenefits: Boolean)

case class EmploymentView(companyName: String, startDate: LocalDate, endDate: Option[LocalDate], taxCode: String,
  recentChanges: Seq[RecentChange], taxCodeChange: Option[RecentChange] = None)

object EmploymentViews {

  import matchers.transactions.matchesBenefitWithMessageCode

  def apply(employments: Seq[Employment],
    taxCodes: Seq[TaxCode],
    taxYear: Int,
    acceptedTransactions: Seq[TxQueueTransaction],
    completedTransactions: Seq[TxQueueTransaction]): Seq[EmploymentView] = {
    for (e <- employments) yield EmploymentView(
      e.employerNameOrReference, e.startDate, e.endDate, taxCodeWithEmploymentNumber(e.sequenceNumber, taxCodes),
      (transactionsWithEmploymentNumber(e.sequenceNumber, taxYear, acceptedTransactions, "accepted") ++
        transactionsWithEmploymentNumber(e.sequenceNumber, taxYear, completedTransactions, "completed")).toList,
      taxCodeChange(e.sequenceNumber, taxYear, acceptedTransactions, completedTransactions))
  }

  private def taxCodeWithEmploymentNumber(employmentSequenceNumber: Int, taxCodes: Seq[TaxCode]) = {
    taxCodes.find(_.employmentSequenceNumber == employmentSequenceNumber).map(_.taxCode).getOrElse("N/A")
  }

  private def transactionsWithEmploymentNumber(employmentSequenceNumber: Int, taxYear: Int, transactions: Seq[TxQueueTransaction],
    messageCodePrefix: String): Seq[RecentChange] = {
    transactions.filter(matchesBenefitWithMessageCode(_, employmentSequenceNumber, taxYear)).map {
      tx =>
        RecentChange(
          tx.tags.get.find(_.startsWith("message.code.")).get.replace("message.code", messageCodePrefix),
          tx.statusHistory(0).createdAt.toLocalDate)
    }
  }

  private def taxCodeChange(employmentSequenceNumber: Int, taxYear: Int, acceptedTransactions: Seq[TxQueueTransaction],
    completedTransactions: Seq[TxQueueTransaction]): Option[RecentChange] = {
    val accepted = findTaxCodeChange(employmentSequenceNumber, taxYear, acceptedTransactions, "taxcode.accepted")
    if (accepted.isEmpty) {
      findTaxCodeChange(employmentSequenceNumber, taxYear, completedTransactions, "taxcode.completed")
    } else {
      accepted
    }
  }

  private def findTaxCodeChange(employmentSequenceNumber: Int, taxYear: Int, transactions: Seq[TxQueueTransaction],
    messageCode: String): Option[RecentChange] = {
    transactions.find(matchesBenefitWithMessageCode(_, employmentSequenceNumber, taxYear)).map {
      tx =>
        RecentChange(messageCode, tx.statusHistory(0).createdAt.toLocalDate)
    }
  }
}

case class RecentChange(messageCode: String, timeOfChange: LocalDate)

