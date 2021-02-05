/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.channelpreferencesfrontend.controllers.swagger

class ApiSpecscControllerSpec {}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.libs.json.{ JsDefined, JsString }
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
class ApiSpecsControllerSpec extends PlaySpec with GuiceOneAppPerSuite {
  private val fakeRequest = FakeRequest("GET", "/api/schema.json")
  private val controller = new ApiSpecsController(stubMessagesControllerComponents())

  "GET /api/schema.json " should {

    "return 200 and a plain text body" in {

      val result = controller.specs(fakeRequest)
      status(result) mustBe Status.OK
      contentType(result) mustBe Some("text/plain")
      charset(result) mustBe Some("utf-8")
      val json = contentAsJson(result)
      (json \ "info" \ "title") mustBe JsDefined(JsString("Channel Preferences Frontend API"))
    }
  }
}
