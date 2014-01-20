package controllers.sa.prefs.service

import java.net.{ URLDecoder, URL }
import com.netaporter.uri.Uri

class RedirectWhiteListService(allowedDomains: Set[String]) {
  def check(url: Uri): Boolean = url.host.map(h => allowedDomains.exists(h.endsWith(_))).getOrElse(false)
}
