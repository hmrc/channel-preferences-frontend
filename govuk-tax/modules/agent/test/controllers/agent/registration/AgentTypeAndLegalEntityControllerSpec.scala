package controllers.agent.registration

import uk.gov.hmrc.common.BaseSpec
import uk.gov.hmrc.microservice.MockMicroServicesForTests
import play.api.test.{ FakeRequest, FakeApplication, WithApplication }
import controllers.common.SessionTimeoutWrapper
import play.api.test.Helpers._

class AgentTypeAndLegalEntityControllerSpec extends BaseSpec with MockAuthentication {

  private def controller = new AgentTypeAndLegalEntityController with MockMicroServicesForTests {
    override lazy val authMicroService = mockAuthMicroService
    override lazy val payeMicroService = mockPayeMicroService
  }

  "The agent type and legal entity" should {
    "not go to the next step if no agent type is chosen" in new WithApplication(FakeApplication()) {
      val result = controller.postDetails()(newRequest("", "ltdCompany"))
      status(result) shouldBe 400
      contentAsString(result) should include("This field is required")
    }

    "not go to the next step if no legal entity is chosen" in new WithApplication(FakeApplication()) {
      val result = controller.postDetails()(newRequest("inBusiness", ""))
      status(result) shouldBe 400
      contentAsString(result) should include("This field is required")
    }
    "go to the next step if all items are chosen" in new WithApplication(FakeApplication()) {
      val result = controller.postDetails()(newRequest("inBusiness", "ltdCompany"))
      status(result) shouldBe 303
    }
    "not go to the next step if an illegal legal entity is chosen" in new WithApplication(FakeApplication()) {
      val result = controller.postDetails()(newRequest("inBusiness", "invalid"))
      status(result) shouldBe 400
      contentAsString(result) should include("Please select a valid option")
    }
    "not go to the next step if an illegal agent type is chosen" in new WithApplication(FakeApplication()) {
      val result = controller.postDetails()(newRequest("aslkjddhjks", "ltdCompany"))
      status(result) shouldBe 400
      contentAsString(result) should include("Please select a valid option")
    }
  }

  def newRequest(agentType: String, legalEntity: String) =
    FakeRequest().withFormUrlEncodedBody("agentType" -> agentType, "legalEntity" -> legalEntity)
      .withSession("userId" -> controller.encrypt(authority), "name" -> controller.encrypt("Will Shakespeare"),
        SessionTimeoutWrapper.sessionTimestampKey -> controller.now().getMillis.toString)

}
