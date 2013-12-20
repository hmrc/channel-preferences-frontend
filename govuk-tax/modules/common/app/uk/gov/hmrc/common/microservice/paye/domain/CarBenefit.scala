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
                      fuelBenefit: Option[FuelBenefit] = None) {
  def isActive = dateWithdrawn.isEmpty

  def hasActiveFuel = fuelBenefit.map(_.isActive).getOrElse(false)

  val benefitCode = BenefitTypes.CAR

  // Convert back to legacy structure
  def toBenefits: Seq[Benefit] = {

    val car = new Benefit(benefitCode, taxYear, grossAmount, employmentSequenceNumber, None, None, None, None, None, None, dateWithdrawn,
      Some(Car(Some(dateMadeAvailable), dateWithdrawn, Some(dateCarRegistered), Some(employeeCapitalContribution),
        Some(fuelType), co2Emissions, engineSize, None, Some(carValue), Some(employeePayments), None)),
      Map.empty, Map.empty, Some(benefitAmount), None)

    val fuel = fuelBenefit.map { fb =>
      new Benefit(fb.benefitCode, taxYear, fb.grossAmount, employmentSequenceNumber, None, None, None, None, None, None, fb.dateWithdrawn, None, Map.empty, Map.empty, Some(fb.benefitAmount), None)
    }

    Seq(car) ++ fuel
  }

}


object CarBenefit {
  def fromCarAndFuel(carAndFuel: CarAndFuel): CarBenefit = {
    CarBenefit.fromBenefits(carAndFuel.carBenefit, carAndFuel.fuelBenefit)
  }

  def fromBenefits(carBenefit: Benefit, fuelBenefit: Option[Benefit]) = {
    CarBenefit.fromBenefit(carBenefit, fuelBenefit.map(FuelBenefit.fromBenefit(_)))
  }

  def fromBenefit(benefit: Benefit, fuelBenefit: Option[FuelBenefit] = None): CarBenefit = {
    require(benefit.benefitType == BenefitTypes.CAR, s"Attempted to create a CarBenefit from a Benefit with type ${benefit.benefitType}")
    require(benefit.car.isDefined, "Attempted to create a CarBenefit from a benefit without a Car")

    val car = benefit.car.get // CarAndFuel enforces presence of car element

    // We're doing naked gets on some of the optional fields from the benefit. These fields
    // must be supplied, according to the business model. This is the point where we are
    // making sure the business model is expressed correctly
    CarBenefit(benefit.taxYear,
      benefit.employmentSequenceNumber,
      benefit.getStartDate(new LocalDate()),
      car.dateCarMadeAvailable.get,
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
      fuelBenefit)
  }
}

case class FuelBenefit(startDate: LocalDate, benefitAmount: BigDecimal, grossAmount: BigDecimal, dateWithdrawn: Option[LocalDate] = None) {
  def isActive = dateWithdrawn.isEmpty

  val benefitCode = BenefitTypes.FUEL
}

object FuelBenefit {
  def fromBenefit(benefit: Benefit): FuelBenefit = {
    require(benefit.benefitType == BenefitTypes.FUEL)

    FuelBenefit(benefit.getStartDate(TaxYearResolver.startOfCurrentTaxYear), benefit.benefitAmount.getOrElse(0), benefit.grossAmount, benefit.dateWithdrawn)
  }
}
