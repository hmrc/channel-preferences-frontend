/*
 * Copyright 2020 HM Revenue & Customs
 *
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
