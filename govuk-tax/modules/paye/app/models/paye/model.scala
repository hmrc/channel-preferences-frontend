package models.paye

import org.joda.time.{DateTime, LocalDate}
import uk.gov.hmrc.common.microservice.paye.domain._
import Matchers.transactions._
import uk.gov.hmrc.common.microservice.txqueue.domain.TxQueueTransaction
import uk.gov.hmrc.common.microservice.paye.domain.TaxCode

case class BenefitInfo(startDate: String, withdrawDate: String, apportionedValue: BigDecimal)

case class RemoveCarBenefitFormData(withdrawDate: LocalDate,
                                    carUnavailable: Option[Boolean] = None,
                                    numberOfDaysUnavailable: Option[Int] = None,
                                    removeEmployeeContributes: Option[Boolean],
                                    removeEmployeeContribution: Option[Int],
                                    fuelDateChoice: Option[String],
                                    fuelWithdrawDate: Option[LocalDate])

object RemoveCarBenefitFormData {
  def apply(data: RemoveFuelBenefitFormData): RemoveCarBenefitFormData =
    RemoveCarBenefitFormData(
      withdrawDate = data.withdrawDate,
      removeEmployeeContributes = None,
      removeEmployeeContribution = None,
      fuelDateChoice = None,
      fuelWithdrawDate = None
    )
}

case class ReplaceCarBenefitFormData(removedCar: RemoveCarBenefitFormData, newCar: CarBenefitData)

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

case class BenefitUpdatedConfirmationData(transactionId: String, oldTaxCode: String, newTaxCode: Option[String])

sealed trait PayeJourney {
  override def toString = this.getClass.getSimpleName.split("\\$").last
}

case object AddCar extends PayeJourney
case object AddFuel extends PayeJourney
case object RemoveCar extends PayeJourney
case object RemoveFuel extends PayeJourney
case object RemoveCarAndFuel extends PayeJourney
case object ReplaceCar extends PayeJourney