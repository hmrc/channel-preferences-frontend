package controllers.agent.registration

import uk.gov.hmrc.common.{MockUtils, BaseSpec}
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import play.api.test.{WithApplication, FakeApplication, FakeRequest}
import play.api.test.Helpers._
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.common.microservice.keystore.{KeyStoreMicroService, KeyStore}
import scala.Some
import uk.gov.hmrc.common.microservice.agent.AgentRoot
import uk.gov.hmrc.domain.Uar
import models.agent.AgentRegistrationRequest
import service.agent.AgentMicroService
import concurrent.Future
import org.scalatest.TestData

class AgentThankYouControllerSpec extends BaseSpec with MockitoSugar {

  //FIXME: why does this need to be a mock? Why not a stub?
  val mockKeyStore = mock[KeyStore[Map[String, String]]]

  val agentRegistrationRequest = mock[AgentRegistrationRequest]
  val agentRoot = mock[AgentRoot]

  val id = "wshakespeare"

  val authority = s"/auth/oid/$id"
  val uri = "/personal/paye/blah"

  val payeRoot = PayeRoot("CE927349E", 1, "Mr", "Will", None, "Shakespeare", "Will Shakespeare", "1983-01-02", Map(), Map(), Map())

  val user = User(id, null, RegimeRoots(paye = Some(payeRoot)), None, None)

  val agentMicroService = mock[AgentMicroService]
  val keyStoreMicroService = mock[KeyStoreMicroService]

  private val controller = new AgentThankYouController(null, keyStoreMicroService)(agentMicroService, null) {
    override def toAgent(implicit keyStore: KeyStore[Map[String, String]]) = {
      agentRegistrationRequest
    }
  }

  override protected def beforeEach(testData: TestData): Unit = {
    MockUtils.resetAll(agentMicroService, keyStoreMicroService, agentRegistrationRequest, agentRoot, mockKeyStore)
  }

  "AgentThankYouController" should {

    "get the keystore, save the agent, delete the keystore and go to the thank you page" in new WithApplication(FakeApplication()) {

      when(keyStoreMicroService.getKeyStore[Map[String, String]](controller.registrationId(user), controller.agent)).thenReturn(Some(mockKeyStore))
      when(agentMicroService.create("CE927349E", agentRegistrationRequest)).thenReturn(Uar("12345"))
      when(agentRoot.uar).thenReturn("12345")

      val result = Future.successful(controller.thankYouAction(user, FakeRequest()))
      status(result) shouldBe 200

      verify(keyStoreMicroService).getKeyStore[Map[String, String]](controller.registrationId(user), controller.agent)
      verify(agentMicroService).create("CE927349E", agentRegistrationRequest)
      verify(keyStoreMicroService).deleteKeyStore(controller.registrationId(user), controller.agent)

    }

    "redirect user to contact details page when keystore is not found" in new WithApplication(FakeApplication()) {

      when(keyStoreMicroService.getKeyStore[Map[String, String]](controller.registrationId(user), controller.agent)).thenReturn(None)

      val result = Future.successful(controller.thankYouAction(user, FakeRequest()))
      status(result) shouldBe 303
      headers(result).get("Location") should contain("/home")
      verifyZeroInteractions(agentMicroService)

    }

  }

}
