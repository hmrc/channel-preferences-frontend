package config

import uk.gov.hmrc.common.BaseSpec
import play.api.test.WithApplication
import play.api.test.FakeApplication

class PortalConfigSpec extends BaseSpec {

  val mockConfigValues = Map("govuk-tax.Test.portal.destinationPath.saViewAccountDetails" -> "/self-assessment/view",
                             "govuk-tax.Test.portal.destinationPath.saFileAReturn" -> "/self-assessment/file-a-return",
                             "govuk-tax.Test.portal.destinationRoot" -> "http://someserver:8080")

  "Portal Config " should {
    "return a fully qualified URL including the portal destination server and the path for saViewAccountDetails " in new WithApplication(FakeApplication(additionalConfiguration = mockConfigValues)) {
      val actualDestinationUrl = PortalConfig.getDestinationUrl("saViewAccountDetails")
      actualDestinationUrl shouldBe "http://someserver:8080/self-assessment/view"
    }

    "return a fully qualified URL including the portal destination server and the path for saFileAReturn " in new WithApplication(FakeApplication(additionalConfiguration = mockConfigValues)) {
      val actualDestinationUrl = PortalConfig.getDestinationUrl("saFileAReturn")
      actualDestinationUrl shouldBe "http://someserver:8080/self-assessment/file-a-return"
    }

    "return a URL containing just the destination root when the path key is not found " in new WithApplication(FakeApplication(additionalConfiguration = mockConfigValues)) {
      val actualDestinationUrl = PortalConfig.getDestinationUrl("keyNotFound")
      actualDestinationUrl shouldBe "http://someserver:8080"
    }
  }

}
