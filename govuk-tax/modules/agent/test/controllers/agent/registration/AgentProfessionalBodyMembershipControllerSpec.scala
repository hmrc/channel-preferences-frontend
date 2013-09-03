package controllers.agent.registration

import play.api.test.{ FakeRequest, WithApplication }
import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.microservice.MockMicroServicesForTests
import org.mockito.Mockito._
import org.mockito.Matchers._
import play.api.test.Helpers._
import org.scalatest.BeforeAndAfterEach
import org.mockito.Matchers
import controllers.agent.registration.FormNames._
import uk.gov.hmrc.microservice.domain.User
import uk.gov.hmrc.microservice.domain.RegimeRoots
import uk.gov.hmrc.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.common.microservice.keystore.KeyStore
import uk.gov.hmrc.common.microservice.agent.Agent
import play.api.test.FakeApplication
import scala.Some

class AgentProfessionalBodyMembershipControllerSpec extends BaseSpec with MockitoSugar with BeforeAndAfterEach {

  val id = "wshakespeare"
  val authority = s"/auth/oid/$id"
  val uri = "/personal/paye/blah"

  val payeRoot = PayeRoot("CE927349E", 1, "Mr", "Will", None, "Shakespeare", "Will Shakespeare", "1983-01-02", Map(), Map())
  val user = User(id, null, RegimeRoots(Some(payeRoot), None, None), None, None)

  val mockAgent = mock[Agent]
  val mockKeyStore = mock[KeyStore[String]]

  private val controller = new AgentProfessionalBodyMembershipController with MockMicroServicesForTests {
  }

  override protected def beforeEach() {
    super.beforeEach()
    controller.resetAll()
  }

  "AgentProfessionalMembershipController" should {
    "not go to the next step if professional body is specified but not the membership number" in new WithApplication(FakeApplication()) {

      val result = controller.postProfessionalBodyMembershipAction(user, newRequestForProfessionalBodyMembership("charteredInstituteOfManagementAccountants", ""))
      status(result) shouldBe 400
      contentAsString(result) should include("You must specify a membership number for your professional body")
      verifyZeroInteractions(controller.keyStoreMicroService, controller.agentMicroService)
    }

    "not go to the next step if professional body is specified but membership number is blank" in new WithApplication(FakeApplication()) {
      val result = controller.postProfessionalBodyMembershipAction(user, newRequestForProfessionalBodyMembership("charteredInstituteOfManagementAccountants", "   "))
      status(result) shouldBe 400
      contentAsString(result) should include("You must specify a membership number for your professional body")
      verifyZeroInteractions(controller.keyStoreMicroService, controller.agentMicroService)
    }

    "not go to the next step if professional body is invalid" in new WithApplication(FakeApplication()) {
      val result = controller.postProfessionalBodyMembershipAction(user, newRequestForProfessionalBodyMembership("sad", ""))
      status(result) shouldBe 400
      contentAsString(result) should include("Please select a valid option")
      verifyZeroInteractions(controller.keyStoreMicroService, controller.agentMicroService)
    }

    "not go to the next step if membership number is specified but not the professional body" in new WithApplication(FakeApplication()) {
      val result = controller.postProfessionalBodyMembershipAction(user, newRequestForProfessionalBodyMembership("", "asdsafd"))
      status(result) shouldBe 400
      contentAsString(result) should include("You must specify which professional body you belong to")
      verifyZeroInteractions(controller.keyStoreMicroService, controller.agentMicroService)
    }

    "go to the next step if no input data is entered" in new WithApplication(FakeApplication()) {
      mockKeyStoreAndAgent
      val result = controller.postProfessionalBodyMembershipAction(user, newRequestForProfessionalBodyMembership("", ""))
      status(result) shouldBe 303
      verify(controller.keyStoreMicroService).addKeyStoreEntry(Matchers.eq(s"Registration:$id"), Matchers.eq("agent"), Matchers.eq(professionalBodyMembershipFormName), any[Map[String, Any]]())
    }

    "go to the next step when input data is entered" in new WithApplication(FakeApplication()) {
      mockKeyStoreAndAgent
      val result = controller.postProfessionalBodyMembershipAction(user, newRequestForProfessionalBodyMembership("charteredInstituteOfManagementAccountants", "data"))
      status(result) shouldBe 303
      verify(controller.keyStoreMicroService).addKeyStoreEntry(Matchers.eq(s"Registration:$id"), Matchers.eq("agent"), Matchers.eq(professionalBodyMembershipFormName), any[Map[String, Any]]())
    }
  }

  def mockKeyStoreAndAgent = {
    when(controller.agentMicroService.create(any[Agent])).thenReturn(Some(mockAgent))
    when(controller.keyStoreMicroService.getKeyStore[String](anyString(), anyString())(any[Manifest[String]])).thenReturn(Some(mockKeyStore))
    when(mockAgent.uar).thenReturn(Some("12324"))
    doNothing().when(controller.keyStoreMicroService).deleteKeyStore(anyString(), anyString())
    when(mockKeyStore.get(anyString())).thenReturn(Some(Map.empty[String, String]))
  }

  def newRequestForProfessionalBodyMembership(professionalBody: String, membershipNumber: String) =
    FakeRequest().withFormUrlEncodedBody("professionalBodyMembership.professionalBody" -> professionalBody, "professionalBodyMembership.membershipNumber" -> membershipNumber)
}