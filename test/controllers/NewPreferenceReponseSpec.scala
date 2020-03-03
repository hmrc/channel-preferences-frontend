/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers

import connectors.{ EmailPreference, PreferenceResponse, TermsAndConditonsAcceptance }
import connectors.PreferenceResponse._
import play.api.libs.json.Json
import org.scalatestplus.play.PlaySpec

class NewPreferenceReponseSpec extends PlaySpec {

  "it" should {
    "work" in {

      val json = Json.parse(s"""{
                               |  "termsAndConditions": {
                               |    "generic": {
                               |      "accepted": true
                               |    }
                               |  },
                               |  "email": {
                               |    "email": "pihklyljtgoxeoh@mail.com",
                               |    "isVerified": true,
                               |    "hasBounces": false,
                               |    "mailboxFull": false,
                               |    "status": "verified"
                               |  },
                               |  "digital": true
                               |}""".stripMargin)

      json.as[PreferenceResponse] mustBe PreferenceResponse(
        Map("generic" -> TermsAndConditonsAcceptance(true)),
        Some(EmailPreference("pihklyljtgoxeoh@mail.com", true, false, false, None)))
    }
  }
}
