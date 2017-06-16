package controllers.internal

import model.HostContext
import uk.gov.hmrc.abtest.{CohortCalculator, Cohorts}
import uk.gov.hmrc.domain.SaUtr

trait OptInCohortCalculator extends CohortCalculator[SaUtr, OptInCohort] {
  def calculateCohort(hostContext: HostContext): OptInCohort =
    if (hostContext.isTaxCredits) TCPage
    else IPage

  def cohorts: Cohorts[OptInCohort] = OptInCohortConfigurationValues.cohorts
}
