package controllers.agent.registration

import uk.gov.hmrc.common.BaseSpec
import uk.gov.hmrc.microservice.MockMicroServicesForTests
import play.api.test.{ FakeRequest, WithApplication }
import play.api.test.Helpers._
import controllers.agent.registration.FormNames._
import uk.gov.hmrc.microservice.domain.User
import uk.gov.hmrc.microservice.domain.RegimeRoots
import uk.gov.hmrc.microservice.paye.domain.PayeRoot
import play.api.test.FakeApplication
import scala.Some
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.Matchers

class AgentTypeAndLegalEntityControllerSpec extends BaseSpec {

  val id = "wshakespeare"
  val authority = s"/auth/oid/$id"
  val uri = "/personal/paye/blah"

  val payeRoot = PayeRoot("CE927349E", 1, "Mr", "Will", None, "Shakespeare", "Will Shakespeare", "1983-01-02", Map(), Map())
  val user = User(id, null, RegimeRoots(Some(payeRoot), None, None), None, None)

  private val controller = new AgentTypeAndLegalEntityController with MockMicroServicesForTests

  "The agent type and legal entity" should {

    "display the agent type and legal entity form" in new WithApplication(FakeApplication()){
      controller.resetAll
      val result = controller.agentTypeAction(user, FakeRequest())
      status(result) shouldBe 200
    }

    "not go to the next step if no agent type is chosen" in new WithApplication(FakeApplication()) {
      controller.resetAll
      val result = controller.postAgentTypeAction(user, newRequest("", "ltdCompany"))
      status(result) shouldBe 400
      contentAsString(result) should include("This field is required")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }

    "not go to the next step if no legal entity is chosen" in new WithApplication(FakeApplication()) {
      controller.resetAll
      val result = controller.postAgentTypeAction(user, newRequest("inBusiness", ""))
      status(result) shouldBe 400
      contentAsString(result) should include("This field is required")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }
    "go to the next step if all items are chosen" in new WithApplication(FakeApplication()) {
      controller.resetAll
      val result = controller.postAgentTypeAction(user, newRequest("inBusiness", "ltdCompany"))
      status(result) shouldBe 303
      verify(controller.keyStoreMicroService).addKeyStoreEntry(Matchers.eq(s"Registration:$id"), Matchers.eq("agent"), Matchers.eq(agentTypeAndLegalEntityFormName), any[Map[String, Any]]())
    }
    "not go to the next step if an illegal legal entity is chosen" in new WithApplication(FakeApplication()) {
      controller.resetAll
      val result = controller.postAgentTypeAction(user, newRequest("inBusiness", "invalid"))
      status(result) shouldBe 400
      contentAsString(result) should include("Please select a valid option")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }
    "not go to the next step if an illegal agent type is chosen" in new WithApplication(FakeApplication()) {
      controller.resetAll
      val result = controller.postAgentTypeAction(user, newRequest("aslkjddhjks", "ltdCompany"))
      status(result) shouldBe 400
      contentAsString(result) should include("Please select a valid option")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }
  }

  def newRequest(agentType: String, legalEntity: String) =
    FakeRequest().withFormUrlEncodedBody("agentType" -> agentType, "legalEntity" -> legalEntity)
}
