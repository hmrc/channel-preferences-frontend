package uk.gov.hmrc.common.filters

import play.api.mvc._
import scala.concurrent.Future
import controllers.common.service.{Decrypter, Encrypter}
import play.api.mvc.SimpleResult
import play.api.http.HeaderNames.{COOKIE => CookieHeaderName}
import controllers.common.CookieCrypto

trait CookieCryptoFilter extends Filter with Encrypter with Decrypter {

  val cookieName: String

  override def apply(next: (RequestHeader) => Future[SimpleResult])(rh: RequestHeader): Future[SimpleResult] = {

    val resultWithPlainCookie = next(decryptCookie(rh))

//    encryptCookie(resultWithPlainCookie)
    resultWithPlainCookie
  }

  private def decryptCookie(rh: RequestHeader): RequestHeader = {

    val cookies = rh.headers.getAll(CookieHeaderName).flatMap(Cookies.decode)

    if (cookies.isEmpty) rh else rh.copy(headers = new Headers {
      override protected val data: Seq[(String, Seq[String])] = {


        val updatedCookies: Seq[Cookie] = cookies.map {
          case c if c.name == cookieName && !c.value.isEmpty => c.copy(value = decrypt(c.value))
          case other => other
        }

        if (updatedCookies.isEmpty) rh.headers.toMap.toSeq
        else (rh.headers.toMap + (CookieHeaderName -> Seq(Cookies.encode(updatedCookies)))).toSeq
      }
    })
  }


  private def encryptCookie(resultWithPlainCookie: Future[SimpleResult]): Future[SimpleResult] = ???
}

object SessionCookieCryptoFilter extends CookieCryptoFilter with CookieCrypto {
  val cookieName: String = Session.COOKIE_NAME
}