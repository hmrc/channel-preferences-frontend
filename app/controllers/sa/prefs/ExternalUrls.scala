package controllers.sa.prefs

import play.api.mvc.Call

object ExternalUrls {
  val accountDetails = Call("GET", "/account/account-details")
}
