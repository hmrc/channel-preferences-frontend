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

class AgentThankYouControllerSpec extends BaseSpec with MockitoSugar {

  val mockKeyStore = mock[KeyStore]

  val id = "wshakespeare"
  val authority = s"/auth/oid/$id"
  val uri = "/personal/paye/blah"

  val payeRoot = PayeRoot("CE927349E", 1, "Mr", "Will", None, "Shakespeare", "Will Shakespeare", "1983-01-02", Map(), Map())
  val user = User(id, null, RegimeRoots(Some(payeRoot), None, None), None, None)

  private val controller = new AgentThankYouController with MockMicroServicesForTests

  "AgentThankYouController" should {
    "get the uar from the keystore" in {
      when(controller.keyStoreMicroService.getKeyStore(s"UAR:$id", "agent")).thenReturn(Some(mockKeyStore))
      when(mockKeyStore.get("uar")).thenReturn(Some(Map[String, String]("uar" -> "1234")))

      val result = controller.thankYouAction(user, FakeRequest())
      status(result) shouldBe 200
      verify(controller.keyStoreMicroService).deleteKeyStore(s"UAR:$id", "agent")
    }
  }

}
