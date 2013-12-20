package models.paye

import org.joda.time.LocalDate
import uk.gov.hmrc.common.microservice.paye.domain._
import uk.gov.hmrc.common.microservice.paye.domain.BenefitTypes._
import controllers.paye.FuelBenefitData
import models.paye.CarBenefitData
import scala.Some
import uk.gov.hmrc.common.microservice.paye.domain.Car
import controllers.paye.FuelBenefitData
import uk.gov.hmrc.common.microservice.paye.domain.CarAndFuel

case class CarBenefitData(providedFrom: Option[LocalDate],
                          carRegistrationDate: Option[LocalDate],
                          listPrice: Option[Int],
                          employeeContributes: Option[Boolean],
                          employeeContribution: Option[Int],
                          employerContributes: Option[Boolean],
                          employerContribution: Option[Int],
                          fuelType: Option[String],
                          co2Figure: Option[Int],
                          co2NoFigure: Option[Boolean],
                          engineCapacity: Option[String],
                          employerPayFuel: Option[String],
                          dateFuelWithdrawn: Option[LocalDate])

case class CarBenefitDataAndCalculations(carBenefitData : CarBenefitData)

object CarBenefitBuilder {
  def apply(carBenefitData: CarBenefitData, taxYear: Int, employmentSequenceNumber: Int): CarBenefit = {
    val car = createCar(carBenefitData)

    val carBenefit = createBenefit(CAR, None, taxYear, employmentSequenceNumber, Some(car), 0, None)

    val fuelBenefit = carBenefitData.employerPayFuel match {
      case Some(data) if data == "true" || data == "again" => Some(createBenefit(benefitType = 29, withdrawnDate = None, taxYear = taxYear,
        employmentSeqNumber =  employmentSequenceNumber, car = Some(car), 0, None))
      case Some("date") => Some(createBenefit(benefitType = 29, withdrawnDate = carBenefitData.dateFuelWithdrawn, taxYear = taxYear,
        employmentSeqNumber =  employmentSequenceNumber, car = Some(car), 0, None))
      case _ => None
    }
    CarBenefit.fromBenefits(carBenefit, fuelBenefit)
  }

  def apply(addFuelBenefit: FuelBenefitData, carBenefit: Benefit, taxYear: Int, employmentSequenceNumber: Int) : CarBenefit  = {
    val car = carBenefit.car
    val fuelBenefit = addFuelBenefit.employerPayFuel match {

      case Some("true" | "again") => Some(createBenefit(FUEL, carBenefit.dateWithdrawn, taxYear, employmentSequenceNumber, car, 0, None))

      case Some("date") => Some(createBenefit(FUEL, addFuelBenefit.dateFuelWithdrawn, taxYear, employmentSequenceNumber, car, 0, None))

      case _ => None
    }
    CarBenefit.fromBenefits(carBenefit, fuelBenefit)
  }


  private def createCar(carBenefitData: CarBenefitData) = {
    Car(dateCarMadeAvailable = carBenefitData.providedFrom,
      dateCarWithdrawn = None,
      dateCarRegistered = carBenefitData.carRegistrationDate,
      employeeCapitalContribution = carBenefitData.employeeContribution.map(BigDecimal(_)),
      fuelType = carBenefitData.fuelType,
      co2Emissions = carBenefitData.co2Figure,
      engineSize = engineSize(carBenefitData.engineCapacity),
      mileageBand = None,
      carValue = carBenefitData.listPrice.map(BigDecimal(_)),
      employeePayments = carBenefitData.employerContribution.map(BigDecimal(_)),
      daysUnavailable = None)
  }

  private def engineSize(engineCapacity: Option[String]) : Option[Int] = {
    engineCapacity match {
      // TODO: Investigate why keystore is returning a string value 'none' instead of an Option None value
      case Some(engine) if engine != EngineCapacity.NOT_APPLICABLE => Some(engine.toInt)
      case _ => None
    }
  }

  private def createBenefit(benefitType: Int, withdrawnDate: Option[LocalDate], taxYear: Int, employmentSeqNumber: Int, car: Option[Car], benefitAmount : Int, forecastBenefitAmount: Option[Int]) = {
    Benefit(benefitType = benefitType,
      taxYear = taxYear,
      grossAmount = 0,
      employmentSequenceNumber = employmentSeqNumber,
      costAmount = None,
      amountMadeGood = None,
      cashEquivalent = None,
      expensesIncurred = None,
      amountOfRelief = None,
      paymentOrBenefitDescription = None,
      dateWithdrawn = withdrawnDate,
      car = car,
      actions = Map.empty[String, String],
      calculations = Map.empty[String, String],
      benefitAmount = Some(benefitAmount),
      forecastAmount = forecastBenefitAmount)
  }
}