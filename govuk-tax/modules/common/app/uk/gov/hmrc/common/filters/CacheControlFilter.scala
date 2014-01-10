package uk.gov.hmrc.common.filters

import play.api.mvc.{SimpleResult, RequestHeader, Filter}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.mvc.Http.HeaderNames

object CacheControlFilter extends Filter {
  def apply(next: (RequestHeader) => Future[SimpleResult])(rh: RequestHeader): Future[SimpleResult] = {
    next(rh).map(_.withHeaders(HeaderNames.CACHE_CONTROL -> "no-cache,no-store,max-age=0"))
  }
}
