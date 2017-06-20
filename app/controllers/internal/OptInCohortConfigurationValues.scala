package controllers.internal

import uk.gov.hmrc.abtest.ConfiguredCohortValues

object OptInCohortConfigurationValues extends ConfiguredCohortValues[OptInCohort] {
  val availableValues = List(IPage, TCPage)
}
