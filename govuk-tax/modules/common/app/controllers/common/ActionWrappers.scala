package controllers.common

import play.api.mvc._
import controllers.common.service._
import com.google.common.net.HttpHeaders

trait HeaderNames {
  val requestId = "X-Request-ID"
  val authorisation = HttpHeaders.AUTHORIZATION
  val forwardedFor = "x-forwarded-for"
  val xSessionId = "X-Session-ID"
}

object HeaderNames extends HeaderNames

@deprecated("please use Actions", "24.10.13")
trait ActionWrappers
  extends MicroServices
  with Actions