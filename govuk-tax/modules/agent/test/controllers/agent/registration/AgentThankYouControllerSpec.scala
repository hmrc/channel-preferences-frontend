package controllers.agent.registration

import uk.gov.hmrc.common.BaseSpec
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
import scala.util.Success
import uk.gov.hmrc.domain.Uar
import models.agent.AgentRegistrationRequest
import service.agent.AgentMicroService

class AgentThankYouControllerSpec extends BaseSpec with MockitoSugar {

  val mockKeyStore = mock[KeyStore[Map[String,String]]]
  val agentRegistrationRequest = mock[AgentRegistrationRequest]
  val agentRoot = mock[AgentRoot]

  val id = "wshakespeare"

  val authority = s"/auth/oid/$id"
  val uri = "/personal/paye/blah"

  val payeRoot = PayeRoot("CE927349E", 1, "Mr", "Will", None, "Shakespeare", "Will Shakespeare", "1983-01-02", Map(), Map(), Map())

  val user = User(id, null, RegimeRoots(Some(Success(payeRoot)), None, None, None, None), None, None)

  private val controller = new AgentThankYouController {

    override lazy val agentMicroService = mock[AgentMicroService]
    override lazy val keyStoreMicroService = mock[KeyStoreMicroService]

    override def toAgent(implicit keyStore: KeyStore[Map[String,String]]) = {
      agentRegistrationRequest
    }

  }
  "AgentThankYouController" should {

    "get the keystore, save the agent, delete the keystore and go to the thank you page" in new WithApplication(FakeApplication()) {

      when(controller.keyStoreMicroService.getKeyStore[Map[String,String]](controller.registrationId(user), controller.agent)).thenReturn(Some(mockKeyStore))
      when(controller.agentMicroService.create("CE927349E", agentRegistrationRequest)).thenReturn(Uar("12345"))
      when(agentRoot.uar).thenReturn("12345")

      val result = controller.thankYouAction(user, FakeRequest())
      status(result) shouldBe 200

      verify(controller.keyStoreMicroService).getKeyStore[Map[String,String]](controller.registrationId(user), controller.agent)
      verify(controller.agentMicroService).create("CE927349E", agentRegistrationRequest)
      verify(controller.keyStoreMicroService).deleteKeyStore(controller.registrationId(user), controller.agent)

    }

    "redirect user to contact details page when keystore is not found" in new WithApplication(FakeApplication()) {

      when(controller.keyStoreMicroService.getKeyStore[Map[String,String]](controller.registrationId(user), controller.agent)).thenReturn(None)

      val result = controller.thankYouAction(user, FakeRequest())
      status(result) shouldBe 303
      headers(result)("Location") should be("/home")
      verifyZeroInteractions(controller.agentMicroService)

    }

  }

}
