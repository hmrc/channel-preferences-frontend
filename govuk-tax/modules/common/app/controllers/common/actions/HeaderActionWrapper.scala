package controllers.common.actions

import controllers.common.CookieEncryption
import play.api.mvc.Results
import concurrent.Future

trait HeaderActionWrapper {
  object WithHeaders extends WithHeaders
}

class WithHeaders extends Results with CookieEncryption {

  import play.api.mvc._
  import org.slf4j.MDC
  import java.util.UUID
  import play.api.{ Mode, Play }
  import views.html.server_error
  import play.Logger
  import uk.gov.hmrc.microservice.HasResponse
  import controllers.common.HeaderNames._

  def apply(action: Action[AnyContent]): Action[AnyContent] = Action.async {
    request =>
      request.session.get("userId").foreach(userId => MDC.put(authorisation, decrypt(userId)))
      request.session.get("token").foreach(token => MDC.put("token", token))
      request.headers.get(forwardedFor).foreach(ip => MDC.put(forwardedFor, ip))
      MDC.put(requestId, s"govuk-tax-${UUID.randomUUID().toString}")
      request.session.get("sessionId").foreach(sessionId => MDC.put(xSessionId, decrypt(sessionId)))
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
      // different pages for prod and dev/test
      case Mode.Dev | Mode.Test => InternalServerError(server_error(t, request, MDC.get(requestId)))
      case Mode.Prod => InternalServerError(server_error(t, request, MDC.get(requestId)))
    })
  }

  private def logThrowable(t: Throwable) {
    Logger.error("Action failed", t)
    t match {
      case resp : HasResponse => Logger.error(s"MicroService Response '${resp.response.body}'")
      case _ =>
    }
  }
}