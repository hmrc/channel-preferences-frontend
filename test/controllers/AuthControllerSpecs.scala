/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers

import controllers.auth.AuthAction
import helpers.{MockAuthController, MockFailingAuthController}
import org.scalatestplus.play.OneAppPerSuite
import play.api.mvc.Controller
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.play.test.UnitSpec

class AuthControllerSpecs extends UnitSpec with OneAppPerSuite {

  val fakeRequest = FakeRequest("GET", "/")

  class FakeController(authAction: AuthAction) extends Controller {
    def onPageLoad() = authAction { request => Ok }
  }

  "Auth Action" when {
    "the user has authenticated should return a successful response" in {
      val authAction = new MockAuthController(None, None, None, None)
      val controller = new FakeController(authAction)
      val result = controller.onPageLoad()(fakeRequest)
      status(result) shouldBe OK
    }

    "return not authorised then no credentials supplied" in {
      val authAction = new MockFailingAuthController(SessionRecordNotFound())
      val controller = new FakeController(authAction)
      val result = controller.onPageLoad()(fakeRequest)
      status(result) shouldBe 401
    }

  }
}
