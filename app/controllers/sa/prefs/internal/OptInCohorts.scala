package controllers.sa.prefs.internal

import play.api.mvc.PathBindable
import uk.gov.hmrc.abtest.Cohort

sealed trait OptInCohort extends Cohort {
  val id: Int
  val name: String

  override def toString: String = name
}

object OptInCohort {

  def fromId(id: Int): Option[OptInCohort] = OptInCohortConfigurationValues.cohorts.values.find(c => c.id == id)

  implicit val pathBinder: PathBindable[Option[OptInCohort]] = PathBindable.bindableInt.transform(fromId, _.map(_.id).getOrElse(throw new IllegalArgumentException("Cannot generate a URL for an unknown Cohort")))
}

object FPage extends OptInCohort {
  override val id: Int = 5
  override val name: String = "FPage"
}

object HPage extends OptInCohort {
  override val id: Int = 7
  override val name: String = "HPage"
}