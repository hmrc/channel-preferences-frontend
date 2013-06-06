package microservice.paye.domain

import microservice.domain.{ RegimeRoot, TaxRegime }
import microservice.paye.PayeMicroService

class PayeRegime extends TaxRegime

case class PayeRoot(designatoryDetails: PayeDesignatoryDetails, links: Map[String, String]) extends RegimeRoot {

  def taxCodes(implicit payeMicroService: PayeMicroService): Option[Seq[TaxCode]] = {
    resourceFor[Seq[TaxCode]]("taxCodes")
  }

  def benefits(implicit payeMicroService: PayeMicroService): Option[Seq[Benefit]] = {
    resourceFor[Seq[Benefit]]("benefits")
  }

  private def resourceFor[T](resource: String)(implicit payeMicroService: PayeMicroService, m: Manifest[T]): Option[T] = {
    links.get(resource) match {
      case Some(uri) => payeMicroService.linkedResource[T](uri)
      case _ => None
    }
  }

}

case class PayeDesignatoryDetails(firstName: String, lastName: String)

case class TaxCode(code: String)

case class Benefit(taxYear: String, grossAmount: Double)

