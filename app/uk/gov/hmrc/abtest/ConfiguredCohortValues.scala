package uk.gov.hmrc.abtest

trait ConfiguredCohortValues[C <: Cohort] extends CohortValues[C] with CohortConfiguration[C]
