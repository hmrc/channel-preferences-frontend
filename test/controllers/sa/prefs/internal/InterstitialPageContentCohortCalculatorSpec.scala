package controllers.sa.prefs.internal

import controllers.sa.prefs.AuthorityUtils._
import controllers.sa.prefs.internal.EmailOptInCohorts.Cohort
import org.scalactic.Tolerance
import org.scalatest.{Inspectors, LoneElement}
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.test.UnitSpec

import scala.util.Random

class InterstitialPageContentCohortCalculatorSpec extends UnitSpec with Inspectors with Tolerance with LoneElement {

  def calculateCohort(user: User) = EmailOptInCohortCalculator.calculateCohort(user)

  "Cohort value" should {

    "always be the same for a given user" in {
      val user = userWithSaUtr("1234567890")
      val cohorts = (1 to 10) map { _ => calculateCohort(user)}
      cohorts.toSet.loneElement should be (a [Cohort])
    }

    "return a default a cohort value for a user with no SA-UTR" in {
      val user = userWithNoUtr
      val cohorts = (1 to 10) map { _ => calculateCohort(user) }
      cohorts.toSet.loneElement should be (EmailOptInCohorts.SignUpForSelfAssessment)
    }

    "be evenly spread for given set of users" in {
      def generateRandomUtr(): String = (for {_ <- 1 to 10} yield Random.nextInt(8) + 1).mkString("")
      val sampleSize = 10000
      val utrs = ((1 to sampleSize) map (_ => generateRandomUtr())).distinct

      val cohorts = utrs.map(userWithSaUtr).map(calculateCohort)

      val cohortCounts = cohorts.groupBy(c => c).mapValues(_.size)

      forEvery(EmailOptInCohorts.values.toSet) { possibleCohort =>
        cohortCounts(possibleCohort) should be(sampleSize / EmailOptInCohorts.values.size +- (sampleSize / 10))
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
