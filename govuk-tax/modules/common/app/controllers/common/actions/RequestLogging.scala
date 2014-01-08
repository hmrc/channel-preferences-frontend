package controllers.common.actions

import uk.gov.hmrc.utils.DateConverter
import config.DateTimeProvider
import util.Success
import controllers.common.HeaderNames
import play.api.mvc._
import java.text.SimpleDateFormat
import java.util.{UUID, Date}
import play.api.Logger
import scala.concurrent.ExecutionContext

private[actions] trait RequestLogging extends HeaderNames with DateConverter {

  import ExecutionContext.Implicits.global

  private val format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ZZZZ")

  def requestWithID(request: Request[AnyContent]): Request[AnyContent] = {
    request.session.get(HeaderNames.xRequestId) match {
      case Some(s) => request
      case _ => {
        val rid = s"govuk-tax-${UUID.randomUUID().toString}"
        val requestIdHeader = HeaderNames.xRequestId -> Seq(rid)
        val requestTimestampHeader = HeaderNames.xRequestTimestamp -> Seq(System.nanoTime().toString)

        // TODO: Find a more efficient way of doing this, if possible.
        val newHeaders = new Headers {
          val data: Seq[(String, Seq[String])] = (request.headers.toMap + requestIdHeader + requestTimestampHeader).toSeq
        }

        new WrappedRequest(request) {
          override val headers = newHeaders
        }
      }
    }
  }

  protected def logRequest(action: Action[AnyContent]) = Action.async {
    request => {
      val start = DateTimeProvider.now().getMillis
      val startTime = format.format(new Date(start))

      val reqWithId = requestWithID(request)

      val hc = HeaderCarrier(reqWithId)

      action(reqWithId).andThen {
        case Success(result) =>
          val elapsedTime = DateTimeProvider.now().getMillis - start
          // Apache combined log format http://httpd.apache.org/docs/2.4/logs.html
          Logger.info(s"${hc.forwarded.getOrElse(request.remoteAddress)} ${hc.requestId} " +
            s"${hc.userId.getOrElse("-")} [$startTime] '${request.method} ${request.uri}' ${result.header.status} " +
            s"- ${elapsedTime}ms '${request.headers.get("Referer").getOrElse("-")}' '${request.headers.get("User-Agent").getOrElse("-")}'")
      }
    }
  }
}
