package models.paye

import org.scalatest._
import org.scalatest.WordSpec
import uk.gov.hmrc.common.microservice.txqueue.domain.TxQueueTransaction
import models.paye.Matchers.transactions
import java.lang.IllegalArgumentException

class MatchersSpec extends WordSpec with Matchers {

  def txQueueTransaction(tags: Option[List[String]], properties: Map[String, String]) =
    TxQueueTransaction(id = null, regime = null, user = null, callback = None, statusHistory = List.empty, tags = tags, properties = properties, createdAt = null, lastUpdatedAt = null)

  val taxYear = 2011
  val employmentSequenceNumber = 1
  val benefitType = 31
  val transactionTypeTag = "removeBenefits"
  val matchingProperties = Map("benefitTypes" -> benefitType.toString, "employmentSequenceNumber" -> employmentSequenceNumber.toString, "taxYear" -> taxYear.toString)

  "matchesBenefitWithMessageCode(sequenceNo, year)" should {

    "return true if a matching transaction exists " in {
      val matchingTransaction: TxQueueTransaction = txQueueTransaction(Some(List(s"message.code.$transactionTypeTag")), matchingProperties)
      transactions.matchesBenefitWithMessageCode(matchingTransaction, employmentSequenceNumber, taxYear) shouldBe true
    }

    "return false if no transaction exists with a matching sequenceNumber " in {
      val propertiesWithMismatchedSequenceNumber = matchingProperties + ("employmentSequenceNumber" -> "2")
      val transaction: TxQueueTransaction = txQueueTransaction(Some(List(s"message.code.$transactionTypeTag")), propertiesWithMismatchedSequenceNumber)
      transactions.matchesBenefitWithMessageCode(transaction, employmentSequenceNumber, taxYear) shouldBe false
    }

    "return false if no transaction exists with a matching tax year " in {
      val propertiesWithMismatchedTaxYear = matchingProperties + ("taxYear" -> "9999")
      val transaction: TxQueueTransaction = txQueueTransaction(Some(List(s"message.code.$transactionTypeTag")), propertiesWithMismatchedTaxYear)
      transactions.matchesBenefitWithMessageCode(transaction, employmentSequenceNumber, taxYear) shouldBe false
    }

    "return false if no transaction exists with a message code " in {
      val transaction: TxQueueTransaction = txQueueTransaction(Some(List.empty), matchingProperties)
      transactions.matchesBenefitWithMessageCode(transaction, employmentSequenceNumber, taxYear) shouldBe false
    }
  }

  "matchBenefitTypes" should {
    "return true if the transaction benefit codes include one of the codes we are interested in" in {
      val transaction = txQueueTransaction(Some(List.empty), Map("benefitTypes" -> s"$benefitType"))
      transactions.matchBenefitTypes(transaction, Set(benefitType)) shouldBe true
    }

    "return true if any of the transaction benefit codes include one of the codes we are interested in" in {
      val transaction = txQueueTransaction(Some(List.empty), Map("benefitTypes" -> s"$benefitType,50"))
      transactions.matchBenefitTypes(transaction, Set(benefitType, 100)) shouldBe true
    }

    "return false if the transaction benefit codes do not include any codes we are interested in" in {
      val transaction = txQueueTransaction(Some(List.empty), Map("benefitTypes" -> "50"))
      transactions.matchBenefitTypes(transaction, Set(benefitType)) shouldBe false
    }

    "return false if the transaction benefit codes are empty" in {
      val transaction = txQueueTransaction(Some(List.empty), Map("benefitTypes" -> ""))
      transactions.matchBenefitTypes(transaction, Set(benefitType)) shouldBe false

    }

    "return false if the transaction has no benefit types" in {
      val transaction = txQueueTransaction(Some(List.empty), Map())
      transactions.matchBenefitTypes(transaction, Set(benefitType)) shouldBe false

    }

    "throw an IllegalArgumentException if we provide an empty set of codes to look for" in {
      val transaction = txQueueTransaction(Some(List.empty), Map("benefitTypes" -> s"$benefitType"))
      evaluating(transactions.matchBenefitTypes(transaction, Set())) should produce[IllegalArgumentException]
    }

    "throw an IllegalArgumentException if the transaction given has benefit types that are in the wrong format" in {
      val transaction = txQueueTransaction(Some(List.empty), Map("benefitTypes" -> "FUEL,CAR"))
      evaluating(transactions.matchBenefitTypes(transaction, Set(benefitType))) should produce[IllegalArgumentException]
    }
  }
}