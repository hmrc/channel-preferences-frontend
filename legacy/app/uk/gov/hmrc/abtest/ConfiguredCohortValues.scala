/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.abtest

trait ConfiguredCohortValues[C <: Cohort] extends CohortValues[C] with CohortConfiguration[C]
