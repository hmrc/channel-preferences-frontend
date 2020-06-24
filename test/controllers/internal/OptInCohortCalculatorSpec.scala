/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers.internal

import model.HostContext
import org.scalatestplus.play.PlaySpec

class OptInCohortCalculatorSpec extends PlaySpec {

  "The calculator" should {
    "Return the paperless interrupt if t&c is generic" in {
      new OptInCohortCalculator {}.calculateCohort(HostContext("", "", Some("generic"))) mustBe CohortCurrent.ipage
    }
    "Return the taxCredits interrupt if t&c is taxCredits" in {
      new OptInCohortCalculator {}.calculateCohort(HostContext("", "", Some("taxCredits"))) mustBe CohortCurrent.tcpage
    }
  }

}
