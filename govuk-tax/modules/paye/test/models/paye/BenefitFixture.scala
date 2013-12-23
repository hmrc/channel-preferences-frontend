package models.paye

import controllers.paye.FuelBenefitData
import uk.gov.hmrc.common.microservice.paye.domain.{Car, BenefitTypes, CarAndFuel, Benefit}
import org.joda.time.LocalDate
import uk.gov.hmrc.common.microservice.paye.domain

object BenefitFixture {
  val carBenefitRegisteredDate = new LocalDate(2013, 1, 2)
  val carBenefitRegisteredDateString = "2 January 2013"
  val carBenefitAvailableDate = new LocalDate(2013, 1, 1)
  val carBenefitAvailableDateString = "1 January 2013"
  val employmentSequenceNumber = 156
  val currentTaxYear = 2013
  val carBenefitAmount: Option[BigDecimal] = Some(5000)
  val carForecastAmount: Option[Int] = Some(6000)
  val carEngineSize = 1400
  val carValue = 20000
  val carValuePounds = "£20,000"
  val carEmployeeCapitalContributionVaue = 10000
  val carEmployeeCapitalContributionVauePounds = "£10,000"
  val carEmployeePrivateUseContributionVaue = 5000
  val carEmployeePrivateUseContributionVauePounds = "£5,000"
  val carFuelType = "diesel"
  val carCo2Emissions = 125

  val car = {
    Car(
      Some(carBenefitAvailableDate),
      None,
      Some(carBenefitRegisteredDate),
      Some(carEmployeeCapitalContributionVaue),
      Some(carFuelType),
      Some(carCo2Emissions),
      Some(carEngineSize),
      Some("a car mileage band"),
      Some(carValue),
      Some(carEmployeePrivateUseContributionVaue),
      Some(15)
    )
  }

  val carBenefit = Benefit(
    BenefitTypes.CAR,
    currentTaxYear,
    3000,
    employmentSequenceNumber,
    Some(2000),
    Some(3000),
    Some(4000),
    Some(5000),
    Some(6000),
    Some("Car benefit description"),
    None,
    Some(car),
    Map.empty,
    Map.empty,
    carBenefitAmount,
    carForecastAmount)

  val fuelBenefitAmount = 250
  val fuelBenefitAmountPounds = "£250"
  val fuelForecastAmount = 270

  val fuelBenefit = Benefit(
    BenefitTypes.FUEL,
    currentTaxYear,
    300,
    employmentSequenceNumber,
    Some(200),
    Some(300),
    Some(400),
    Some(500),
    Some(600),
    Some("Fuel benefit description"),
    None,
    None,
    Map.empty,
    Map.empty,
    Some(fuelBenefitAmount),
    Some(fuelForecastAmount)
  )

  val carWithoutFuel = CarAndFuel(carBenefit)

  val carWithFuel = CarAndFuel(carBenefit, Some(fuelBenefit))

}
