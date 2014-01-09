package uk.gov.hmrc.common

import org.scalatest.mock.MockitoSugar
import controllers.common.SessionKeys
import play.api.test.{FakeRequest, FakeApplication, WithApplication}

class AffinityGroupParserSpec extends BaseSpec with MockitoSugar {

  "getAffinityGroup" should {

    "retrieve the affinity group from the request when affinity group is found in the session" in new WithApplication(FakeApplication()) {

      val affinityGroupParser = new AffinityGroupParser {}
      implicit val request = FakeRequest().withSession(SessionKeys.affinityGroup -> "Organisation")

      affinityGroupParser.parseAffinityGroup shouldBe "Organisation"
    }

    "throw an exception whe affinity group is not found in the session" in new WithApplication(FakeApplication()) {

      val affinityGroupParser = new AffinityGroupParser {}
      implicit val request = FakeRequest()

      intercept[RuntimeException] {
        affinityGroupParser.parseAffinityGroup
      }

    }

  }

}
