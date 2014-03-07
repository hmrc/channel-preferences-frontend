package models.paye

import uk.gov.hmrc.common.microservice.txqueue.domain.TxQueueTransaction
object Matchers {

  object transactions {

    def matchesBenefitWithMessageCode(tx: TxQueueTransaction, employmentSequenceNumber: Int, taxYear: Int): Boolean = {
      tx.properties("employmentSequenceNumber").toInt == employmentSequenceNumber &&
        tx.properties("taxYear").toInt == taxYear &&
        tx.tags.get.exists(_.startsWith("message.code."))
    }

    def matchBenefitTypes(tx : TxQueueTransaction, benefitTypes: Set[Int]) : Boolean = {
      require(!benefitTypes.isEmpty, "benefitTypes should not be empty")
      def convertToInts(value : String)= value.split(",").map(_.toInt).toSet

      val transactionBenefitTypesConvertedToInts : Option[Set[Int]] = tx.properties.get("benefitTypes").filter(_ != "") map convertToInts
      transactionBenefitTypesConvertedToInts.filter(!_.intersect(benefitTypes).isEmpty).isDefined
    }
  }
}
