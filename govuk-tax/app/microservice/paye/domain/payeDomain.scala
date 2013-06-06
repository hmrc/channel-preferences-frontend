package microservice.paye.domain

import microservice.domain.{ RegimeRoot, TaxRegime }
import microservice.paye.PayeMicroService

class PayeRegime extends TaxRegime

case class PayeRoot(designatoryDetails: PayeDesignatoryDetails,
    links: Map[String, String]) extends RegimeRoot {

  def taxCodes(implicit payeMicroService: PayeMicroService): Option[Seq[TaxCode]] = {
    links.get("taxCodes") match {
      case Some(uri) => Some(payeMicroService.taxCodes(uri))
      case _ => None
    }
  }
}

case class PayeDesignatoryDetails(name: String)

case class TaxCode(code: String)

