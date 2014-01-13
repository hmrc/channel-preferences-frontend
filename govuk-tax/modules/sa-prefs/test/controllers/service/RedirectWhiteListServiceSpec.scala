package controllers.service

import org.scalatest._
import java.net.URL
import java.net.URLEncoder
import controllers.sa.prefs.service.RedirectWhiteListService

class RedirectWhiteListServiceSpec extends WordSpec with ShouldMatchers {
  val allowedHost = "localhost"
  val disallowedHost = "monkey"
  val allowedUrl = URLEncoder.encode(s"http://$allowedHost:8080/portal", "UTF-8")
  val disallowedUrl = URLEncoder.encode(s"http://$disallowedHost:8080/portal", "UTF-8")

  def createService = {
    new RedirectWhiteListService(Set(allowedHost))
  }

  "check url" should {
    "return false for a disallowed host" in {
      val returned = createService.check(disallowedUrl)

      returned shouldBe false
    }

    "return true for an allowed host" in {
      val returned = createService.check(allowedUrl)

      returned shouldBe true
    }
  }
}