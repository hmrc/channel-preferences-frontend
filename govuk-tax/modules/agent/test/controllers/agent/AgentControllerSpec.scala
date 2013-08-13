package controllers.agent

import play.api.test.{ FakeRequest, FakeApplication, WithApplication }
import uk.gov.hmrc.common.BaseSpec
import controllers.agent.AgentController

class AgentControllerSpec extends BaseSpec {

  import play.api.test.Helpers._

  private def controller = new AgentController

  "The sro check page" should {
    "include two agreements" in new WithApplication(FakeApplication()) {
      val content = contentAsString(controller.sroCheck()(FakeRequest()))
      content should include("Yes, I am the Senior Responsible Officer")
      content should include("I accept the Terms &amp; Conditions")
    }

    "Return a BadRequest error if agreements are not checked" in new WithApplication(FakeApplication()) {

      val result = controller.submitAgreement()(newRequest("false", "false"))
      status(result) shouldBe 400
      contentAsString(result) should include("Please specify that you are the Senior Responsible Officer")
      contentAsString(result) should include("Please accept the terms and conditions")

    }

    "Return a Redirect if agreements are checked" in new WithApplication(FakeApplication()) {

      val result = controller.submitAgreement()(newRequest("true", "true"))

      status(result) shouldBe 303
      header("Location", result) shouldBe Some("/samllogin")

    }
  }

  "The submit agreement page" should {
    "add a register agent entry in the session" in new WithApplication(FakeApplication()) {
      val result = controller.submitAgreement()(newRequest("true", "true"))

      session(result).data("register agent") should equal("true")
    }
  }

  def newRequest(sro: String, tnc: String) =
    FakeRequest().withFormUrlEncodedBody("sroAgreement" -> sro, "tncAgreement" -> tnc)

}
