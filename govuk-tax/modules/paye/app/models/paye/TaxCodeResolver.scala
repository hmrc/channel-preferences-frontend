package models.paye

import uk.gov.hmrc.common.microservice.paye.domain.{PayeRoot, TaxCode}

import uk.gov.hmrc.common.microservice.paye.PayeMicroService

object TaxCodeResolver {

  def currentTaxCode(payeRoot: PayeRoot, employmentSequenceNumber: Int, taxYear: Int)(implicit microservice: PayeMicroService) : String = {
    currentTaxCode(payeRoot.taxCodes(taxYear), employmentSequenceNumber )
  }

  def currentTaxCode(taxCodes: Seq[TaxCode], employmentSequenceNumber: Int): String = {
    //TODO it's possible to have multiple tax codes for the same employment, in that case we have to use the one with the highest 'codingSequenceNumber'
    taxCodes.find(_.employmentSequenceNumber == employmentSequenceNumber).map(_.taxCode).getOrElse("N/A")
  }

}
