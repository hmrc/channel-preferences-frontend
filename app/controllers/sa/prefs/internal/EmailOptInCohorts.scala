package controllers.sa.prefs.internal

import play.api.mvc.PathBindable

sealed trait OptInCohort extends Cohort

object OptInCohort extends CohortValues[OptInCohort] {
  override val values = List(OptInNotSelected, OptInSelected)
  implicit val pathBinder: PathBindable[OptInCohort] = PathBindable.bindableInt.transform(fromId, _.id)
}

object OptInNotSelected extends OptInCohort {
  override val id: Int = 0
  override val name: String = "OptInNotSelected"
}

object OptInSelected extends OptInCohort {
  override val id: Int = 1
  override val name: String = "OptInSelected"
}