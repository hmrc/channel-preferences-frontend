/*
 * Copyright 2021 HM Revenue & Customs
 *
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
