package config

import play.api.mvc._
import controllers.CookieEncryption
import org.joda.time.DateTime

object LastRequestTimestampFilter extends LastRequestTimestampFilter {
  override def now = new DateTime
}

trait LastRequestTimestampFilter extends Filter with CookieEncryption {

  private[config] val lastRequestTimestampCookieName = "PLAY_REQUEST"
  private[config] val timeoutMinutes = 15

  def apply(next: RequestHeader => Result)(requestHeader: RequestHeader) = {
    next(requestHeader).withCookies(Cookie(lastRequestTimestampCookieName, toCookieValue(now), Some(timeoutMinutes * 60)))
  }

  //  private def checkIncomingTimestamp(requestHeader: RequestHeader): RequestHeader = {
  //    extractLastRequestTimestamp(requestHeader) match {
  //      case None => deleteSession(requestHeader)
  //      case Some(timestamp) if sessionHasExpired(timestamp) => deleteSession(requestHeader)
  //      case _ => requestHeader
  //    }
  //  }
  //
  //  private def deleteSession(rh: RequestHeader): RequestHeader = {
  //    val cookieHeaders: Seq[String] = rh.headers.toMap.get("Cookie").getOrElse(Nil)
  //    val cookiesMinusSession: Seq[String] = cookieHeaders.filter(!_.startsWith(Session.COOKIE_NAME + "="))
  //
  //    val newHeaders = new Headers {
  //      override val data: Seq[(String, Seq[String])] = (rh.headers.toMap + ("Cookie" -> cookiesMinusSession)).toSeq
  //    }
  //
  //    rh.copy(headers = newHeaders)
  //  }
  //
  //  private def sessionHasExpired(lastRequestTimestamp: Long) = {
  //
  //    val minutesSinceLastRequest = (now - lastRequestTimestamp) / (60L * 1000L)
  //
  //    Logger.debug("Last Request Timestamp: " + lastRequestTimestamp)
  //    Logger.debug("Time Now: " + now)
  //    Logger.debug("Minutes since last request: " + minutesSinceLastRequest)
  //
  //    minutesSinceLastRequest >= timeoutMinutes
  //  }
  //
  //  private def extractLastRequestTimestamp(rh: RequestHeader): Option[Long] = {
  //    try {
  //      rh.cookies.get(lastRequestTimestampCookieName).map(cookie => fromCookieValue(cookie.value).toLong)
  //    } catch {
  //      case e: NumberFormatException => None
  //    }
  //  }
  //
  private def toCookieValue(requestTimestamp: DateTime) = encrypt(requestTimestamp.getMillis.toString)

  protected def now: DateTime
}

