package microservice.paye.domain

import microservice.domain.{ RegimeRoot, TaxRegime }

class PayeRegime extends TaxRegime

case class PayeRoot(designatoryDetails: PayeDesignatoryDetails,
    links: Map[String, String]) extends RegimeRoot {
}

case class PayeDesignatoryDetails(name: String)

case class TaxCode(code: String)

