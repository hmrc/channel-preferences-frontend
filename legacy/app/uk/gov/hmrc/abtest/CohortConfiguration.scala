/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.abtest

import play.api.Play

trait CohortConfiguration[C <: Cohort] {
  self: CohortValues[C] =>

  def isEnabled(cohort: C) =
    Play.current.configuration.getBoolean(s"abTesting.cohort.${cohort.toString}.enabled").getOrElse(false)

  def verifyConfiguration(): Unit = enabledCohorts
}
