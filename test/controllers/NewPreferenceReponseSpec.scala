package controllers

import connectors.{EmailPreference, PreferenceResponse, TermsAndConditonsAcceptance}
import connectors.PreferenceResponse._
import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec

class NewPreferenceReponseSpec extends UnitSpec{

  "it" should {
    "work" in {

      val json = Json.parse(
        s"""{
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

      json.as[PreferenceResponse] shouldBe PreferenceResponse(Map("generic" -> TermsAndConditonsAcceptance(true)), Some(EmailPreference("pihklyljtgoxeoh@mail.com", true, false, false, None)))
    }
  }
}
