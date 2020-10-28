/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers.internal

import uk.gov.hmrc.abtest.ConfiguredCohortValues

object OptInCohortConfigurationValues extends ConfiguredCohortValues[OptInCohort] {
  val availableValues = List(IPage7, IPage8, TCPage9, ReOptInPage10, ReOptInPage52)
}
