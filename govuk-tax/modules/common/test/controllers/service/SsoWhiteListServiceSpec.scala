package controllers.service

import java.net.URI
import uk.gov.hmrc.common.BaseSpec
import controllers.common.service.SsoWhiteListService

class SsoWhiteListServiceSpec extends BaseSpec {

  val whiteList = Set("gov.uk", "localhost", "hmrc.gov.uk")
  val ssoWhiteListService = new SsoWhiteListService(whiteList)

  "calling check" should {
    "return true when the domain is in the whitelist" in {
      ssoWhiteListService.check(URI.create("http://localhost:8081/platform").toURL) should be(true)
      ssoWhiteListService.check(URI.create("https://www.gov.uk/home?param=2").toURL) should be(true)
      ssoWhiteListService.check(URI.create("https://www.gov.uk:9990/home?param=2").toURL) should be(true)
    }

    "return false when the domain is not in the whitelist" in {
      ssoWhiteListService.check(URI.create("http://gov.com/platform").toURL) should be(false)
    }
  }
}
