package controllers.sa.prefs.internal

import controllers.sa.prefs.AuthorityUtils._
import controllers.sa.prefs.config.PreferencesGlobal
import org.scalactic.Tolerance
import org.scalatest.{Inspectors, LoneElement}
import play.api.Play
import play.api.test.{FakeApplication, WithApplication}
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.test.UnitSpec

import scala.util.Random

class EmailOptInCohortCalculatorSpec extends UnitSpec with Inspectors with Tolerance with LoneElement {

  "Cohort value" should {

    "always be the same for a given user" in new WithApplication(FakeApplication()) with CohortCalculator[OptInCohort] {
      override val values: List[OptInCohort] = OptInCohort.values
      val user = userWithSaUtr("1234567890")
      val cohorts = (1 to 10) map { _ => calculateCohort(user)}
      cohorts.toSet.loneElement should be(a[Cohort])
    }

    "return a default a cohort value for a user with no SA-UTR" in new WithApplication(FakeApplication()) with CohortCalculator[OptInCohort] {
      override val values: List[OptInCohort] = OptInCohort.values
      val user = userWithNoUtr
      val cohorts = (1 to 10) map { _ => calculateCohort(user)}
      cohorts.toSet.loneElement should be(OptInNotSelected)
    }

    "be evenly spread for given set of users" in new WithApplication(FakeApplication()) with CohortCalculator[OptInCohort] {
      override val values: List[OptInCohort] = OptInCohort.values

      def generateRandomUtr(): String = (for {_ <- 1 to 10} yield Random.nextInt(8) + 1).mkString("")

      val sampleSize = 10000
      val utrs = ((1 to sampleSize) map (_ => generateRandomUtr())).distinct

      val cohorts = utrs.map(userWithSaUtr).map(calculateCohort)

      val cohortCounts = cohorts.groupBy(c => c).mapValues(_.size)

      forEvery(values.toSet) { possibleCohort =>
        cohortCounts(possibleCohort) should be(sampleSize / values.size +- (sampleSize / 10))
      }
    }

    "not return a disabled cohort" in new WithApplication(FakeApplication(additionalConfiguration = disabledCohorts)) with CohortCalculator[OptInCohort] {
      override val values: List[OptInCohort] = OptInCohort.values

      def generateRandomUtr(): String = (for {_ <- 1 to 10} yield Random.nextInt(8) + 1).mkString("")

      val sampleSize = 100
      val utrs = ((1 to sampleSize) map (_ => generateRandomUtr())).distinct

      val cohorts = utrs.map(userWithSaUtr).map(calculateCohort)

      forEvery(cohorts) { cohort => cohort shouldBe OptInNotSelected}
    }
  }

  "The preference-frontend microservice" should {
    "not start the app if all cohorts are disabled" in {

      object OptInCohortCalculatorVerifier extends CohortCalculator[OptInCohort] {
        override val values: List[OptInCohort] = OptInCohort.values
      }

      object PreferencesGlobalForTest extends PreferencesGlobal {
        override val cohortCalculator = OptInCohortCalculatorVerifier
      }

      intercept[RuntimeException] {
        Play.start(FakeApplication(withGlobal = Some(PreferencesGlobalForTest),
          additionalConfiguration = disabledCohorts ++ Map("abTesting.cohort.OptInNotSelected.enabled" -> false)
        ))
      }
    }
  }

  def disabledCohorts: Map[String, Boolean] = {
    Map("abTesting.cohort.OptInSelected.enabled" -> false,
      "abTesting.cohort.CPage.enabled" -> false,
      "abTesting.cohort.DPage.enabled" -> false,
      "abTesting.cohort.EPage.enabled" -> false
    )
  }

  "CohortValues" should {
    "find the correct cohort by id" in new WithApplication(FakeApplication()) {
      OptInCohort.fromId(0) shouldBe OptInNotSelected
      OptInCohort.fromId(1) shouldBe OptInSelected
    }
    "throw an IllegalArgoumentException if cohort not found" in new WithApplication(FakeApplication()) {
      intercept[IllegalArgumentException] {
        OptInCohort.fromId(100)
      }
    }
  }

  val userWithNoUtr = User(
    userId = "userId",
    userAuthority = emptyAuthority(id = "userId"),
    nameFromGovernmentGateway = Some("Ciccio"),
    decryptedToken = None
  )

  def userWithSaUtr(utr: String) = User(
    userId = "userId",
    userAuthority = saAuthority(id = "userId", utr = utr.toString),
    nameFromGovernmentGateway = Some("Ciccio"),
    decryptedToken = None
  )

}
