package uk.gov.hmrc.common.filters

import play.api.mvc._
import scala.concurrent.Future
import play.api.mvc.SimpleResult
import org.joda.time.DateTime
import controllers.common.SessionTimeoutWrapper
import uk.gov.hmrc.utils.DateTimeUtils
import SessionTimeoutWrapper._

object CSRFExceptionsFilter extends Filter {

  val whitelist = List("/ida/login", "/ssoin")

  def apply(f: (RequestHeader) => Future[SimpleResult])(rh: RequestHeader): Future[SimpleResult] = {
     f(filteredHeaders(rh))
  }

  private[filters] def filteredHeaders(rh: RequestHeader, now: () => DateTime = () => DateTimeUtils.now) =
    if (rh.method == "POST" && (!hasValidTimestamp(rh.session, now) || whitelist.contains(rh.path)))
      rh.copy(headers = new CustomHeaders(rh))
    else rh

  private class CustomHeaders(rh: RequestHeader) extends Headers {
    protected val data: Seq[(String, Seq[String])] = {
      rh.headers.toMap.toList :+ ("Csrf-Token" -> Seq("nocheck"))
    }
  }
}
