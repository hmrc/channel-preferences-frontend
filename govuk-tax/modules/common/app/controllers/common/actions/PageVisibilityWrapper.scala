package controllers.common.actions

import play.api.mvc._
import play.api.mvc.Results._
import uk.gov.hmrc.common.microservice.domain.User
import play.api.mvc.SimpleResult
import scala.concurrent._
import uk.gov.hmrc.common.StickyMdcExecutionContext.global

trait PageVisibilityPredicate {
  def isVisible(user: User, request: Request[AnyContent]): Future[Boolean]

  def nonVisibleResult: SimpleResult = NotFound
}

private[actions] object WithPageVisibility {
  def apply(predicate: PageVisibilityPredicate, user: User)(action: User => Action[AnyContent]): Action[AnyContent] =

    Action.async {
      request =>
        predicate.isVisible(user, request).flatMap { visible =>
          if (visible)
            action(user)(request)
          else
            Action(predicate.nonVisibleResult)(request)
        }
    }
}

object DefaultPageVisibilityPredicate extends PageVisibilityPredicate {
  def isVisible(user: User, request: Request[AnyContent]) = Future.successful(true)
}
