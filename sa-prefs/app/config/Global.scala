package config

import play.api.mvc._
import scala.concurrent.Future
import play.api.mvc.Results._
import play.api.i18n.Messages
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.GlobalSettings

object Global extends GlobalSettings {

  override def doFilter(action: EssentialAction) = EssentialAction { request =>
    action(request).map(_.withHeaders(
      "Cache-Control" -> "no-cache,no-store,max-age=0"
    ))
  }

  override def onError(request: RequestHeader, ex: Throwable) = {
    Future.successful(InternalServerError(
      views.html.global_error(Messages("global.error.heading"), "An error has occurred template text") //ex
    ))
  }

  override def onHandlerNotFound(request: RequestHeader) = {
    Future.successful(NotFound(
      views.html.global_error(Messages("global.error.heading"), "The requested resource doesn't seem to exist: " + request.path)
    ))
  }

  override def onBadRequest(request: RequestHeader, error: String) = {
    Future.successful(BadRequest("Bad Request: " + error))
  }
}
