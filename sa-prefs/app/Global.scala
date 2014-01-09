import play.api._
import play.mvc.Http.RequestHeader

object Global extends GlobalSettings {

  override def onError(request: RequestHeader, ex: Throwable) = {
    InternalServerError(
      views.html.errorPage(ex)
    )
  }


  def NoCache[A](action: Action[A]): Action[A] = Action(action.parser) { request =>
    action(request) match {
      case s: SimpleResult[_] => s.withHeaders(PRAGMA -> "no-cache")
      case result => result
    }
  }

  override def onRouteRequest(request: RequestHeader): Option[Handler] = {
    if (Play.isDev) {
      super.onRouteRequest(request).map {
        case action: Action[_] => NoCache(action)
        case other => other
      }
    } else {
      super.onRouteRequest(request)
    }
  }


}
