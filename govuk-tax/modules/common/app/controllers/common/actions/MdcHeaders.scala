package controllers.common.actions

import controllers.common.{SessionKeys, HeaderNames}

import concurrent.Future
import play.api.mvc._
import org.slf4j.MDC
import play.api.{Mode, Play}
import views.html.server_error
import play.Logger
import uk.gov.hmrc.common.microservice.HasResponse
import com.google.common.net.HttpHeaders

private[actions] trait MdcHeaders extends Results {
  protected def storeHeaders(action: Action[AnyContent]): Action[AnyContent] = Action.async {
    request =>
      request.session.get(SessionKeys.userId).foreach(MDC.put(MdcKeys.authorisation, _))
      request.session.get(SessionKeys.token).foreach(MDC.put(MdcKeys.token, _))
      request.session.get(SessionKeys.sessionId).foreach(MDC.put(MdcKeys.xSessionId, _))
      request.headers.get(HeaderNames.forwardedFor).foreach(MDC.put(MdcKeys.forwardedFor, _))
      request.headers.get(HeaderNames.xRequestId).foreach(MDC.put(MdcKeys.xRequestId, _))
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
      case Mode.Dev | Mode.Test => InternalServerError(server_error(t, request, MDC.get(MdcKeys.xRequestId)))
      case Mode.Prod => InternalServerError(server_error(t, request, MDC.get(MdcKeys.xRequestId)))
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
object MdcKeys {
  val authorisation = HttpHeaders.AUTHORIZATION
  val token = "token"
  val xSessionId = "X-Session-ID"
  val forwardedFor = "x-forwarded-for"
  val xRequestId = "X-Request-ID"
}
