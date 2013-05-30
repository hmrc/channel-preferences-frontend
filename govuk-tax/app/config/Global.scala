package config

import play.api.Logger
import play.api.mvc._
import scala.concurrent.ExecutionContext.Implicits._
import java.text.SimpleDateFormat
import java.util.Date

object AccessLoggingFilter extends Filter {
  def apply(next: (RequestHeader) => Result)(rh: RequestHeader) = {
    val start = System.currentTimeMillis
    val startTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ZZZZ").format(new Date(start))

    def logTime(result: PlainResult): Result = {
      val time = System.currentTimeMillis - start
      // Apache combined log format http://httpd.apache.org/docs/2.4/logs.html
      Logger("accesslog").info(s"${rh.remoteAddress} - ${rh.session.get("username").getOrElse("-")} " +
        s"[$startTime] '${rh.method} ${rh.uri}' ${result.header.status} - ${time}ms " +
        s"'${rh.headers.get("Referer").getOrElse("-")}' '${rh.headers.get("User-Agent").getOrElse("-")}'")
      result
    }

    next(rh) match {
      case plain: PlainResult => logTime(plain)
      case async: AsyncResult => async.transform(logTime)
    }
  }
}

object Global extends WithFilters(AccessLoggingFilter) {
}
