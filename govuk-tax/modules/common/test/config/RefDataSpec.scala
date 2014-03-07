package config

import config.RefData._
import uk.gov.hmrc.common.BaseSpec

class RefDataSpec extends BaseSpec {

  "The reference data" should {

    "be loaded from classpath" in {

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

      iabdTypeFor(0) shouldBe "Unknown"
      iabdTypeFor(-1) shouldBe "Unknown"
      iabdTypeFor(300) shouldBe "Unknown"

    }
  }

}
