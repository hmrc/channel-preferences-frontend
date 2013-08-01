package config

import org.joda.time.{ Duration, DateTimeZone, DateTime }
import controllers.CookieEncryption

trait CookieSupport extends CookieEncryption {

  def validTimestampCookie = {
    LastRequestTimestampCookie(DateTime.now.withZone(DateTimeZone.UTC)).toCookie
  }

  def expiredTimestampCookie = {
    val oldRequestTimestamp = DateTime.now.withZone(DateTimeZone.UTC).minus(Duration.standardMinutes(LastRequestTimestampCookie.timeoutMinutes + 1))
    LastRequestTimestampCookie(oldRequestTimestamp).toCookie
  }

  def brokenTimestampCookie = validTimestampCookie.copy(value = "not-a-valid-value")
}
