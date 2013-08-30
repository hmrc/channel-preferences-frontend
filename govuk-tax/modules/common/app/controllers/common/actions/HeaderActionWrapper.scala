package controllers.common.actions

import controllers.common.service.MicroServices
import play.api.mvc._
import controllers.common.{ HeaderNames, CookieEncryption }
import org.slf4j.MDC
import java.util.UUID
import play.api.{ Mode, Play }
import views.html.server_error
import play.Logger
import uk.gov.hmrc.microservice.HasResponse

trait HeaderActionWrapper {
  self: Controller with MicroServices with CookieEncryption with HeaderNames =>

  object WithHeaders {

    def apply(action: Action[AnyContent]): Action[AnyContent] = Action {
      request =>
        request.session.get("userId").foreach(userId => MDC.put(authorisation, decrypt(userId)))
        request.session.get("token").foreach(token => MDC.put("token", token))
        request.headers.get(forwardedFor).foreach(ip => MDC.put(forwardedFor, ip))
        MDC.put(requestId, s"frontend-${UUID.randomUUID().toString}")
        try {
          action(request)
        } catch {
          case t: Throwable => internalServerError(request, t)
        } finally {
          MDC.clear
        }
    }
  }

  private def internalServerError(request: Request[AnyContent], t: Throwable): Result = {
    logThrowable(t)
    import play.api.Play.current
    Play.application.mode match {
      // different pages for prod and dev/test
      case Mode.Dev | Mode.Test => InternalServerError(server_error(t, request, MDC.get(requestId)))
      case Mode.Prod => InternalServerError(server_error(t, request, MDC.get(requestId)))
    }
  }

  private def logThrowable(t: Throwable) {
    Logger.error("Action failed", t)
    if (t.isInstanceOf[HasResponse]) {
      Logger.error(s"MicroService Response '${t.asInstanceOf[HasResponse].response.body}'")
    }
  }
}
