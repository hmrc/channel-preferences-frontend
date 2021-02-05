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

package controllers

import connectors.PreferenceResponse._
import connectors.{ EmailPreference, PreferenceResponse, TermsAndConditonsAcceptance }
import helpers.Resources
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.JsValue

class PreferenceResponseSpec extends PlaySpec {

  "it" should {
    "work" in {

      val json: JsValue = Resources.readJson("PreferenceResponseLegacy.json")

      json.as[PreferenceResponse] mustBe PreferenceResponse(
        Map("generic" -> TermsAndConditonsAcceptance(true)),
        Some(EmailPreference("pihklyljtgoxeoh@mail.com", true, false, false, None))
      )
    }

    "work with a major version" in {

      val json: JsValue = Resources.readJson("PreferenceResponseWithMajor.json")

      json.as[PreferenceResponse] mustBe PreferenceResponse(
        Map("generic" -> TermsAndConditonsAcceptance(accepted = true, majorVersion = Option(3))),
        Some(EmailPreference("pihklyljtgoxeoh@mail.com", true, false, false, None))
      )
    }

    "work with a paperless" in {

      val json: JsValue = Resources.readJson("PreferenceResponseWithPaperless.json")
      json.as[PreferenceResponse] mustBe PreferenceResponse(
        Map(
          "generic" -> TermsAndConditonsAcceptance(accepted = true, majorVersion = Option(3), paperless = Some(true))
        ),
        Some(EmailPreference("pihklyljtgoxeoh@mail.com", true, false, false, None))
      )
    }
  }
}
