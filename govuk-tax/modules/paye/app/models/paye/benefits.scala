package models.paye

import org.joda.time.LocalDate
import controllers.paye.{TaxYearSupport, BenefitValue}
import uk.gov.hmrc.common.microservice.paye.domain.{Benefit, CarAndFuel, BenefitTypes}


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

  val benefitValue: BenefitValue = BenefitValue(grossAmount)
  val fuelBenefitValue: Option[BenefitValue] = fuelBenefit.map(b => BenefitValue(b.grossAmount))
}


object CarBenefit extends TaxYearSupport {
  def fromCarAndFuel(carAndFuel: CarAndFuel): CarBenefit = {
    val fuelBenefit = carAndFuel.fuelBenefit.map(FuelBenefit.fromBenefit(_))

    import carAndFuel.carBenefit
    val car = carBenefit.car.get // CarAndFuel enforces presence of car element

    // We're doing naked gets on some of the optional fields from the benefit. These fields
    // must be supplied, according to the business model. This is the point where we are
    // making sure the business model is expressed correctly
    CarBenefit(carBenefit.taxYear,
      carBenefit.employmentSequenceNumber,
      carBenefit.getStartDate(startOfCurrentTaxYear),
      car.dateCarMadeAvailable.get,
      carBenefit.benefitAmount.getOrElse(0),
      carBenefit.grossAmount,
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

object FuelBenefit extends TaxYearSupport {
  def fromBenefit(benefit: Benefit): FuelBenefit = {
    require(benefit.benefitType == BenefitTypes.FUEL)

    FuelBenefit(benefit.getStartDate(startOfCurrentTaxYear), benefit.benefitAmount.getOrElse(0), benefit.grossAmount, benefit.dateWithdrawn)
  }
}

