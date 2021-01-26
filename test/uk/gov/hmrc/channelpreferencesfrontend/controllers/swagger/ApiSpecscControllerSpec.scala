/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
