package controllers.common

import com.google.common.net.HttpHeaders

trait HeaderNames {
  val authorisation = HttpHeaders.AUTHORIZATION
  val xForwardedFor = "x-forwarded-for"
  val xRequestId = "X-Request-ID"
  val xRequestTimestamp = "X-Request-Timestamp"
  val xSessionId = "X-Session-ID"
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
  val lastRequestTimestamp = "ts"
  val redirect = "login_redirect"
  val npsVersion = "nps-version"
}
