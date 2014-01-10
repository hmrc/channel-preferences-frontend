package controllers.common

import com.google.common.net.HttpHeaders

trait HeaderNames {
  val xRequestId = "X-Request-ID"
  val authorisation = HttpHeaders.AUTHORIZATION
  val forwardedFor = "x-forwarded-for"
  val xSessionId = "X-Session-ID"
  val xRequestTimestamp = "X-Request-Timestamp"
}
object HeaderNames extends HeaderNames

trait CookieNames {
  val deviceFingerprint = "mdtpdf"
}
object CookieNames extends CookieNames

object SessionKeys {
  val sessionId = "sessionId"
  val userId = "userId"
  val name = "name"
  val token = "token"
  val affinityGroup = "affinityGroup"
  val lastRequestTimestamp = SessionTimeoutWrapper.lastRequestTimestampKey
  val redirect = "login_redirect"
  val npsVersion = "nps-version"
}
