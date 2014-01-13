package controllers.sa.prefs.service

import java.net.{ URLDecoder, URL }

class RedirectWhiteListService(allowedDomains: Set[String]) {

  def check(url: String): Boolean = {
    allowedDomains.exists(new URL(URLDecoder.decode(url, "UTF-8")).getHost.endsWith(_))
  }

}
