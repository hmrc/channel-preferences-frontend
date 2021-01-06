package uk.gov.hmrc.channelpreferencesfrontend

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.common.Slf4jNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.scalatest.Matchers.convertToAnyShouldWrapper
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach }
import org.scalatestplus.play.PlaySpec
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.channelpreferences.controllers.ProxyController

import scala.concurrent.Await
import scala.concurrent.duration._

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
class ProxyITSpec extends PlaySpec with BeforeAndAfterEach with BeforeAndAfterAll {

  implicit val system: ActorSystem = ActorSystem("system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  lazy val mockServer = new WireMockServer(
    wireMockConfig()
      .dynamicHttpsPort()
      .dynamicPort()
      .notifier(new Slf4jNotifier(true))
  )

  lazy val mockServerHttpUrl = s"http://localhost:${mockServer.port()}"

  override def beforeAll(): Unit = {
    super.beforeAll()
    mockServer.start()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    mockServer.stop()
  }

  override protected def afterEach(): Unit =
    mockServer.resetAll()

  trait Setup {
    val application = new GuiceApplicationBuilder()
      .configure("preferencesFrontendUrl" -> mockServerHttpUrl)
      .configure("metrics.enabled" -> false)
      .build()

    val proxyController = application.injector.instanceOf[ProxyController]

    mockServer.addStubMapping(
      get(urlPathMatching("/ping/ping")).willReturn(aResponse().withStatus(200).withBody("OK")).build()
    )
  }

  "the proxied outbound request" must {
    "preserve the HTTP method of the inbound request" in new Setup {
      val fakeRequest = FakeRequest("GET", "/ping/ping")
        .withHeaders("User-Agent" -> "test-user-agent")

      val result: Result = Await.result(proxyController.proxy("/ping/ping")(fakeRequest).run(), 60.seconds)

      result.header.status should be(OK)

      mockServer.verify(getRequestedFor(urlPathEqualTo("/ping/ping")))

      Await.result(application.stop(), 60.seconds)
    }

    "preserve the body of the inbound request" in new Setup {
      mockServer
        .addStubMapping(
          post(urlPathMatching("/ping/ping"))
            .willReturn(
              aResponse()
                .withStatus(200)
            )
            .build()
        )

      val body: Source[ByteString, NotUsed] = Source.single(ByteString("RequestBody"))

      val fakeRequest = FakeRequest("POST", "/ping/ping")
        .withHeaders("User-Agent" -> "test-user-agent")
        .withTextBody("RequestBody")

      val result = Await.result(proxyController.proxy("/ping/ping")(fakeRequest).run(body), 60.seconds)

      result.header.status should be(OK)

      mockServer
        .verify(
          postRequestedFor(urlPathEqualTo("/ping/ping"))
            .withRequestBody(equalTo("RequestBody"))
        )

      Await.result(application.stop(), 60.seconds)
    }

    "preserve the headers of the inbound request" in new Setup {
      val fakeRequest = FakeRequest("GET", "/ping/ping")
        .withHeaders(
          "User-Agent" -> "test-user-agent",
          "Accept"     -> "application/json",
          "Cookie"     -> "cookie-1",
          "Cookie"     -> "cookie-2"
        )

      val result = Await.result(proxyController.proxy("/ping/ping")(fakeRequest).run(), 60.seconds)

      result.header.status shouldBe OK

      mockServer.verify(
        getRequestedFor(urlPathEqualTo("/ping/ping"))
          .withHeader("Accept", equalTo("application/json"))
          .withHeader("Cookie", equalTo("cookie-1"))
          .withHeader("Cookie", equalTo("cookie-2"))
      )

      Await.result(application.stop(), 60.seconds)
    }

    "preserve the query params of the inbound request" in new Setup {
      val fakeRequest = FakeRequest("GET", "/ping/ping?key=value&key=value_2&key_2=value_3")
        .withHeaders("User-Agent" -> "test-user-agent")
      Await.result(proxyController.proxy("/ping/ping")(fakeRequest).run(), 60.seconds)

      mockServer.verify(
        getRequestedFor(urlPathEqualTo("/ping/ping"))
          .withQueryParam("key", equalTo("value"))
          .withQueryParam("key", equalTo("value_2"))
          .withQueryParam("key_2", equalTo("value_3"))
      )

      Await.result(application.stop(), 60.seconds)
    }

    "add any extra headers configured" in new Setup {
      val fakeRequest = FakeRequest("GET", "/ping/ping")
        .withHeaders(
          "User-Agent" -> "test-user-agent",
          "Accept"     -> "application/json",
          "Cookie"     -> "cookie-1",
          "Cookie"     -> "cookie-2"
        )

      val result = Await.result(proxyController.proxy("/ping/ping")(fakeRequest).run(), 60.seconds)

      result.header.status shouldBe OK

      mockServer.verify(
        getRequestedFor(urlPathEqualTo("/ping/ping"))
          .withHeader("Accept", equalTo("application/json"))
          .withHeader("Cookie", equalTo("cookie-1"))
          .withHeader("Cookie", equalTo("cookie-2"))
      )

      Await.result(application.stop(), 60.seconds)
    }
  }

  "the proxied response" must {
    "preserve the body of the inbound response" in new Setup {
      val fakeRequest = FakeRequest("GET", "/ping/ping")
        .withHeaders("User-Agent" -> "test-user-agent")

      val result = proxyController.proxy("/ping/ping")(fakeRequest).run()

      contentAsString(result) shouldBe "OK"

      Await.result(application.stop(), 60.seconds)
    }

    "preserve the headers of the inbound response" in new Setup {
      mockServer.addStubMapping(
        get(urlPathMatching("/ping/ping"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withHeader("Server", "Apache")
              .withHeader("Server", "Tomcat")
          )
          .build()
      )

      val fakeRequest = FakeRequest("GET", "/ping/ping")
        .withHeaders("User-Agent" -> "test-user-agent")

      val result = Await.result(proxyController.proxy("/ping/ping")(fakeRequest).run(), 60.seconds)

      result.header.headers("Server") shouldBe "Apache,Tomcat"

      Await.result(application.stop(), 60.seconds)
    }
  }

}
