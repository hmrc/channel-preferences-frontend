package controllers.sa.prefs.internal

import play.api.mvc.PathBindable
import uk.gov.hmrc.common.microservice.domain.User

trait InterstitialPageContentCohortCalculator {
  type Cohort = InterstitialPageContentCohorts.Cohort

  def calculateCohortFor(user: User) = user.userAuthority.accounts.sa.map { sa =>
    InterstitialPageContentCohorts(Math.abs(sa.utr.value.hashCode) % 2)
  }
}

object InterstitialPageContentCohorts extends Enumeration {
  type Cohort = Value
  val GetSelfAssesment = Value(0)
  val SignUpForSelfAssesment = Value(1)

  implicit val pathBinder: PathBindable[Cohort] = PathBindable.bindableInt.transform(apply, _.id)
}
