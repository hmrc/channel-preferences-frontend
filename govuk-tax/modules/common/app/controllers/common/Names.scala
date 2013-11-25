package controllers.common

import com.google.common.net.HttpHeaders

trait HeaderNames {
  val xRequestId = "X-Request-ID"
  val authorisation = HttpHeaders.AUTHORIZATION
  val forwardedFor = "x-forwarded-for"
  val xSessionId = "X-Session-ID"
}
object HeaderNames extends HeaderNames

trait CookieNames {
  val deviceFingerprint = "mdtpdf"
}
object CookieNames extends CookieNames