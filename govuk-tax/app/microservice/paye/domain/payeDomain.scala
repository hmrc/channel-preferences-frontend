package microservice.paye.domain

import microservice.domain.{ RegimeRoot, TaxRegime }
import microservice.paye.PayeMicroService
import org.joda.time.LocalDate

class PayeRegime extends TaxRegime

case class PayeRoot(nino: String, version: Int, name: String, links: Map[String, String]) extends RegimeRoot {

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
case class Benefit(benefitType: Int, taxYear: Int, grossAmount: BigDecimal, employmentSequenceNumber: Int, car: Option[Car], actions: Map[String, String], calculations: Map[String, String]) {
  def grossAmountToString(format: String = "%.2f") = format.format(grossAmount)
}
case class Car(dateCarMadeAvailable: Option[LocalDate], dateCarWithdrawn: Option[LocalDate], dateCarRegistered: Option[LocalDate], employeeCapitalContribution: BigDecimal, fuelType: Int, co2Emissions: Int, engineSize: Int, mileageBand: String, carValue: BigDecimal)
case class RemoveCarBenefit(version: Int, benefit: Benefit, revisedAmount: BigDecimal, withdrawDate: LocalDate)

case class Employment(sequenceNumber: Int, startDate: LocalDate, endDate: Option[LocalDate], taxDistrictNumber: String, payeNumber: String, employerName: String)
