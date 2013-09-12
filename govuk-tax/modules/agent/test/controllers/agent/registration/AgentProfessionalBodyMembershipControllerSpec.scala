package controllers.agent.registration

import play.api.test.{ FakeRequest, WithApplication }
import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.microservice.MockMicroServicesForTests
import org.mockito.Mockito._
import play.api.test.Helpers._
import org.scalatest.{ TestData, BeforeAndAfterEach }
import org.mockito.{ ArgumentCaptor, Matchers }
import controllers.agent.registration.FormNames._
import uk.gov.hmrc.microservice.domain.User
import uk.gov.hmrc.microservice.domain.RegimeRoots
import uk.gov.hmrc.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.common.microservice.keystore.KeyStore
import uk.gov.hmrc.common.microservice.agent.Agent
import play.api.test.FakeApplication
import scala.Some
import controllers.agent.registration.AgentProfessionalBodyMembershipFormFields._

class AgentProfessionalBodyMembershipControllerSpec extends BaseSpec with MockitoSugar {

  val id = "wshakespeare"
  val authority = s"/auth/oid/$id"
  val uri = "/personal/paye/blah"

  val payeRoot = PayeRoot("CE927349E", 1, "Mr", "Will", None, "Shakespeare", "Will Shakespeare", "1983-01-02", Map(), Map())
  val user = User(id, null, RegimeRoots(Some(payeRoot), None, None), None, None)

  val mockAgent = mock[Agent]
  val mockKeyStore = mock[KeyStore[String]]

  private val controller = new AgentProfessionalBodyMembershipController with MockMicroServicesForTests

  override protected def beforeEach(testData: TestData) {
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
      val keyStoreDataCaptor = ArgumentCaptor.forClass(classOf[Map[String, String]])
      val result = controller.postProfessionalBodyMembershipAction(user, newRequestForProfessionalBodyMembership("", ""))
      status(result) shouldBe 303
      headers(result)("Location") should be("/thank-you")
      verify(controller.keyStoreMicroService).addKeyStoreEntry(
        Matchers.eq(controller.registrationId(user)),
        Matchers.eq(controller.agent),
        Matchers.eq(professionalBodyMembershipFormName),
        keyStoreDataCaptor.capture()
      )(Matchers.any())
      val keyStoreData: Map[String, String] = keyStoreDataCaptor.getAllValues.get(0)
      keyStoreData(qualifiedProfessionalBody) should be("")
      keyStoreData(qualifiedMembershipNumber) should be("")
    }

    "go to the next step when input data is entered" in new WithApplication(FakeApplication()) {
      val keyStoreDataCaptor = ArgumentCaptor.forClass(classOf[Map[String, String]])
      val result = controller.postProfessionalBodyMembershipAction(user, newRequestForProfessionalBodyMembership("charteredInstituteOfManagementAccountants", "data"))
      status(result) shouldBe 303
      headers(result)("Location") should be("/thank-you")
      verify(controller.keyStoreMicroService).addKeyStoreEntry(
        Matchers.eq(controller.registrationId(user)),
        Matchers.eq(controller.agent),
        Matchers.eq(professionalBodyMembershipFormName),
        keyStoreDataCaptor.capture()
      )(Matchers.any())
      val keyStoreData: Map[String, String] = keyStoreDataCaptor.getAllValues.get(0)
      keyStoreData(qualifiedProfessionalBody) should be("charteredInstituteOfManagementAccountants")
      keyStoreData(qualifiedMembershipNumber) should be("data")
    }
  }

  def newRequestForProfessionalBodyMembership(professionalBody: String, membershipNumber: String) =
    FakeRequest().withFormUrlEncodedBody(qualifiedProfessionalBody -> professionalBody, qualifiedMembershipNumber -> membershipNumber)
}