/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.abtest

trait Cohort {
  def name: String

  override def toString = name
}
