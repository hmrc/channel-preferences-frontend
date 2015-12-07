import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.scalatest.{Suite, BeforeAndAfterAll}
import play.api.test.TestServer

trait ServerSetup extends BeforeAndAfterAll {
  this: Suite =>

  val t = TestServer(9000)

  override def beforeAll() = {
    super.beforeAll()
    t.start()

    val wireMockServer = new WireMockServer(wireMockConfig().port(8080))
    wireMockServer.start()
  }

  override def afterAll() = {
    super.afterAll()
    t.stop()
  }

}
