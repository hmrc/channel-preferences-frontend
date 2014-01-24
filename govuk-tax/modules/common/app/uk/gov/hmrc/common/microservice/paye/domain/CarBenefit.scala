package uk.gov.hmrc.common.microservice.paye.domain

import org.joda.time.LocalDate
import uk.gov.hmrc.utils.TaxYearResolver

case class CarBenefit(taxYear: Int,
                      employmentSequenceNumber: Int,
                      startDate: LocalDate,
                      dateMadeAvailable: LocalDate,
                      benefitAmount: BigDecimal,
                      grossAmount: BigDecimal,
                      fuelType: String,
                      engineSize: Option[Int], // can be None for fuel type "electric"
                      co2Emissions: Option[Int],
                      carValue: BigDecimal,
                      employeePayments: BigDecimal, // Can be zero
                      employeeCapitalContribution: BigDecimal, // Can be zero
                      dateCarRegistered: LocalDate,
                      dateWithdrawn: Option[LocalDate] = None,
                      actions: Map[String, String] = Map.empty,
                      fuelBenefit: Option[FuelBenefit] = None,
                      daysUnavailable: Option[Int] = None) {
  def isActive = dateWithdrawn.isEmpty

  lazy val hasActiveFuel = fuelBenefit.exists(_.isActive)

  lazy val activeFuelBenefit = fuelBenefit.filter(_.isActive)

  val benefitCode = BenefitTypes.CAR

  // Convert back to legacy structure
  def toBenefits: Seq[Benefit] = {
    val legacyCar = Car(Some(dateMadeAvailable), dateWithdrawn, Some(dateCarRegistered), Some(employeeCapitalContribution),
      Some(fuelType), co2Emissions, engineSize, None, Some(carValue), Some(employeePayments), None)

    val carLegacyBenefit = new Benefit(benefitCode, taxYear, grossAmount, employmentSequenceNumber, None, None, None, None, None, None, dateWithdrawn,
      Some(legacyCar), actions, Map.empty, Some(benefitAmount), None)

    val fuelLegacyBenefit = fuelBenefit.map {
      fb =>
        new Benefit(fb.benefitCode, taxYear, fb.grossAmount, employmentSequenceNumber, None, None, None, None, None, None, fb.dateWithdrawn, Some(legacyCar), fb.actions, Map.empty, Some(fb.benefitAmount), None)
    }

    Seq(carLegacyBenefit) ++ fuelLegacyBenefit
  }

}


object CarBenefit {
  def apply(carAndFuel: CarAndFuel): CarBenefit = {
    CarBenefit(carAndFuel.carBenefit, carAndFuel.fuelBenefit)
  }

  def apply(benefit: Benefit): CarBenefit = {
    apply(benefit, None)
  }

  def apply(benefit: Benefit, fuelBenefit: Option[Benefit]): CarBenefit = {
    require(benefit.benefitType == BenefitTypes.CAR, s"Attempted to create a CarBenefit from a Benefit with type ${benefit.benefitType}")
    require(benefit.car.isDefined, "Attempted to create a CarBenefit from a benefit without a Car")

    val fb = fuelBenefit.map(FuelBenefit.fromBenefit)

    val car = benefit.car.get // CarAndFuel enforces presence of car element

    // We're doing naked gets on some of the optional fields from the benefit. These fields
    // must be supplied, according to the business model. This is the point where we are
    // making sure the business model is expressed correctly
    CarBenefit(benefit.taxYear,
      benefit.employmentSequenceNumber,
      benefit.getStartDate(TaxYearResolver.startOfCurrentTaxYear), // TODO: Some tests may need a different TYR.
      car.dateCarMadeAvailable.getOrElse(TaxYearResolver.startOfCurrentTaxYear),
      benefit.benefitAmount.getOrElse(0),
      benefit.grossAmount,
      car.fuelType.get,
      car.engineSize,
      car.co2Emissions,
      car.carValue.get,
      car.employeePayments.getOrElse(0),
      car.employeeCapitalContribution.getOrElse(0),
      car.dateCarRegistered.get,
      car.dateCarWithdrawn,
      benefit.actions,
      fb,
      car.daysUnavailable)
  }
}

case class FuelBenefit(startDate: LocalDate,
                       benefitAmount: BigDecimal,
                       grossAmount: BigDecimal,
                       dateWithdrawn: Option[LocalDate] = None,
                       actions: Map[String, String] = Map.empty) {
  def isActive = dateWithdrawn.isEmpty

  val benefitCode = BenefitTypes.FUEL
}

object FuelBenefit {
  def fromBenefit(benefit: Benefit): FuelBenefit = {
    require(benefit.benefitType == BenefitTypes.FUEL)

    FuelBenefit(benefit.getStartDate(TaxYearResolver.startOfCurrentTaxYear),
      benefit.benefitAmount.getOrElse(0),
      benefit.grossAmount,
      benefit.dateWithdrawn,
      benefit.actions)
  }
}
