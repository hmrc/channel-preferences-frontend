package controllers.common.service

import java.net.URL

class SsoWhiteListService(allowedDomains: Set[String]) {

  def check(url: URL): Boolean = allowedDomains.exists(url.getHost.endsWith(_))

}
