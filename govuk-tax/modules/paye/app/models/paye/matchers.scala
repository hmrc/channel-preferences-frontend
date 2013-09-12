package models.paye

import uk.gov.hmrc.microservice.txqueue.TxQueueTransaction
import uk.gov.hmrc.microservice.paye.domain.Benefit

object matchers {

  object transactions {

    def matchesBenefitWithMessageCode(tx: TxQueueTransaction, employmentSequenceNumber: Int, taxYear: Int): Boolean = {
      tx.properties("employmentSequenceNumber").toInt == employmentSequenceNumber &&
        tx.properties("taxYear").toInt == taxYear &&
        tx.tags.get.exists(_.startsWith("message.code."))
    }

    def matchesBenefit(tx: TxQueueTransaction, kind: Int, employmentSequenceNumber: Int, year: Int): Boolean = {
      tx.properties("benefitTypes").split(',').contains(kind.toString) &&
        tx.properties("employmentSequenceNumber").toInt == employmentSequenceNumber &&
        tx.properties("taxYear").toInt == year
    }

    def matchesBenefitWithMessageCode(tx: TxQueueTransaction, benefit: Benefit): Boolean = {
      tx.properties("benefitTypes").split(',').contains(benefit.benefitType.toString) &&
        tx.properties("employmentSequenceNumber").toInt == benefit.employmentSequenceNumber &&
        tx.properties("taxYear").toInt == benefit.taxYear &&
        tx.tags.get.exists(_.startsWith("message.code."))
    }
  }
}
