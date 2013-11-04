package models.paye

import uk.gov.hmrc.common.microservice.paye.domain.{PayeRoot, TaxCode}

import uk.gov.hmrc.common.microservice.paye.PayeConnector

object TaxCodeResolver {

  val NON_DEFINED_TAXCODE = "N/A"

  def currentTaxCode(payeRoot: PayeRoot, employmentSequenceNumber: Int, taxYear: Int)(implicit microservice: PayeConnector) : String = {
    currentTaxCode(payeRoot.fetchTaxCodes(taxYear), employmentSequenceNumber )
  }

  def currentTaxCode(taxCodes: Seq[TaxCode], employmentSequenceNumber: Int): String = {

    val taxCodesForEmployment = taxCodes.filter(taxCode => taxCode.employmentSequenceNumber == employmentSequenceNumber && taxCode.codingSequenceNumber.isDefined)
    taxCodesForEmployment match {
      case Nil => NON_DEFINED_TAXCODE
      case _ => taxCodesForEmployment.sortWith((tc1, tc2) => tc1.codingSequenceNumber.get > tc2.codingSequenceNumber.get).head.taxCode
    }

  }

}
