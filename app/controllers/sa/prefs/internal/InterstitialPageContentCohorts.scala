package controllers.sa.prefs.internal

import play.api.mvc.PathBindable
import uk.gov.hmrc.common.microservice.domain.User

trait CohortCalculator[CohortEnum <: Enumeration] {
  def CohortEnum: CohortEnum

  def calculateCohort(user: User): CohortEnum#Value = user.userAuthority.accounts.sa.map { sa =>
    CohortEnum(Math.abs(sa.utr.value.hashCode) % CohortEnum.values.size)
  }.getOrElse(CohortEnum.values.firstKey)
}

trait InterstitialPageContentCohortCalculator extends CohortCalculator[InterstitialPageContentCohorts.type] {
  val CohortEnum = InterstitialPageContentCohorts
}

object InterstitialPageContentCohortCalculator extends InterstitialPageContentCohortCalculator

object InterstitialPageContentCohorts extends Enumeration {
  type Cohort = Value
  val SignUpForSelfAssessment = Value(0)
  val GetSelfAssessment = Value(1)

  implicit val pathBinder: PathBindable[Cohort] = PathBindable.bindableInt.transform(apply, _.id)
}
