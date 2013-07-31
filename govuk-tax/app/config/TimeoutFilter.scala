package config

import play.api.mvc._
import controllers.CookieEncryption
import org.joda.time.{ Duration, DateTime }
import play.api.Logger
import scala.Some

object TimeoutFilter extends Filter with CookieEncryption {

  def apply(next: (RequestHeader) => Result)(rh: RequestHeader) = {

    val lastRequestTimestamp: Option[Long] = extractTimestamp(rh)

    val result = lastRequestTimestamp match {
      case None => next(deleteSession(rh))
      case Some(timestamp) if sessionHasExpired(timestamp) => next(deleteSession(rh))
      case _ => next(rh)
    }

    val nextExpiry = new DateTime().getMillis

    result.withCookies(Cookie("PLAY_REQUEST", enc(nextExpiry), Some(15 * 60)))
  }

  private def deleteSession(rh: RequestHeader): RequestHeader = {
    val cookieHeaders: Seq[String] = rh.headers.toMap.get("Cookie").getOrElse(Nil)
    val cookiesMinusSession: Seq[String] = cookieHeaders.filter(!_.startsWith(Session.COOKIE_NAME + "="))

    val newHeaders = new Headers {
      override val data: Seq[(String, Seq[String])] = (rh.headers.toMap + ("Cookie" -> cookiesMinusSession)).toSeq
    }

    rh.copy(headers = newHeaders)
  }

  private def sessionHasExpired(lastRequestTimestamp: Long) = {

    val now = new DateTime().getMillis
    val minutesSinceLastRequest = (now - lastRequestTimestamp) / (60L * 1000L)

    Logger.debug("Last Request Timestamp: " + lastRequestTimestamp)
    Logger.debug("Time Now: " + now)
    Logger.debug("Minutes since last request: " + minutesSinceLastRequest)

    minutesSinceLastRequest >= 15
  }

  private def extractTimestamp(rh: RequestHeader): Option[Long] = {
    try {
      rh.cookies.get("PLAY_REQUEST").map(cookie => dec(cookie.value).toLong)
    } catch {
      case e: NumberFormatException => None
    }
  }

  private def enc(value: Long) = encrypt(value.toString)

  private def dec(value: String) = decrypt(value).toLong

}

