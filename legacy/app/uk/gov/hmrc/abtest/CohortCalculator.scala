/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.abtest

trait CohortCalculator[K, C <: Cohort] {
  def cohorts: Cohorts[C]

  def calculate(id: K): C = cohorts.values.drop(math.abs(id.hashCode) % cohorts.values.size).head
}
