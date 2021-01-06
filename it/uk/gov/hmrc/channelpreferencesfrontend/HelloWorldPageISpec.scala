package uk.gov.hmrc.channelpreferencesfrontend

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers._
//import play.api.test._
//import org.scalatest.Matchers.convertToAnyShouldWrapper
import uk.gov.hmrc.http.HttpClient
//import uk.gov.hmrc.play.HeaderCarrierConverter

//import scala.concurrent.ExecutionContext.Implicits._

class HelloWorldPageISpec extends PlaySpec with GuiceOneAppPerSuite {

  override lazy val app = new GuiceApplicationBuilder()
    .configure(
      "play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck"
    )
    .build()

  val http = app.injector.instanceOf[HttpClient]

  "calling the hello-world route" must {

    "return an OK response" in {

      val fakeRequest = FakeRequest(GET, s"/channel-preferences-frontend/hello-world")
        .withHeaders("Content-Type" -> "application/x-www-form-urlencoded")
        .withHeaders("Csrf-Token" -> "nocheck")
//      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(fakeRequest.headers)

      val result = route(app, fakeRequest).get
      status(result) mustBe OK
    }
  }

}
