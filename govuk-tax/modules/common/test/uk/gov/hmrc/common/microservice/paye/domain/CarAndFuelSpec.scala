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
      val carBenefit = Benefit(BenefitTypes.CAR, 2013, 100, 1, car = Some(Car(dateCarWithdrawn = Some(new LocalDate()))))
      CarAndFuel(carBenefit).isActive shouldBe false
    }

    "have active fuel fuelBenefit is defined and has not withdrawn date" in {
      val carBenefit = Benefit(BenefitTypes.CAR, 2013, 100, 1, car = Some(Car()))
      val fuelBenefit = Benefit(BenefitTypes.FUEL, 2013, 200, 1)

      CarAndFuel(carBenefit, Some(fuelBenefit)).hasActiveFuel shouldBe true
    }

    "not have active fuel if fuelBenefit is defined but has a withdrawn date" in {
      val carBenefit = Benefit(BenefitTypes.CAR, 2013, 100, 1, car = Some(Car()))
      val fuelBenefit = Benefit(BenefitTypes.FUEL, 2013, 200, 1, dateWithdrawn = Some(new LocalDate))

      CarAndFuel(carBenefit, Some(fuelBenefit)).hasActiveFuel shouldBe false
    }

    "not have active fuel if fuelBenefit is not defined" in {
      val carBenefit = Benefit(BenefitTypes.CAR, 2013, 100, 1, car = Some(Car()))

      CarAndFuel(carBenefit, None).hasActiveFuel shouldBe false
    }

    "turn a car benefit with no fuel benefit into a sequence of 1 Benefit" in {
      val carBenefit = Benefit(BenefitTypes.CAR, 2013, 100, 1, car = Some(Car()))
      val benefits = CarAndFuel(carBenefit).toSeq

      benefits.length shouldBe 1
      benefits(0).benefitType shouldBe BenefitTypes.CAR
    }

    "turn a car benefit with a fuel benefit into a sequence of 2 Benefits" in {
      val carBenefit = Benefit(BenefitTypes.CAR, 2013, 100, 1, car = Some(Car()))
      val fuelBenefit = Benefit(BenefitTypes.FUEL, 2013, 100, 1)
      val benefits = CarAndFuel(carBenefit, Some(fuelBenefit)).toSeq

      benefits.length shouldBe 2
      benefits(0).benefitType shouldBe BenefitTypes.CAR
      benefits(1).benefitType shouldBe BenefitTypes.FUEL
    }
  }

}
