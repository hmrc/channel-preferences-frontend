package controllers.agent.registration

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.microservice.MockMicroServicesForTests
import org.mockito.Mockito._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.microservice.domain.User
import uk.gov.hmrc.microservice.domain.RegimeRoots
import uk.gov.hmrc.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.common.microservice.keystore.KeyStore
import scala.Some
import uk.gov.hmrc.common.microservice.agent.Agent

class AgentThankYouControllerSpec extends BaseSpec with MockitoSugar {

  val mockKeyStore = mock[KeyStore[String]]
  val mockAgent = mock[Agent]

  val id = "wshakespeare"

  val authority = s"/auth/oid/$id"
  val uri = "/personal/paye/blah"

  val payeRoot = PayeRoot("CE927349E", 1, "Mr", "Will", None, "Shakespeare", "Will Shakespeare", "1983-01-02", Map(), Map())

  val user = User(id, null, RegimeRoots(Some(payeRoot), None, None), None, None)

  private val controller = new AgentThankYouController with MockMicroServicesForTests {

    override def toAgent(implicit keyStore: KeyStore[String]) = {
      mockAgent
    }

  }
  "AgentThankYouController" should {

    "get the keystore, save the agent, delete the keystore and go to the thank you page" in {

      when(controller.keyStoreMicroService.getKeyStore[String](controller.registrationId(user), controller.agent)).thenReturn(Some(mockKeyStore))
      when(controller.agentMicroService.create(mockAgent)).thenReturn(Some(mockAgent))
      when(mockAgent.uar).thenReturn(Some("12345"))

      val result = controller.thankYouAction(user, FakeRequest())
      status(result) shouldBe 200

      verify(controller.keyStoreMicroService).getKeyStore[String](controller.registrationId(user), controller.agent)
      verify(controller.agentMicroService).create(mockAgent)
      verify(controller.keyStoreMicroService).deleteKeyStore(controller.registrationId(user), controller.agent)

    }

    "redirect user to contact details page when keystore is not found" in {

      when(controller.keyStoreMicroService.getKeyStore(controller.registrationId(user), controller.agent)).thenReturn(None)

      val result = controller.thankYouAction(user, FakeRequest())
      status(result) shouldBe 303
      headers(result)("Location") must be("/home")
      verifyZeroInteractions(controller.agentMicroService)

    }

  }

}
