package models.paye

import org.joda.time.{DateTime, LocalDate}
import uk.gov.hmrc.common.microservice.paye.domain.Employment
import uk.gov.hmrc.common.microservice.paye.domain.Car
import uk.gov.hmrc.common.microservice.paye.domain.Benefit
import uk.gov.hmrc.common.microservice.paye.domain.TaxCode
import uk.gov.hmrc.common.microservice.txqueue.domain.TxQueueTransaction
import Matchers.transactions._
import javax.transaction.Transaction

case class BenefitInfo(startDate: String, withdrawDate: String, apportionedValue: BigDecimal)


case class DisplayBenefit(employment: Employment,
                          benefits: Seq[Benefit],
                          car: Option[Car]) {

  lazy val benefit = benefits(0)

  lazy val allBenefitsToString = DisplayBenefit.allBenefitsAsString(benefits.map(_.benefitType))
}

object DisplayBenefit {
  private val sep: Char = ','

  private def allBenefitsAsString(values: Seq[Int]) = values.mkString(sep.toString)

  def fromStringAllBenefit(input: String): Seq[Int] = input.split(sep).map(_.toInt)
}

object DisplayBenefits {

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

case class RemoveCarBenefitFormData(withdrawDate: LocalDate,
                                 carUnavailable: Option[Boolean] = None,
                                 numberOfDaysUnavailable: Option[Int] = None,
                                 employeeContributes: Option[Boolean],
                                 employeeContribution: Option[Int],
                                 fuelDateChoice: Option[String],
                                 fuelWithdrawDate: Option[LocalDate])
object RemoveCarBenefitFormData {
  def apply(data: RemoveFuelBenefitFormData): RemoveCarBenefitFormData =
    RemoveCarBenefitFormData(
      withdrawDate = data.withdrawDate,
      employeeContributes = None,
      employeeContribution = None,
      fuelDateChoice = None,
      fuelWithdrawDate = None
    )
}

case class RemoveFuelBenefitFormData(withdrawDate: LocalDate)
object RemoveFuelBenefitFormData {
  def apply(removeCarBenefitFormData: RemoveCarBenefitFormData): RemoveFuelBenefitFormData = RemoveFuelBenefitFormData(removeCarBenefitFormData.withdrawDate)
}

case class PayeOverview(name: String, lastLogin: Option[DateTime], nino: String, employmentViews: Seq[EmploymentView], hasBenefits: Boolean)

case class EmploymentView(companyName: String, startDate: LocalDate, endDate: Option[LocalDate], taxCode: String,
                          recentChanges: Seq[RecentChange])

object EmploymentViews {

  val TRANSACTION_STATUS_ACCEPTED = "accepted"
  val TRANSACTION_STATUS_COMPLETED = "completed"

  def createEmploymentViews(employments: Seq[Employment],
                            taxCodes: Seq[TaxCode],
                            taxYear: Int,
                            benefitTypes: Set[Int],
                            transactionHistory: Seq[TxQueueTransaction]
                            ): Seq[EmploymentView] =
    employments.map { e =>
      EmploymentView(e.employerNameOrReference, e.startDate, e.endDate, TaxCodeResolver.currentTaxCode(taxCodes, e.sequenceNumber),
        recentChanges(e.sequenceNumber, taxYear, Seq.empty, benefitTypes))
    }

  def hasPendingTransactionsOfType(employmentViews: Seq[EmploymentView], transactionType: String): Boolean = employmentViews.flatMap(_.recentChanges).exists(_.messageCode.endsWith(s"$transactionType.$TRANSACTION_STATUS_ACCEPTED"))

  private val messageCodes = Map("accepted" -> s".$TRANSACTION_STATUS_ACCEPTED", "completed" -> s".$TRANSACTION_STATUS_COMPLETED")

  private def recentChanges(sequenceNumber: Int, taxYear: Int, transactionHistory: Seq[TxQueueTransaction], interestingBenefitTypes: Set[Int]) =
    transactionsWithEmploymentNumber(sequenceNumber, taxYear, transactionHistory, interestingBenefitTypes)

  private def transactionsWithEmploymentNumber(employmentSequenceNumber: Int, taxYear: Int, transactions: Seq[TxQueueTransaction], interestingBenefitTypes: Set[Int]): Seq[RecentChange] = {
    transactions.filter(matchesBenefitWithMessageCode(_, employmentSequenceNumber, taxYear)).filter(matchBenefitTypes(_, interestingBenefitTypes)).map {
      tx =>
        RecentChange(
          tx.tags.get.find(_.startsWith("message.code.")).get.replace("message.code.", "") + messageCodes(tx.statusHistory(0).status.toLowerCase),
          tx.createdAt.toLocalDate, tx.properties("benefitTypes").split(',').toList)
    }
  }
}

case class RecentChange(messageCode: String, timeUserRequestedChange: LocalDate, types: Seq[String] = Seq.empty)

case class CarFuelBenefitDates(carDate: Option[LocalDate], fuelDateType: Option[String])

case class BenefitUpdatedConfirmationData(oldTaxCode: String, newTaxCode: Option[String], personalAllowance: Option[Int], taxYearStart: LocalDate, taxYearEnd: LocalDate)
