package models.paye

import uk.gov.hmrc.common.microservice.paye.domain.{PayeRoot, TaxCode}

import uk.gov.hmrc.common.microservice.paye.PayeConnector
import controllers.common.actions.HeaderCarrier
import scala.concurrent._
import uk.gov.hmrc.common.StickyMdcExecutionContext.global

object TaxCodeResolver {

  val NON_DEFINED_TAXCODE = "N/A"

  def currentTaxCode(payeRoot: PayeRoot, employmentSequenceNumber: Int, taxYear: Int)(implicit microservice: PayeConnector, headerCarrier: HeaderCarrier): Future[String] = {
    payeRoot.fetchTaxCodes(taxYear).map(currentTaxCode(_, employmentSequenceNumber))
  }

  def currentTaxCode(taxCodes: Seq[TaxCode], employmentSequenceNumber: Int): String = {
    def pred(taxCode: TaxCode) = taxCode.employmentSequenceNumber == employmentSequenceNumber && taxCode.codingSequenceNumber.isDefined
    def sort(tc1: TaxCode, tc2: TaxCode) = tc1.codingSequenceNumber.get > tc2.codingSequenceNumber.get

    taxCodes.filter(pred).sortWith(sort).headOption.map(_.taxCode).getOrElse(NON_DEFINED_TAXCODE)
  }
}
