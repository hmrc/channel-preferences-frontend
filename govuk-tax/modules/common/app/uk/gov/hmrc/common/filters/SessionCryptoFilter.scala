package uk.gov.hmrc.common.filters

import play.api.mvc._
import scala.concurrent.Future
import controllers.common.service.{Decrypter, Encrypter}
import play.api.mvc.SimpleResult

abstract class SessionCryptoFilter extends Filter with Encrypter with Decrypter {

  override def apply(next: (RequestHeader) => Future[SimpleResult])(rh: RequestHeader): Future[SimpleResult] = {
    next(new RequestHeader {
      def uri: String = rh.uri

      def remoteAddress: String = rh.remoteAddress

      def queryString: Map[String, Seq[String]] = rh.queryString

      def method: String = rh.method

      def headers: Headers = rh.headers

      def path: String = rh.path

      def version: String = rh.version

      def tags: Map[String, String] = rh.tags

      def id: Long = rh.id
    })
  }
}
