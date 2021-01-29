/*
 * Copyright 2019 HM Revenue & Customs
 *
 */

import connectors.PreferenceResponse
import controllers.internal.IPage7
import model.HostContext
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.{ HeaderCarrier, HttpClient, SessionKeys }
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.ExecutionContext.Implicits._

class ChoosePaperlessControllerISpec
    extends PlaySpec with GuiceOneAppPerSuite with ScalaFutures with IntegrationPatience {

  override lazy val app = new GuiceApplicationBuilder()
    .configure(
      "play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck"
    )
    .build()

  val authHelper = app.injector.instanceOf[ItAuthHelper]
  val wsClient = app.injector.instanceOf[WSClient]
  val http = app.injector.instanceOf[HttpClient]

  "submitForm" should {
    "should create a preference with specified cohort" in {

      val utr = Generate.utr
      val header = authHelper.authHeader(utr)

      val queryString = model.HostContext.hostContextBinder
        .unbind("anyValName", HostContext(returnUrl = "foo&value", returnLinkText = "bar", cohort = Some(IPage7)))

      val fakeRequest = FakeRequest(POST, s"/paperless/choose?$queryString")
        .withFormUrlEncodedBody(
          "opt-in" -> "true",
          ("email.main", "test@foo.com"),
          ("email.confirm", "test@foo.com"),
          ("emailVerified", "true"),
          "accept-tc" -> "true"
        )
        .withSession(
          (SessionKeys.authToken -> header._2)
        )
        .withHeaders("Content-Type" -> "application/x-www-form-urlencoded")
        .withHeaders(header._1 -> header._2)
        .withHeaders("Csrf-Token" -> "nocheck")
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(fakeRequest.headers)

      val result = route(app, fakeRequest).get
      status(result) mustBe (303)

      val prefererencesResponse =
        http.GET[Option[PreferenceResponse]](s"http://localhost:8015/preferences").futureValue
      prefererencesResponse.get.termsAndConditions("generic").majorVersion.get mustBe (IPage7.majorVersion)
      prefererencesResponse.get.email.get.pendingEmail.get mustBe ("test@foo.com")

    }
  }

}
