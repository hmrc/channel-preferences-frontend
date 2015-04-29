package controllers.sa.prefs.internal

import uk.gov.hmrc.abtest.{CohortCalculator, Cohorts}
import uk.gov.hmrc.play.frontend.auth.AuthContext

trait OptInCohortCalculator extends CohortCalculator[OptInCohort] {
  def calculateCohort(authContext: AuthContext): OptInCohort = authContext.principal.accounts.sa.map(sa => calculate(sa.utr)).getOrElse(cohorts.first)
  def cohorts: Cohorts[OptInCohort] = OptInCohortConfigurationValues.cohorts
}
