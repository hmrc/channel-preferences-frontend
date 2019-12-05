package controllers.internal

import model.HostContext
import uk.gov.hmrc.play.test.UnitSpec

class OptInCohortCalculatorSpec extends UnitSpec {

  "The calculator" should {
    "Return the paperless interrupt if t&c is generic" in {
      new OptInCohortCalculator{}.calculateCohort(HostContext("", "", Some("generic"))) shouldBe IPage
    }
    "Return the taxCredits interrupt if t&c is taxCredits" in {
      new OptInCohortCalculator{}.calculateCohort(HostContext("", "", Some("taxCredits"))) shouldBe TCPage
    }
  }

}
