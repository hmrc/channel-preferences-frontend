/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.abtest

class Cohorts[C <: Cohort](val first: C, val others: C*) {
  val values = (first +: others).toSet
}

object Cohorts {
  def apply[C <: Cohort](first: C, others: C*): Cohorts[C] = new Cohorts(first, others: _*)
}
