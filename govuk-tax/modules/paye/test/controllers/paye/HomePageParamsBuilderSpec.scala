package controllers.paye

class HomePageParamsBuilderSpec extends PayeBaseSpec {
  "buildTotalBenefitValue" should {
    "return a benefit value containing the sum of the two non-None values passed in" in {
      HomePageParamsBuilder.buildTotalBenefitValue(Some(BenefitValue(1)), Some(BenefitValue(2))) shouldBe Some(BenefitValue(3))
    }

    "return None when the first parameter is None" in {
      HomePageParamsBuilder.buildTotalBenefitValue(None, Some(BenefitValue(2))) shouldBe None
    }

    "return None when the second parameter is None" in {
      HomePageParamsBuilder.buildTotalBenefitValue(Some(BenefitValue(1)), None) shouldBe None
    }

    "return None when the both parameters are None" in {
      HomePageParamsBuilder.buildTotalBenefitValue(None, None) shouldBe None
    }
  }
}
