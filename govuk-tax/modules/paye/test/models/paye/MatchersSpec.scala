package models.paye

import org.scalatest._
import org.scalatest.WordSpec
import uk.gov.hmrc.common.microservice.txqueue.domain.TxQueueTransaction
import models.paye.Matchers.transactions
import uk.gov.hmrc.common.microservice.paye.domain.Benefit
import org.joda.time.LocalDate

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


  "matchesBenefitWithMessageCode(benefit) " should {
    val transactionTypeTag = "removeBenefits"
    val matchingBenefit = Benefit(benefitType, taxYear, 1000, employmentSequenceNumber, Some(2000), Some(2000), Some(3000), Some(4000), Some(5000), Some("paymentOrBenefitDescription"), Some(LocalDate.now), None, Map.empty, Map.empty)

    "return true if a matching transaction exists " in {
      val matchingTransaction = txQueueTransaction(Some(List(s"message.code.$transactionTypeTag")), matchingProperties)
      transactions.matchesBenefitWithMessageCode(matchingTransaction, matchingBenefit)
    }

    "return false if no transaction exists with a matching sequenceNumber " in {
      val propertiesWithMismatchedSequenceNumber = matchingProperties + ("employmentSequenceNumber" -> "2")
      val transaction: TxQueueTransaction = txQueueTransaction(Some(List(s"message.code.$transactionTypeTag")), propertiesWithMismatchedSequenceNumber)
      transactions.matchesBenefitWithMessageCode(transaction, matchingBenefit) shouldBe false
    }
    
    "return false if no transaction exists with a matching benfitType " in {
      val propertiesWithMismatchedBenefitType = matchingProperties + ("benefitTypes" -> "mismatchingBenefitType")
      val transaction: TxQueueTransaction = txQueueTransaction(Some(List(s"message.code.$transactionTypeTag")), propertiesWithMismatchedBenefitType)
      transactions.matchesBenefitWithMessageCode(transaction, matchingBenefit) shouldBe false
    }

    "return false if no transaction exists with a matching tax year " in {
      val propertiesWithMismatchedTaxYear = matchingProperties + ("taxYear" -> "9999")
      val transaction: TxQueueTransaction = txQueueTransaction(Some(List(s"message.code.$transactionTypeTag")), propertiesWithMismatchedTaxYear)
      transactions.matchesBenefitWithMessageCode(transaction, matchingBenefit) shouldBe false
    }

    "return false if no transaction exists with a message code " in {
      val transaction: TxQueueTransaction = txQueueTransaction(Some(List.empty), matchingProperties)
      transactions.matchesBenefitWithMessageCode(transaction, matchingBenefit) shouldBe false
    }
  }
}