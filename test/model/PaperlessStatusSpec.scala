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

import helpers.Resources
import model.Category.ActionRequired
import model.StatusName.EmailNotVerified
import model._
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{ JsString, JsValue, Json }

class PaperlessStatusSpec extends PlaySpec {

  val paperlessStatusJson: JsValue = Resources.readJson("PaperlessStatusNotVerified.json")

  val paperlessStatusModel = StatusWithUrl(
    PaperlessStatus(
      EmailNotVerified,
      ActionRequired,
      "You need to verify your email address before you can get Self Assessment tax letters online"),
    Url(
      "http://localhost:9024/paperless/check-settings?returnUrl=VYBxyuFWQBQZAGpe5tSgmw%3D%3D&returnLinkText=VYBxyuFWQBQZAGpe5tSgmw%3D%3D",
      "Verify your email address"
    )
  )

  "PaperlessStatus model" should {
    "Serialise into the correct PaperlessStatus json structure" in {
      Json.toJson(paperlessStatusModel) mustBe paperlessStatusJson
    }

    "Deserialise into a PaperlessStatus Model" in {
      paperlessStatusJson.as[StatusWithUrl] mustBe paperlessStatusModel
    }
  }

  "model.StatusName" should {
    "Serialise into the correct json structure" in {
      StatusName.values.map(enum => Json.toJson(enum)).toList mustBe List(
        JsString("PAPER"),
        JsString("EMAIL_NOT_VERIFIED"),
        JsString("BOUNCED_EMAIL"),
//        JsString("WELSH_AVAILABLE"),
        JsString("ALRIGHT"),
        JsString("NEW_CUSTOMER"),
        JsString("NO_EMAIL")
      )
    }
  }

  "model.Category" should {
    "Serialise into the correct json structure" in {
      Category.values.map(name => Json.toJson(name)).toList mustBe List(
        JsString("ACTION_REQUIRED"),
//        JsString("OPTION_AVAILABLE"),
        JsString("INFO"))
    }
  }
}
