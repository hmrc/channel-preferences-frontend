package uk.gov.hmrc.common.filters

import play.api.mvc._
import scala.concurrent.Future
import controllers.common.service.{Decrypter, Encrypter}
import play.api.mvc.SimpleResult
import play.api.http.HeaderNames.{COOKIE => CookieHeaderName}

abstract class SessionCryptoFilter extends Filter with Encrypter with Decrypter {

  override def apply(next: (RequestHeader) => Future[SimpleResult])(rh: RequestHeader): Future[SimpleResult] = {

    val resultWithPlainSession = next(decryptSession(rh))

//    encryptSession(resultWithPlainSession)
    resultWithPlainSession
  }

  private def decryptSession(rh: RequestHeader): RequestHeader = {

    val cookies = rh.headers.getAll(CookieHeaderName).flatMap(Cookies.decode)

    if (cookies.isEmpty) rh else rh.copy(headers = new Headers {
      override protected val data: Seq[(String, Seq[String])] = {


        val updatedCookies: Seq[Cookie] = cookies.map {
          case session @ Cookie(Session.COOKIE_NAME, encryptedSession, _, _, _, _, _) => session.copy(value = decrypt(encryptedSession))
          case other => other
        }

        if (updatedCookies.isEmpty) rh.headers.toMap.toSeq
        else (rh.headers.toMap + (CookieHeaderName -> Seq(Cookies.encode(updatedCookies)))).toSeq
      }
    })
  }


  private def encryptSession(resultWithPlainSession: Future[SimpleResult]): Future[SimpleResult] = ???

//new RequestHeader {
//      def uri: String = rh.uri
//
//      def remoteAddress: String = rh.remoteAddress
//
//      def queryString: Map[String, Seq[String]] = rh.queryString
//
//      def method: String = rh.method
//
//      def headers: Headers = rh.headers
//
//      def path: String = rh.path
//
//      def version: String = rh.version
//
//      def tags: Map[String, String] = rh.tags
//
//      def id: Long = rh.id
//    })
//  }
}
