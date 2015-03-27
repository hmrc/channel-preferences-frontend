package controllers.sa.prefs.internal

import controllers.sa.prefs.AuthorityUtils._
import org.scalactic.Tolerance
import org.scalatest.{Inspectors, LoneElement}
import play.api.test.{FakeApplication, WithApplication}
import uk.gov.hmrc.abtest.Cohorts
import uk.gov.hmrc.play.frontend.auth.User
import uk.gov.hmrc.play.test.UnitSpec

import scala.util.Random

class EmailOptInCohortCalculatorSpec extends UnitSpec with Inspectors with Tolerance with LoneElement {

  "Cohort value" should {

    "always be the same for a given user" in new WithApplication(FakeApplication()) with OptInCohortCalculator {
      override val cohorts = Cohorts(FPage, HPage)
      val user = userWithSaUtr("1234567890")
      val calculatedCohorts = (1 to 10) map { _ => calculateCohort(user)}
      calculatedCohorts.toSet.loneElement should be(a[OptInCohort])
    }

    "return a default cohort value for a user with no SA-UTR" in new WithApplication(FakeApplication()) with OptInCohortCalculator {
      override val cohorts = Cohorts(FPage, HPage)

      val user = userWithNoUtr
      val calculatedCohorts = (1 to 10) map { _ => calculateCohort(user)}
      calculatedCohorts.toSet.loneElement should be(FPage)
    }

    "be evenly spread for given set of users" in new WithApplication(FakeApplication()) with OptInCohortCalculator {
      override val cohorts = Cohorts(FPage, HPage)

      def generateRandomUtr(): String = (for {_ <- 1 to 10} yield Random.nextInt(8) + 1).mkString("")

      val sampleSize = 10000
      val utrs = ((1 to sampleSize) map (_ => generateRandomUtr())).distinct

      val calculatedCohorts = utrs.map(userWithSaUtr).map(calculateCohort)

      val cohortCounts = calculatedCohorts.groupBy(c => c).mapValues(_.size)

      forEvery(cohorts.values) { possibleCohort =>
        cohortCounts(possibleCohort) should be(sampleSize / cohorts.values.size +- (sampleSize / 10))
      }
    }
  }

  def disabledCohorts: Map[String, Boolean] = Map(
    "abTesting.cohort.FPage.enabled" -> true,
    "abTesting.cohort.HPage.enabled" -> false
  )

  "CohortValues" should {
    "find the correct cohort by id" in new WithApplication(FakeApplication()) {
      OptInCohort.fromId(5) should contain (FPage)
      OptInCohort.fromId(7) should contain (HPage)
    }

    "return none if cohort not found" in new WithApplication(FakeApplication()) {
      OptInCohort.fromId(100) should be (None)
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
