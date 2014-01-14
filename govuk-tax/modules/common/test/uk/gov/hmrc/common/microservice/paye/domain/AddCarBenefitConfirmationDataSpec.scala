package uk.gov.hmrc.common.microservice.paye.domain

import org.scalatest.{Matchers, WordSpecLike}


class AddCarBenefitConfirmationDataSpec extends WordSpecLike with Matchers {
  import AddCarBenefitConfirmationData._

  "convertEmployerPayFuel" should {
    "turn a fuel type of electricity into a None" in {
      convertEmployerPayFuel(Some(fuelTypeElectric), None) shouldBe None
    }

    "turn a fuel type of diesel and employerPayFuel of None into a None" in {
      convertEmployerPayFuel(Some("diesel"), None) shouldBe None
    }

    "turn a fuel type of diesel and employerPayFuel of Some(true) into a Some(true)" in {
      convertEmployerPayFuel(Some("diesel"), Some("true")) shouldBe Some(true)
    }
  }

}
