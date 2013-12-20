package uk.gov.hmrc.common

import org.scalatest.mock.MockitoSugar
import controllers.common.{SessionKeys, CookieEncryption}
import play.api.test.{FakeRequest, FakeApplication, WithApplication}

class AffinityGroupParserSpec extends BaseSpec with MockitoSugar {

  class AffinityGroupParserTest extends AffinityGroupParser

  "getAffinityGroup" should {

    "retrieve the affinity group from the request when affinity group is found in the session" in new WithApplication(FakeApplication()) {

      val affinityGroupParser = new AffinityGroupParserTest
      implicit val request = FakeRequest().withSession(SessionKeys.affinityGroup -> CookieEncryption.encrypt("Organisation"))

      affinityGroupParser.parseAffinityGroup shouldBe "Organisation"
    }

    "throw an exception whe affinity group is not found in the session" in new WithApplication(FakeApplication()) {

      val affinityGroupParser = new AffinityGroupParserTest
      implicit val request = FakeRequest()

      intercept[RuntimeException] {
        affinityGroupParser.parseAffinityGroup
      }

    }

  }

}
