package controllers.agent.registration

import uk.gov.hmrc.common.BaseSpec
import play.api.test.{FakeRequest, WithApplication}
import play.api.test.Helpers._
import controllers.agent.registration.FormNames._
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import play.api.test.FakeApplication
import scala.Some
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, Matchers}
import controllers.agent.registration.AgentTypeAndLegalEntityFormFields._
import concurrent.Future
import uk.gov.hmrc.common.microservice.keystore.KeyStoreMicroService
import org.scalatest.mock.MockitoSugar

class AgentTypeAndLegalEntityControllerSpec extends BaseSpec with MockitoSugar {

  val id = "wshakespeare"
  val authority = s"/auth/oid/$id"
  val uri = "/personal/paye/blah"

  val payeRoot = PayeRoot("CE927349E", 1, "Mr", "Will", None, "Shakespeare", "Will Shakespeare", "1983-01-02", Map(), Map(), Map())
  val user = User(id, null, RegimeRoots(Some(payeRoot), None, None, None, None), None, None)

  val keyStoreMicroService = mock[KeyStoreMicroService]

  private val controller = new AgentTypeAndLegalEntityController(null, keyStoreMicroService)(null)

  "The agent type and legal entity" should {

    "display the agent type and legal entity form" in new WithApplication(FakeApplication()) {
      val result = Future.successful(controller.agentTypeAction(user, FakeRequest()))
      status(result) shouldBe 200
    }

    "not go to the next step if no agent type is chosen" in new WithApplication(FakeApplication()) {
      val result = Future.successful(controller.postAgentTypeAction(user, newRequest("", "ltdCompany")))
      status(result) shouldBe 400
      contentAsString(result) should include("This field is required")
      verifyZeroInteractions(keyStoreMicroService)
    }

    "not go to the next step if no legal entity is chosen" in new WithApplication(FakeApplication()) {
      val result = Future.successful(controller.postAgentTypeAction(user, newRequest("inBusiness", "")))
      status(result) shouldBe 400
      contentAsString(result) should include("This field is required")
      verifyZeroInteractions(keyStoreMicroService)
    }
    "not go to the next step if an illegal legal entity is chosen" in new WithApplication(FakeApplication()) {
      val result = Future.successful(controller.postAgentTypeAction(user, newRequest("inBusiness", "invalid")))
      status(result) shouldBe 400
      contentAsString(result) should include("Please select a valid option")
      verifyZeroInteractions(keyStoreMicroService)
    }
    "not go to the next step if an illegal agent type is chosen" in new WithApplication(FakeApplication()) {
      val result = Future.successful(controller.postAgentTypeAction(user, newRequest("aslkjddhjks", "ltdCompany")))
      status(result) shouldBe 400
      contentAsString(result) should include("Please select a valid option")
      verifyZeroInteractions(keyStoreMicroService)
    }
    "go to the company details page and save data in keystore if all items are chosen" in new WithApplication(FakeApplication()) {
      val keyStoreDataCaptor = ArgumentCaptor.forClass(classOf[Map[String, String]])
      val result = Future.successful(controller.postAgentTypeAction(user, newRequest("inBusiness", "ltdCompany")))
      status(result) shouldBe 303
      headers(result).get("Location") should contain("/company-details")
      verify(keyStoreMicroService).addKeyStoreEntry(
        Matchers.eq(controller.registrationId(user)),
        Matchers.eq(controller.agent),
        Matchers.eq(agentTypeAndLegalEntityFormName),
        keyStoreDataCaptor.capture())(Matchers.any())
      val keyStoreData: Map[String, String] = keyStoreDataCaptor.getAllValues.get(0)
      keyStoreData(agentType) should be("inBusiness")
      keyStoreData(legalEntity) should be("ltdCompany")
    }
  }

  def newRequest(agentTypeVal: String, legalEntityVal: String) =
    FakeRequest().withFormUrlEncodedBody(agentType -> agentTypeVal, legalEntity -> legalEntityVal)
}
