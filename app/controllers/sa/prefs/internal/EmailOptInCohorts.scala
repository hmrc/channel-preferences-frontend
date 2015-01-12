package controllers.sa.prefs.internal

import play.api.mvc.PathBindable

sealed trait OptInCohort extends Cohort

object OptInCohort extends CohortValues[OptInCohort] {
  override val values = List(FPage, HPage)
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