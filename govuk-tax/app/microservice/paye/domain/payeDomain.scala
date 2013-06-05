package microservice.paye.domain

import microservice.domain.{ RegimeRoot, TaxRegime }
import microservice.paye.PayeMicroService

class PayeRegime extends TaxRegime

case class PayeRoot(designatoryDetails: PayeDesignatoryDetails,
    links: Map[String, String]) extends RegimeRoot {

  def taxCode(implicit payeMicroService: PayeMicroService): Option[TaxCode] = {
    links.get("taxCode") match {
      case Some(uri) => Some(payeMicroService.taxCode(uri))
      case _ => None
    }
  }
}

case class PayeDesignatoryDetails(name: String)

case class TaxCode(code: String)

