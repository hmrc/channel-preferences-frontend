package uk.gov.hmrc.abtest

import org.scalatest.{LoneElement, Matchers, WordSpec}
import play.api.GlobalSettings
import play.api.test.{FakeApplication, WithApplication}

class OptInCohortValuesTest extends WordSpec with Matchers with LoneElement {

  object cohort1 extends Cohort {
    override def toString: String = "cohort1"
  }
  object cohort2 extends Cohort {
    override def toString: String = "cohort2"
  }

  "Opt-in cohorts" should {

    "balk when no cohorts enabled" in new WithApplication(FakeApplication(withGlobal = Some(new GlobalSettings {}), additionalConfiguration = Map(
      "abTesting.cohort.cohort1.enabled" -> false,
      "abTesting.cohort.cohort2.enabled" -> false))) with ConfiguredCohortValues[Cohort] {
      def availableValues: List[Cohort] = List(cohort1, cohort2)

      intercept[IllegalArgumentException] {
        verifyConfiguration()
      }
    }

    "default for single cohort" in new WithApplication(FakeApplication(withGlobal = Some(new GlobalSettings {}), additionalConfiguration = Map(
      "abTesting.cohort.cohort1.enabled" -> false,
      "abTesting.cohort.cohort2.enabled" -> true))) with ConfiguredCohortValues[Cohort] {
      def availableValues: List[Cohort] = List(cohort1, cohort2)

      verifyConfiguration()

      cohorts.values should contain only cohort2
    }

    "load multiple cohorts" in new WithApplication(FakeApplication(withGlobal = Some(new GlobalSettings {}), additionalConfiguration = Map(
      "abTesting.cohort.cohort1.enabled" -> true,
      "abTesting.cohort.cohort2.enabled" -> true))) with ConfiguredCohortValues[Cohort] {
      def availableValues: List[Cohort] = List(cohort1, cohort2)

      verifyConfiguration()

      cohorts.values should contain allOf(cohort1, cohort2)
    }
  }
}

