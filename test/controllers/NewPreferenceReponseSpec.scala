package controllers

import connectors.{EmailPreference, NewPreferenceResponse, TermsAndConditonsAcceptance}
import connectors.NewPreferenceResponse._
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

      json.as[NewPreferenceResponse] shouldBe NewPreferenceResponse(Map("generic" -> TermsAndConditonsAcceptance(true)), Some(EmailPreference("pihklyljtgoxeoh@mail.com", true, false, false, None)))
    }
  }
}
