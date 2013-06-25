package config

import test.BaseSpec
import config.RefData._

class RefDataSpec extends BaseSpec {

  "The reference data" should {

    "be loaded from classpath" in {

      engineSizeFor(1) mustBe "0-1400 cc"
      engineSizeFor(2) mustBe "1401 - 2000 cc"
      engineSizeFor(3) mustBe "2000+ cc"

      fuelTypeFor(1) mustBe "None"
      fuelTypeFor(2) mustBe "Bi-Fuel"
      fuelTypeFor(3) mustBe "Combination"
      fuelTypeFor(4) mustBe "Diesel"
      fuelTypeFor(5) mustBe "Electric"
      fuelTypeFor(6) mustBe "Hybrid Electric"
      fuelTypeFor(7) mustBe "Low Emission Diesel"
      fuelTypeFor(8) mustBe "Petrol"
      fuelTypeFor(9) mustBe "E85 Bio-Ethanol"
      fuelTypeFor(10) mustBe "All Other"

      iabdTypeFor(1) mustBe "Gift Aid Payments"
      iabdTypeFor(13) mustBe "Married Couples Allowance (MAA)"
      iabdTypeFor(28) mustBe "Benefit in Kind"
      iabdTypeFor(29) mustBe "Car Fuel Benefit"
      iabdTypeFor(30) mustBe "Medical Insurance"
      iabdTypeFor(31) mustBe "Car Benefit"
      iabdTypeFor(35) mustBe "Van Benefit"
      iabdTypeFor(36) mustBe "Van Fuel Benefit"
      iabdTypeFor(86) mustBe "Trusts, Settlements & Estates at Trust Rate"
      iabdTypeFor(124) mustBe "Child Benefit"

    }

    "return Unknown if the code does not have a corresponding label" in {
      fuelTypeFor(0) mustBe "Unknown"
      fuelTypeFor(-1) mustBe "Unknown"
      fuelTypeFor(200) mustBe "Unknown"

      iabdTypeFor(0) mustBe "Unknown"
      iabdTypeFor(-1) mustBe "Unknown"
      iabdTypeFor(300) mustBe "Unknown"

      engineSizeFor(0) mustBe "Unknown"
      engineSizeFor(-1) mustBe "Unknown"
      engineSizeFor(300) mustBe "Unknown"
    }
  }

}
