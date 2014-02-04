package uk.gov.hmrc.common.filters

import play.api.mvc.{SimpleResult, RequestHeader, Filter}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.mvc.Http.{Status, HeaderNames}
import play.api.{Play, Logger}
import scala.collection.JavaConverters._

abstract class CacheControlFilter extends Filter {
  val cachableContentTypes: Seq[String]

  final def apply(next: (RequestHeader) => Future[SimpleResult])(rh: RequestHeader): Future[SimpleResult] = {
    next(rh).map(r =>
      (r.header.status, r.header.headers.get(HeaderNames.CONTENT_TYPE)) match {
        case (Status.NOT_MODIFIED, _) => r
        case (_, Some(contentType)) if cachableContentTypes.exists(contentType.startsWith) => r
        case _ => r.withHeaders(HeaderNames.CACHE_CONTROL -> "no-cache,no-store,max-age=0")
      }
    )

  }
}
object CacheControlFilter {
  def fromConfig(configKey: String) = {
    new CacheControlFilter {
      override lazy val cachableContentTypes = {
        val c = Play.current.configuration.getStringList(configKey).toList.map(_.asScala).flatten
        Logger.info(s"Will allow caching of content types matching: ${c.mkString(", ")}")
        c
      }
    }
  }
}