package models.paye

import org.joda.time.{ DateTime, LocalDate }
import uk.gov.hmrc.microservice.paye.domain.Employment
import uk.gov.hmrc.microservice.paye.domain.Car
import uk.gov.hmrc.microservice.txqueue.TxQueueTransaction
import uk.gov.hmrc.microservice.paye.domain.Benefit
import uk.gov.hmrc.microservice.paye.domain.TaxCode

case class DisplayBenefit(employment: Employment,
  benefit: Benefit,
  car: Option[Car],
  transaction: Option[TxQueueTransaction])

object DisplayBenefits {

  import models.paye.matchers.transactions.matchesBenefitWithMessageCode

  def apply(benefits: Seq[Benefit], employments: Seq[Employment], transactions: Seq[TxQueueTransaction]): Seq[DisplayBenefit] = {
    val matchedBenefits = benefits.filter {
      benefit => employments.exists(_.sequenceNumber == benefit.employmentSequenceNumber)
    }

    matchedBenefits.map {
      benefit =>
        DisplayBenefit(employments.find(_.sequenceNumber == benefit.employmentSequenceNumber).get,
          benefit,
          benefit.car,
          transactions.find(matchesBenefitWithMessageCode(_, benefit))
        )
    }
  }
}

case class RemoveBenefitFormData(withdrawDate: LocalDate,
  agreement: Boolean)

case class PayeOverview(name: String, lastLogin: Option[DateTime], nino: String, employmentViews: Seq[EmploymentView], hasBenefits: Boolean)

case class EmploymentView(companyName: String, startDate: LocalDate, endDate: Option[LocalDate], taxCode: String, recentChanges: Seq[RecentChange])

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
        transactionsWithEmploymentNumber(e.sequenceNumber, taxYear, completedTransactions, "completed")).toList)
  }

  private def taxCodeWithEmploymentNumber(employmentSequenceNumber: Int, taxCodes: Seq[TaxCode]) = {
    taxCodes.find(_.employmentSequenceNumber == employmentSequenceNumber).map(_.taxCode).getOrElse("N/A")
  }

  private def transactionsWithEmploymentNumber(employmentSequenceNumber: Int, taxYear: Int, transactions: Seq[TxQueueTransaction],
    messageCodePrefix: String): Seq[RecentChange] =
    transactions.filter(matchesBenefitWithMessageCode(_, employmentSequenceNumber, taxYear)).map {
      tx =>
        RecentChange(
          tx.tags.get.find(_.startsWith("message.code.")).get.replace("message.code", messageCodePrefix),
          tx.statusHistory(0).createdAt.toLocalDate)
    }
}

case class RecentChange(messageCode: String, timeOfChange: LocalDate)

