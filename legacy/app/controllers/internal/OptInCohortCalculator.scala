/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.abtest.{ CohortCalculator, Cohorts }
import uk.gov.hmrc.domain.SaUtr

trait OptInCohortCalculator extends CohortCalculator[SaUtr, OptInCohort] {
  def calculateCohort(hostContext: HostContext): OptInCohort =
    hostContext.cohort
      .getOrElse(
        if (hostContext.isTaxCredits) CohortCurrent.tcpage
        else CohortCurrent.ipage
      )

  def cohorts: Cohorts[OptInCohort] = OptInCohortConfigurationValues.cohorts
}