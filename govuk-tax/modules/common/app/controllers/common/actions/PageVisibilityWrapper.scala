package controllers.common.actions

import play.api.mvc._
import play.api.mvc.Results._
import uk.gov.hmrc.common.microservice.domain.User
import play.api.mvc.SimpleResult

trait PageVisibilityPredicate {
  def isVisible(user: User, request: Request[AnyContent]): Boolean

  def nonVisibleResult: SimpleResult = {
    NotFound
  }
}

object WithPageVisibility {
  def apply(predicate: PageVisibilityPredicate, user: User)(action: User => Action[AnyContent]): Action[AnyContent] =

    Action.async {
      request =>
        if (predicate.isVisible(user, request))
          action(user)(request)
        else
          Action(predicate.nonVisibleResult)(request)
    }
}

