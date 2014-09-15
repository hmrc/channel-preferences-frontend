package controllers.sa.prefs.internal

import play.api.mvc.PathBindable
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.domain.SaUtr

trait CohortCalculator[CohortEnum <: Enumeration] {
  def CohortEnum: CohortEnum

  def calculateCohort(utr: SaUtr): CohortEnum#Value = CohortEnum(Math.abs(utr.value.hashCode) % CohortEnum.values.size)

  def calculateCohort(user: User): CohortEnum#Value = user.userAuthority.accounts.sa.map(sa => calculateCohort(sa.utr))
    .getOrElse(CohortEnum.values.firstKey)
}

trait EmailOptInCohortCalculator extends CohortCalculator[EmailOptInCohorts.type] {
  val CohortEnum = EmailOptInCohorts
}

object EmailOptInCohortCalculator extends EmailOptInCohortCalculator

object EmailOptInCohorts extends Enumeration {
  type Cohort = Value
  val OptInNotSelected = Value(0)
  val OptInSelected = Value(1)

  implicit val pathBinder: PathBindable[Cohort] = PathBindable.bindableInt.transform(apply, _.id)


}
