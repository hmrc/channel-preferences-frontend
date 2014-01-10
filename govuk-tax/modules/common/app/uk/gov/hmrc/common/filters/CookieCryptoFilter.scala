package uk.gov.hmrc.common.filters

import play.api.mvc._
import scala.concurrent.Future
import play.api.mvc.SimpleResult
import play.api.http.HeaderNames
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try
import uk.gov.hmrc.common.crypto.{Decrypter, Encrypter, SessionCookieCrypto}

trait CookieCryptoFilter extends Filter {

  protected val cookieName: String
  protected val crypto: Encrypter with Decrypter

  override def apply(next: (RequestHeader) => Future[SimpleResult])(rh: RequestHeader) =
    encryptCookie(next(decryptCookie(rh)))

  private def decryptCookie(rh: RequestHeader) = rh.copy(headers = new Headers {
    override protected val data: Seq[(String, Seq[String])] = {
      val updatedCookies = rh.headers.getAll(HeaderNames.COOKIE).flatMap(Cookies.decode).flatMap {
        case c if c.name == cookieName && !c.value.isEmpty => decryptValue(c.value).map(dv => c.copy(value = dv))
        case other => Some(other)
      }

      if (updatedCookies.isEmpty)
        rh.headers.toMap - HeaderNames.COOKIE
      else
        rh.headers.toMap + (HeaderNames.COOKIE -> Seq(Cookies.encode(updatedCookies)))
    }.toSeq

    def decryptValue(value: String): Option[String] = {
      Try(crypto.decrypt(value)).toOption
    }
  })

  private def encryptCookie(f: Future[SimpleResult]): Future[SimpleResult] = f.map {
    result =>
      val updatedHeader: Option[String] = result.header.headers.get(HeaderNames.SET_COOKIE).map {
        cookieHeader =>
          Cookies.encode(Cookies.decode(cookieHeader).map {
            case c if c.name == cookieName && !c.value.isEmpty => c.copy(value = crypto.encrypt(c.value))
            case other => other
          })
      }

      updatedHeader.map(header => result.withHeaders(HeaderNames.SET_COOKIE -> header)).getOrElse(result)
  }
}

object SessionCookieCryptoFilter extends CookieCryptoFilter {
  // Lazy because the filter is instantiated before the config is loaded
  protected override lazy val cookieName: String = Session.COOKIE_NAME

  protected override val crypto = SessionCookieCrypto
}