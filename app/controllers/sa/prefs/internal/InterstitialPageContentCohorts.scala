package controllers.sa.prefs.internal

import play.api.mvc.PathBindable
import uk.gov.hmrc.common.microservice.domain.User

object InterstitialPageContentCohorts extends Enumeration {
  val GetSelfAssesment = Value(1)
  val SignUpForSelfAssesment = Value(2)

  def calculateFor(user: User): Value = GetSelfAssesment

  implicit val pathBinder: PathBindable[Value] = PathBindable.bindableInt.transform(apply, _.id)
}
