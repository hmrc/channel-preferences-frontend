package controllers.service

import org.scalatest._
import controllers.sa.prefs.service.RedirectWhiteListService
import com.netaporter.uri.dsl.stringToUri

class RedirectWhiteListServiceSpec extends WordSpec with ShouldMatchers {
  val allowedHost = "localhost"

  def createService = {
    new RedirectWhiteListService(Set(allowedHost))
  }

  "check url" should {
    "return false for a disallowed host" in {
      createService.check(s"http://monkey:8080/portal") shouldBe false
    }

    "return true for an allowed host" in {
      createService.check(s"http://$allowedHost:8080/portal") shouldBe true
    }

    "return false for a URL without a host" in {
      createService.check("/portal") shouldBe false
    }
  }
}