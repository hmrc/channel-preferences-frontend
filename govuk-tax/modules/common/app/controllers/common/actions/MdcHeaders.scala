package controllers.common.actions

import controllers.common.{HeaderNames, CookieEncryption}
import concurrent.Future
import play.api.mvc._
import org.slf4j.MDC
import play.api.{Mode, Play}
import views.html.server_error
import play.Logger
import uk.gov.hmrc.common.microservice.HasResponse

private[actions] trait MdcHeaders extends Results with CookieEncryption with HeaderNames {
  protected def storeHeaders(action: Action[AnyContent]): Action[AnyContent] = Action.async {
    request =>
      request.session.get("userId").foreach(userId => MDC.put(authorisation, decrypt(userId)))
      request.session.get("token").foreach(token => MDC.put("token", token))
      request.session.get("sessionId").foreach(sessionId => MDC.put(xSessionId, decrypt(sessionId)))
      request.headers.get(forwardedFor).foreach(ip => MDC.put(forwardedFor, ip))
      request.headers.get(xRequestId).foreach(rid => MDC.put(xRequestId, rid))
      Logger.debug("Request details added to MDC")
      try {
        action(request)
      } catch {
        case t: Throwable => internalServerError(request, t)
      } finally {
        MDC.clear()
      }
  }

  private def internalServerError(request: Request[AnyContent], t: Throwable): Future[SimpleResult] = {
    logThrowable(t)

    import play.api.Play.current

    Future.successful(Play.application.mode match {
      case Mode.Dev | Mode.Test => InternalServerError(server_error(t, request, MDC.get(xRequestId)))
      case Mode.Prod => InternalServerError(server_error(t, request, MDC.get(xRequestId)))
    })
  }

  private def logThrowable(t: Throwable) {
    Logger.error("Action failed", t)
    t match {
      case resp: HasResponse => Logger.error(s"MicroService Response '${resp.response.body}'")
      case _ =>
    }
  }
}
