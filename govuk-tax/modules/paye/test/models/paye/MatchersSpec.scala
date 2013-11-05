package models.paye

import org.scalatest._
import org.scalatest.WordSpec
import uk.gov.hmrc.common.microservice.txqueue.domain.TxQueueTransaction
import models.paye.Matchers.transactions

class MatchersSpec extends WordSpec with Matchers{

      def txQueueTransaction(tags : Option[List[String]], properties : Map[String, String] ) = TxQueueTransaction(id = null,regime =  null, user = null, callback = None, statusHistory = List.empty, tags = tags, properties = properties, createdAt = null, lastUpdatedAt = null)
  "matchesBenefit " should {
   "return true if an matching transaction exists " in {
     val taxYear = 2011
     val employmentSequenceNumber = 1
     val benefitType = 31
     val transactionTypeTag = "removeBenefits"
     val matchingProperties = Map("benefitTypes" -> benefitType.toString, "employmentSequenceNumber" -> employmentSequenceNumber.toString, "taxYear" -> taxYear.toString)

     val matchingTransaction:TxQueueTransaction = txQueueTransaction(Some(List(s"message.code.$transactionTypeTag")), matchingProperties)
     transactions.matchesBenefit(matchingTransaction, benefitType, employmentSequenceNumber, taxYear, transactionTypeTag) shouldBe true
    }
    "return false if no transaction exists with a matching benefitType " in {
      pending
    }
    "return false if no transaction exists with a matching sequenceNumber " in {
      pending
    }
    "return false if no transaction exists with a matching tax year " in {
      pending
    }
    "return false if no transaction exists with a matching transactionType " in {
      pending
    }
  }
  /*
  def matchesBenefit(tx: TxQueueTransaction, kind: Int, employmentSequenceNumber: Int, year: Int, transactionTypeTag:String): Boolean = {
tx.properties("benefitTypes").split(',').contains(kind.toString) &&
tx.properties("employmentSequenceNumber").toInt == employmentSequenceNumber &&
tx.properties("taxYear").toInt == year &&
tx.tags.exists(_.exists(_.contains(transactionTypeTag)))
}
   */
}