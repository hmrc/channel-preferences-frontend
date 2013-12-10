package uk.gov.hmrc.common.microservice.paye.domain

import uk.gov.hmrc.common.BaseSpec
import org.joda.time.LocalDate

class CarAndFuelSpec extends BaseSpec {

  "CarAndFuel" should {
    "be active if the Car benefit does not have a withdrawn date" in {
      val carBenefit = Benefit(BenefitTypes.CAR, 2013, 100, 1, car = Some(Car()))
      CarAndFuel(carBenefit).isActive shouldBe true
    }

    "be inactive if the Car benefit has a withdrawn date" in {
      val carBenefit = Benefit(BenefitTypes.CAR, 2013, 100, 1, dateWithdrawn = Some(new LocalDate()), car = Some(Car()))
      CarAndFuel(carBenefit).isActive shouldBe false
    }

    "turn a car benefit with no fuel benefit into a sequence of 1 Benefit" in {
      val carBenefit = Benefit(BenefitTypes.CAR, 2013, 100, 1, car = Some(Car()))
      val benefits = CarAndFuel(carBenefit).toSeq

      benefits.length shouldBe 1
      benefits(0).benefitType shouldBe BenefitTypes.CAR
    }

    "turn a car benefit with a fuel benefit into a sequence of s Benefits" in {
      val carBenefit = Benefit(BenefitTypes.CAR, 2013, 100, 1, car = Some(Car()))
      val fuelBenefit = Benefit(BenefitTypes.FUEL, 2013, 100, 1)
      val benefits = CarAndFuel(carBenefit, Some(fuelBenefit)).toSeq

      benefits.length shouldBe 2
      benefits(0).benefitType shouldBe BenefitTypes.CAR
      benefits(1).benefitType shouldBe BenefitTypes.FUEL
    }
  }

}
