package microservice.paye.domain

import microservice.domain.{ RegimeRoot, TaxRegime }
import microservice.paye.PayeMicroService

class PayeRegime extends TaxRegime

case class PayeRoot(name: String, links: Map[String, String]) extends RegimeRoot {

  def taxCodes(implicit payeMicroService: PayeMicroService): Seq[TaxCode] = {
    resourceFor[Seq[TaxCode]]("taxCode").getOrElse(Seq.empty)
  }

  def benefits(implicit payeMicroService: PayeMicroService): Seq[Benefit] = {
    resourceFor[Seq[Benefit]]("benefits").getOrElse(Seq.empty)
  }

  def employments(implicit payeMicroService: PayeMicroService): Seq[Employment] = {
    resourceFor[Seq[Employment]]("employments").getOrElse(Seq.empty)
  }

  private def resourceFor[T](resource: String)(implicit payeMicroService: PayeMicroService, m: Manifest[T]): Option[T] = {
    links.get(resource) match {
      case Some(uri) => payeMicroService.linkedResource[T](uri)
      case _ => None
    }
  }
}

case class TaxCode(taxCode: String)
case class Benefit(benefitType: Int, taxYear: String, grossAmount: Long, employmentSequenceNumber: Int, cars: Seq[Car]) {
  lazy val grossAmountToMoney = BigDecimal(grossAmount) / 100
  def grossAmountToString(format: String = "%.2f") = format.format(grossAmountToMoney)
}
case class Car(engineSize: Int, fuelType: Int, dateCarRegistered: String)

case class Employment(sequenceNumber: Int, startDate: String, endDate: String, taxDistrictNumber: String, payeNumber: String)

