package config

import config.RefData._
import uk.gov.hmrc.common.BaseSpec

class RefDataSpec extends BaseSpec {

  "The reference data" should {

    "be loaded from classpath" in {

      engineSizeFor(1) shouldBe "0-1400 cc"
      engineSizeFor(2) shouldBe "1401 - 2000 cc"
      engineSizeFor(3) shouldBe "2000+ cc"

      fuelTypeFor(1) shouldBe "None"
      fuelTypeFor(2) shouldBe "Bi-Fuel"
      fuelTypeFor(3) shouldBe "Combination"
      fuelTypeFor(4) shouldBe "Diesel"
      fuelTypeFor(5) shouldBe "Electric"
      fuelTypeFor(6) shouldBe "Hybrid Electric"
      fuelTypeFor(7) shouldBe "Low Emission Diesel"
      fuelTypeFor(8) shouldBe "Petrol"
      fuelTypeFor(9) shouldBe "E85 Bio-Ethanol"
      fuelTypeFor(10) shouldBe "All Other"

      iabdTypeFor(1) shouldBe "Gift Aid Payments"
      iabdTypeFor(13) shouldBe "Married Couples Allowance (MAA)"
      iabdTypeFor(28) shouldBe "Benefit in Kind"
      iabdTypeFor(29) shouldBe "Car Fuel Benefit"
      iabdTypeFor(30) shouldBe "Medical Insurance"
      iabdTypeFor(31) shouldBe "Car Benefit"
      iabdTypeFor(35) shouldBe "Van Benefit"
      iabdTypeFor(36) shouldBe "Van Fuel Benefit"
      iabdTypeFor(86) shouldBe "Trusts, Settlements & Estates at Trust Rate"
      iabdTypeFor(124) shouldBe "Child Benefit"

    }

    "return Unknown if the code does not have a corresponding label" in {
      fuelTypeFor(0) shouldBe "Unknown"
      fuelTypeFor(-1) shouldBe "Unknown"
      fuelTypeFor(200) shouldBe "Unknown"

      iabdTypeFor(0) shouldBe "Unknown"
      iabdTypeFor(-1) shouldBe "Unknown"
      iabdTypeFor(300) shouldBe "Unknown"

      engineSizeFor(0) shouldBe "Unknown"
      engineSizeFor(-1) shouldBe "Unknown"
      engineSizeFor(300) shouldBe "Unknown"
    }
  }

}
