package controllers.paye.validation

import uk.gov.hmrc.common.BaseSpec
import models.paye.EngineCapacity._

class EngineCapacitySpec extends BaseSpec {

  "EngineCapacity " should {
    "give an empty engine capacity " in {
      engineCapacityEmpty(None) shouldBe true
      engineCapacityEmpty(Some("none")) shouldBe true
      engineCapacityEmpty("none") shouldBe true
    }

    "give a non-empty engine capacity " in {
      engineCapacityEmpty(Some("123")) shouldBe false
      engineCapacityEmpty("456") shouldBe false
    }

    "map an empty engine capacity to None " in {
      mapEngineCapacityToInt(Some("none")) shouldBe None
      mapEngineCapacityToInt(None) shouldBe None
    }

    "map a non-empty engine capacity to the same value " in {
      mapEngineCapacityToInt(Some("123")) shouldBe Some(123)
    }
  }
}
