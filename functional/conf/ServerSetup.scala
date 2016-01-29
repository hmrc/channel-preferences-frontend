package conf

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.scalatest.{BeforeAndAfterAll, Suite}
import play.api.test.TestServer


trait ServerSetup extends BeforeAndAfterAll {
  this: Suite =>

  val preferencesFrontendServer = TestServer(9000)
  val wireMockServer = new WireMockServer(wireMockConfig().port(8080))

  override def beforeAll() = {
    super.beforeAll()

    preferencesFrontendServer.start()
    wireMockServer.start()
  }

  override def afterAll() = {
    super.afterAll()

    preferencesFrontendServer.stop()
    wireMockServer.stop()
  }
}

