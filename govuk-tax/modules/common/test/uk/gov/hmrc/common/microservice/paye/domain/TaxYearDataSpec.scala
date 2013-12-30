package uk.gov.hmrc.common.microservice.paye.domain

import uk.gov.hmrc.common.BaseSpec
import org.joda.time.LocalDate

class TaxYearDataSpec extends BaseSpec {
  // This is the minumum set of data that must be supplied to be able to create a CarBenefit
  val testCar = Car(carValue = Some(3000), dateCarRegistered = Some(new LocalDate), fuelType = Some("Diesel"), dateCarMadeAvailable = Some(new LocalDate))

  "TaxYearData" should {
    "return None when asked for active benefit of type FUEL if fuel benefit is present but withdrawn" in {
      val car = Benefit(BenefitTypes.CAR, 2013, 100, 1, car = Some(testCar))
      val fuel = Benefit(BenefitTypes.FUEL, 2013, 100, 1, dateWithdrawn = Some(new LocalDate()))

      val carBenefit = CarBenefit(car, Some(fuel))

      val tyd = TaxYearData(Seq(carBenefit), Seq())

      tyd.findActiveFuelBenefit(1) shouldBe None
    }

    "return None when asked for active benefit of type FUEL if fuel benefit is not present" in {
      val car = Benefit(BenefitTypes.CAR, 2013, 100, 1, car = Some(testCar))
      val fuel = None

      val carBenefit = CarBenefit(car, fuel)

      val tyd = TaxYearData(Seq(carBenefit), Seq())

      tyd.findActiveFuelBenefit(1) shouldBe None
    }

    "return some fuel benefit when asked for active benefit of type FUEL if fuel benefit is present and not withdrawn" in {
      val car = Benefit(BenefitTypes.CAR, 2013, 100, 1, car = Some(testCar))
      val fuel = Benefit(BenefitTypes.FUEL, 2013, 100, 1)

      val carBenefit = CarBenefit(car, Some(fuel))

      val tyd = TaxYearData(Seq(carBenefit), Seq())

      tyd.findActiveFuelBenefit(1) shouldBe Some(FuelBenefit.fromBenefit(fuel))
    }

    "return some car benefit when asked for active benefit of type CAR if car is not withdrawn" in {
      val car = Benefit(BenefitTypes.CAR, 2013, 100, 1, car = Some(testCar))
      val carBenefit = CarBenefit(car, None)

      val tyd = TaxYearData(Seq(carBenefit), Seq())

      tyd.findActiveCarBenefit(1) shouldBe Some(carBenefit)
    }

    "return None when asked for active benefit of type CAR if car is withdrawn" in {
      val car = Benefit(BenefitTypes.CAR, 2013, 100, 1, car = Some(testCar.copy(dateCarWithdrawn = Some(new LocalDate()))))
      val carBenefit = CarBenefit(car, None)

      val tyd = TaxYearData(Seq(carBenefit), Seq())

      tyd.findActiveCarBenefit(1) shouldBe None
    }
  }

}
