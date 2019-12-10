/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.internal

import model.HostContext
import org.scalatestplus.play.PlaySpec

class OptInCohortCalculatorSpec extends PlaySpec {

  "The calculator" should {
    "Return the paperless interrupt if t&c is generic" in {
      new OptInCohortCalculator {}.calculateCohort(HostContext("", "", Some("generic"))) mustBe IPage
    }
    "Return the taxCredits interrupt if t&c is taxCredits" in {
      new OptInCohortCalculator {}.calculateCohort(HostContext("", "", Some("taxCredits"))) mustBe TCPage
    }
  }

}
