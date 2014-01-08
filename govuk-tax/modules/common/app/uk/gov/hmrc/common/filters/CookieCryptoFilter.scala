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

    if (cookies.isEmpty) rh
    else rh.copy(headers = new Headers {
      override protected val data: Seq[(String, Seq[String])] = {

        val updatedCookies: Seq[Cookie] = cookies.flatMap {
          case c if c.name == cookieName && !c.value.isEmpty =>
            try {
              Some(c.copy(value = decrypt(c.value)))
            } catch {
              case _: Exception => None
            }
          case other => Some(other)
        }

        val updatedHeaders =
          if (updatedCookies.isEmpty) rh.headers.toMap - HeaderNames.COOKIE
          else rh.headers.toMap + (HeaderNames.COOKIE -> Seq(Cookies.encode(updatedCookies)))

        updatedHeaders.toSeq
      }
    })
  }


  private def encryptCookie(f: Future[SimpleResult]): Future[SimpleResult] = f.map {
    result =>
      val updatedHeader: Option[String] = result.header.headers.get(HeaderNames.SET_COOKIE).map {
        cookieHeader =>
          Cookies.encode(Cookies.decode(cookieHeader).map {
            case c if c.name == cookieName && !c.value.isEmpty => c.copy(value = encrypt(c.value))
            case other => other
          })
      }
    
      updatedHeader.map(header => result.withHeaders(HeaderNames.SET_COOKIE -> header)).getOrElse(result)
  }
}

object SessionCookieCryptoFilter extends CookieCryptoFilter with CookieCrypto {
  val cookieName: String = Session.COOKIE_NAME
}