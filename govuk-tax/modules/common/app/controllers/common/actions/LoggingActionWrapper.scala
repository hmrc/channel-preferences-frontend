package controllers.common.actions

import play.api.mvc._
import java.text.SimpleDateFormat
import java.util.Date
import play.api.Logger
import controllers.common.HeaderNames
import scala.concurrent.ExecutionContext

trait LoggingActionWrapper extends MdcHelper {
  self: Controller with HeaderNames =>

  import ExecutionContext.Implicits.global

  object WithRequestLogging {

    def apply(action: Action[AnyContent]): Action[AnyContent] = Action {
      request =>
        {
          val start = System.currentTimeMillis
          val startTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ZZZZ").format(new Date(start))
          val mdc = fromMDC

          def log(result: PlainResult): Result = {
            val elapsedTime = System.currentTimeMillis - start

            // Apache combined log format http://httpd.apache.org/docs/2.4/logs.html
            Logger.info(s"${mdc.get(forwardedFor).getOrElse(request.remoteAddress)} ${mdc.get(requestId).getOrElse("-")} " +
              s"${mdc.get(authorisation).getOrElse("-")} [$startTime] '${request.method} ${request.uri}' ${result.header.status} " +
              s"- ${elapsedTime}ms '${request.headers.get("Referer").getOrElse("-")}' '${request.headers.get("User-Agent").getOrElse("-")}'")

            result
          }

          action(request) match {
            case plain: PlainResult => log(plain)
            case async: AsyncResult => async.transform(log)
          }
        }
    }
  }
}
