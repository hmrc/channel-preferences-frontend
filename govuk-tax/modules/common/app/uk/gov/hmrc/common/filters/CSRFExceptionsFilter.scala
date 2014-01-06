package uk.gov.hmrc.common.filters

import play.api.mvc.{Headers, SimpleResult, RequestHeader, Filter}
import scala.concurrent.Future

object CSRFExceptionsFilter extends Filter {
  def apply(f: (RequestHeader) => Future[SimpleResult])(rh: RequestHeader): Future[SimpleResult] = {
    f(filteredHeaders(rh))
  }

  private[filters] def filteredHeaders(rh: RequestHeader) =
    if (rh.path == "/ida/login" && rh.method == "POST") rh.copy(headers = new CustomHeaders(rh))
    else rh

  private class CustomHeaders(rh: RequestHeader) extends Headers {
    protected val data: Seq[(String, Seq[String])] = {
      rh.headers.toMap.toList :+ ("Csrf-Token" -> Seq("nocheck"))
    }
  }

}
