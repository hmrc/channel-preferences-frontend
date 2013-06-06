package microservice.paye.domain

import microservice.domain.{ RegimeRoot, TaxRegime }
import microservice.paye.PayeMicroService

class PayeRegime extends TaxRegime

case class PayeRoot(name: String, links: Map[String, String]) extends RegimeRoot {

  def taxCodes(implicit payeMicroService: PayeMicroService): Option[List[TaxCode]] = {
    links.get("taxCode") match {
      case Some(uri) => Some(payeMicroService.taxCodes(uri))
      case _ => None
    }
  }

  def employments(implicit payeMicroService: PayeMicroService): Option[List[Employment]] = {
    links.get("employments") match {
      case Some(uri) => Some(payeMicroService.employments(uri))
      case _ => None
    }
  }

  def benefits(implicit payeMicroService: PayeMicroService): Option[List[Benefit]] = {
    links.get("benefits") match {
      case Some(uri) => Some(payeMicroService.benefits(uri))
      case _ => None
    }
  }
}

case class TaxCode(taxCode: String)
case class Benefit(taxYear: String, grossAmount: Double)
case class Employment(startDate: String, endDate: String, taxDistrictNumber: String, payeNumber: String)

