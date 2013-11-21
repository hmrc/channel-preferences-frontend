package controllers.common.actions

import uk.gov.hmrc.utils.DateConverter
import config.DateTimeProvider
import util.Success
import controllers.common.HeaderNames
import play.api.mvc._
import java.text.SimpleDateFormat
import java.util.Date
import play.api.Logger
import scala.concurrent.ExecutionContext

private[actions] trait RequestLogging extends HeaderNames with DateConverter {

  import ExecutionContext.Implicits.global

  private val format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ZZZZ")

  protected def logRequest(action: Action[AnyContent]) = Action.async {
    request => {
      val start = DateTimeProvider.now().getMillis
      val startTime = format.format(new Date(start))
      val hc = HeaderCarrier(request)

      action(request).andThen {
        case Success(result) =>
          val elapsedTime = DateTimeProvider.now().getMillis - start

          // Apache combined log format http://httpd.apache.org/docs/2.4/logs.html
          Logger.info(s"${hc.forwarded.getOrElse(request.remoteAddress)} ${hc.requestIdString} " +
            s"${hc.userId.getOrElse("-")} [$startTime] '${request.method} ${request.uri}' ${result.header.status} " +
            s"- ${elapsedTime}ms '${request.headers.get("Referer").getOrElse("-")}' '${request.headers.get("User-Agent").getOrElse("-")}'")
      }
    }
  }
}
