/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers

import helpers.Resources
import connectors.PreferenceResponse._
import connectors.{ EmailPreference, PreferenceResponse, TermsAndConditonsAcceptance }
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{ JsValue, Json }

class PreferenceResponseSpec extends PlaySpec {

  "it" should {
    "work" in {

      val json: JsValue = Resources.readJson("PreferenceResponseLegacy.json")

      json.as[PreferenceResponse] mustBe PreferenceResponse(
        Map("generic" -> TermsAndConditonsAcceptance(true)),
        Some(EmailPreference("pihklyljtgoxeoh@mail.com", true, false, false, None)))
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
          "generic" -> TermsAndConditonsAcceptance(accepted = true, majorVersion = Option(3), paperless = Some(true))),
        Some(EmailPreference("pihklyljtgoxeoh@mail.com", true, false, false, None))
      )
    }
  }
}
