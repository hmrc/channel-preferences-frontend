package controllers.common

import com.google.common.net.HttpHeaders

object HeaderNames {
  val authorisation = HttpHeaders.AUTHORIZATION
  val xForwardedFor = "x-forwarded-for"
  val xRequestId = "X-Request-ID"
  val xRequestTimestamp = "X-Request-Timestamp"
  val xSessionId = "X-Session-ID"
}

object CookieNames {
  val deviceFingerprint = "mdtpdf"
}

object SessionKeys {
  val sessionId = "sessionId"
  val userId = "userId"
  val name = "name"
  val token = "token"
  val authToken = "authToken"
  val affinityGroup = "affinityGroup"
  val authProvider = "ap"
  val lastRequestTimestamp = "ts"
  val redirect = "login_redirect"
  val npsVersion = "nps-version"
}
