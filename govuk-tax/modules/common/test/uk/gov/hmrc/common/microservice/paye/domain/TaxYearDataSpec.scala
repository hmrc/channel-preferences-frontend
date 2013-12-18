package uk.gov.hmrc.common.microservice.paye.domain

import uk.gov.hmrc.common.BaseSpec
import org.joda.time.LocalDate

class TaxYearDataSpec extends BaseSpec {

  "TaxYearData" should {
    "return None when asked for active benefit of type FUEL if fuel benefit is present but withdrawn" in {
      val car = Car()
      val carBenefit = Benefit(BenefitTypes.CAR, 2013, 100, 1, car = Some(car))
      val fuelBenefit = Benefit(BenefitTypes.FUEL, 2013, 100, 1, dateWithdrawn = Some(new LocalDate()))
      val carAndFuel = CarAndFuel(carBenefit, Some(fuelBenefit))

      val tyd = TaxYearData(Seq(carAndFuel), Seq())

      tyd.findActiveBenefit(1, BenefitTypes.FUEL) shouldBe None
    }

    "return None when asked for active benefit of type FUEL if fuel benefit is not present" in {
      val car = Car()
      val carBenefit = Benefit(BenefitTypes.CAR, 2013, 100, 1, car = Some(car))
      val carAndFuel = CarAndFuel(carBenefit, None)

      val tyd = TaxYearData(Seq(carAndFuel), Seq())

      tyd.findActiveBenefit(1, BenefitTypes.FUEL) shouldBe None
    }

    "return some fuel benefit when asked for active benefit of type FUEL if fuel benefit is present and not withdrawn" in {
      val car = Car()
      val carBenefit = Benefit(BenefitTypes.CAR, 2013, 100, 1, car = Some(car))
      val fuelBenefit = Benefit(BenefitTypes.FUEL, 2013, 100, 1)
      val carAndFuel = CarAndFuel(carBenefit, Some(fuelBenefit))

      val tyd = TaxYearData(Seq(carAndFuel), Seq())

      tyd.findActiveBenefit(1, BenefitTypes.FUEL) shouldBe Some(fuelBenefit)
    }

    "return some car benefit when asked for active benefit of type CAR if car is not withdrawn" in {
      val car = Car()
      val carBenefit = Benefit(BenefitTypes.CAR, 2013, 100, 1, car = Some(car))
      val carAndFuel = CarAndFuel(carBenefit)

      val tyd = TaxYearData(Seq(carAndFuel), Seq())

      tyd.findActiveBenefit(1, BenefitTypes.CAR) shouldBe Some(carBenefit)
    }

    "return None when asked for active benefit of type CAR if car is withdrawn" in {
      val car = Car(dateCarWithdrawn = Some(new LocalDate()))
      val carBenefit = Benefit(BenefitTypes.CAR, 2013, 100, 1, car = Some(car))
      val carAndFuel = CarAndFuel(carBenefit)

      val tyd = TaxYearData(Seq(carAndFuel), Seq())

      tyd.findActiveBenefit(1, BenefitTypes.CAR) shouldBe None
    }
  }

}
