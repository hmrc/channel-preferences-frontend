package config

import play.api.mvc._
import controllers.CookieEncryption
import org.joda.time.{ DateTimeZone, Duration, DateTime }

object LastRequestTimestampFilter extends LastRequestTimestampFilter

trait LastRequestTimestampFilter extends Filter with CookieEncryption with DateTimeProvider {

  def apply(next: RequestHeader => Result)(requestHeader: RequestHeader) = {
    next(requestHeader).withCookies(LastRequestTimestampCookie(now()).toCookie)
  }
}

case class LastRequestTimestampCookie(lastRequestTimestamp: DateTime) extends CookieEncryption {

  import LastRequestTimestampCookie._

  def toCookie: Cookie = Cookie(cookieName, cookieValue, Some(timeoutMinutes * 60))

  def isValid(now: () => DateTime): Boolean = {
    val sessionExpiryTimestamp = lastRequestTimestamp.withDurationAdded(Duration.standardMinutes(timeoutMinutes), 1)
    now().isBefore(sessionExpiryTimestamp)
  }

  private def cookieValue = encrypt(lastRequestTimestamp.getMillis.toString)
}

object LastRequestTimestampCookie extends CookieEncryption {

  val cookieName = "PLAY_REQUEST"

  val timeoutMinutes = 15

  def apply(cookie: Cookie): Option[LastRequestTimestampCookie] = {
    require(cookie.name == cookieName)
    decryptCookie(cookie).map(LastRequestTimestampCookie(_))
  }

  private def decryptCookie(cookie: Cookie): Option[DateTime] = {
    try {
      Some(new DateTime(decrypt(cookie.value).toLong, DateTimeZone.UTC))
    } catch {
      case e: NumberFormatException => None
    }
  }
}
