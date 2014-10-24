package controllers.sa.prefs.internal

import play.api.Play
import uk.gov.hmrc.common.microservice.domain.User

trait Cohort {
  val id: Int
  val name: String

  override def toString: String = name
}

trait CohortCalculator[T <: Cohort] {

  val values: List[T]

  def calculateCohort(user: User): T = user.userAuthority.accounts.sa.map(sa => calculate(sa.utr.hashCode)).getOrElse(enabledCohorts.head)

  def calculate(hashCode: Int): T = {
    verifyConfiguration()
    enabledCohorts(Math.abs(hashCode) % enabledCohorts.size)
  }

  def verifyConfiguration() = if(enabledCohorts.isEmpty) throw new RuntimeException("No enabled cohorts found")

  private lazy val enabledCohorts: List[T] = values.filter(isEnabled)

  private def isEnabled(cohort: T) = Play.current.configuration.getBoolean(s"abTesting.cohort.${cohort.toString}.enabled").getOrElse(false)
}

trait CohortValues[T <: Cohort] {
  val values: List[T]

  def fromId(id: Int): T = values.find(c => c.id == id).getOrElse(throw new IllegalArgumentException("Cohort with id not found"))
}