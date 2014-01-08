package uk.gov.hmrc.common.filters

import play.api.mvc._
import scala.concurrent.Future
import controllers.common.service.{Decrypter, Encrypter}
import play.api.mvc.SimpleResult
import play.api.http.HeaderNames
import controllers.common.CookieCrypto
import scala.concurrent.ExecutionContext.Implicits.global

trait CookieCryptoFilter extends Filter with Encrypter with Decrypter {

  val cookieName: String

  override def apply(next: (RequestHeader) => Future[SimpleResult])(rh: RequestHeader) = encryptCookie(next(decryptCookie(rh)))

  private def decryptCookie(rh: RequestHeader) = {
    val cookies = rh.headers.getAll(HeaderNames.COOKIE).flatMap(Cookies.decode)

    if (cookies.isEmpty) rh else rh.copy(headers = new Headers {
      override protected val data: Seq[(String, Seq[String])] = {

        val updatedCookies: Seq[Cookie] = cookies.map {
          case c if c.name == cookieName && !c.value.isEmpty => c.copy(value = decrypt(c.value))
          case other => other
        }

        if (updatedCookies.isEmpty) rh.headers.toMap.toSeq
        else (rh.headers.toMap + (HeaderNames.COOKIE -> Seq(Cookies.encode(updatedCookies)))).toSeq
      }
    })
  }


  private def encryptCookie(f: Future[SimpleResult]): Future[SimpleResult] = f.map { result =>
    val updatedCookie = result.header.headers.get(HeaderNames.SET_COOKIE).flatMap { cookieHeader =>
      Cookies.decode(cookieHeader).find(_.name == cookieName).map { c =>
        c.copy(value = encrypt(c.value))
      }
    }
  
    updatedCookie.map(c => result.withCookies(c)).getOrElse(result)
  }
}

object SessionCookieCryptoFilter extends CookieCryptoFilter with CookieCrypto {
  val cookieName: String = Session.COOKIE_NAME
}