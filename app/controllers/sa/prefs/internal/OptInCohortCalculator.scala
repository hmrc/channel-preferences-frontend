package controllers.sa.prefs.internal

import uk.gov.hmrc.abtest.{CohortCalculator, Cohorts}
import uk.gov.hmrc.play.frontend.auth.User

trait OptInCohortCalculator extends CohortCalculator[OptInCohort] {
  def calculateCohort(user: User): OptInCohort = user.userAuthority.accounts.sa.map(sa => calculate(sa.utr)).getOrElse(cohorts.first)
  def cohorts: Cohorts[OptInCohort] = OptInCohortConfigurationValues.cohorts
}
