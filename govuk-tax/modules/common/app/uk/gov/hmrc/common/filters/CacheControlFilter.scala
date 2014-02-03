package uk.gov.hmrc.common.filters

import play.api.mvc.{SimpleResult, RequestHeader, Filter}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.mvc.Http.{Status, HeaderNames}

case class CacheControlFilter(cachableContentTypes: String*) extends Filter {
  def apply(next: (RequestHeader) => Future[SimpleResult])(rh: RequestHeader): Future[SimpleResult] = {
    next(rh).map(r =>
      (r.header.status, r.header.headers.get(HeaderNames.CONTENT_TYPE)) match {
        case (Status.NOT_MODIFIED, _) => r
        case (_, Some(contentType)) if cachableContentTypes.exists(contentType.startsWith) => r
        case _ => r.withHeaders(HeaderNames.CACHE_CONTROL -> "no-cache,no-store,max-age=0")
      }
    )
  }
}