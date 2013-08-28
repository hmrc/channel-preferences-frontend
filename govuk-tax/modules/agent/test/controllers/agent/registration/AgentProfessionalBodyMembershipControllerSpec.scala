package controllers.agent.registration

import play.api.test.{ FakeRequest, FakeApplication, WithApplication }
import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.microservice.paye.PayeMicroService
import org.mockito.Matchers
import uk.gov.hmrc.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.microservice.domain.{ RegimeRoots, User }
import org.jsoup.Jsoup
import controllers.common.SessionTimeoutWrapper
import uk.gov.hmrc.microservice.auth.AuthMicroService
import uk.gov.hmrc.microservice.MockMicroServicesForTests
import org.mockito.Mockito._
import org.mockito.Matchers._
import uk.gov.hmrc.microservice.auth.domain.{ Regimes, UserAuthority }
import java.net.URI
import play.api.test.Helpers._
import uk.gov.hmrc.microservice.auth.domain.UserAuthority
import uk.gov.hmrc.microservice.auth.domain.Regimes
import uk.gov.hmrc.microservice.domain.User
import uk.gov.hmrc.microservice.domain.RegimeRoots
import uk.gov.hmrc.microservice.paye.domain.PayeRoot
import play.api.test.FakeApplication
import scala.Some
import uk.gov.hmrc.common.microservice.agent.{ AgentMicroService, Agent }
import uk.gov.hmrc.common.microservice.keystore.KeyStoreMicroService
import uk.gov.hmrc.common.microservice.keystore.KeyStore
import org.scalatest.{ BeforeAndAfterEach, BeforeAndAfter }

class AgentProfessionalBodyMembershipControllerSpec extends BaseSpec with MockitoSugar with BeforeAndAfterEach {

  val id = "wshakespeare"
  val authority = s"/auth/oid/$id"
  val uri = "/personal/paye/blah"

  val payeRoot = PayeRoot("CE927349E", 1, "Mr", "Will", None, "Shakespeare", "Will Shakespeare", "1983-01-02", Map(), Map())
  val user = User(id, null, RegimeRoots(Some(payeRoot), None, None), None, None)

  var mockAuthMicroService = mock[AuthMicroService]
  var mockPayeMicroService = mock[PayeMicroService]
  var mockAgentMicroService = mock[AgentMicroService]
  var mockAgent = mock[Agent]
  var mockKeyStoreMicroService = mock[KeyStoreMicroService]
  var mockKeyStore = mock[KeyStore]

  private def controller = new AgentProfessionalBodyMembershipController with MockMicroServicesForTests {
    override val authMicroService = mockAuthMicroService
    override val payeMicroService = mockPayeMicroService
    override val keyStoreMicroService = mockKeyStoreMicroService
    override val agentMicroService = mockAgentMicroService

    override def toAgent(implicit keyStore: KeyStore) = {
      mockAgent
    }
  }

  override def beforeEach() = {
    mockAuthMicroService = mock[AuthMicroService]
    mockPayeMicroService = mock[PayeMicroService]
    mockAgentMicroService = mock[AgentMicroService]
    mockAgent = mock[Agent]
    mockKeyStoreMicroService = mock[KeyStoreMicroService]
    mockKeyStore = mock[KeyStore]

    when(mockPayeMicroService.root(uri)).thenReturn(payeRoot)
    when(mockAuthMicroService.authority(anyString())).thenReturn(Some(UserAuthority(authority, Regimes(paye = Some(URI.create(uri))))))
    when(mockAgentMicroService.create(any[Agent])).thenReturn(Some(mockAgent))
    when(mockKeyStoreMicroService.getKeyStore(anyString(), anyString())).thenReturn(Some(mockKeyStore))
    when(mockAgent.uar).thenReturn(Some("12324"))
    doNothing().when(mockKeyStoreMicroService).deleteKeyStore(anyString(), anyString())
    when(mockKeyStore.get(anyString())).thenReturn(Some(Map.empty[String, String]))
  }

  "AgentProfessionalMembershipController" should {
    "not go to the next step if professional body is specified but not the membership number" in new WithApplication(FakeApplication()) {
      val result = controller.postProfessionalBodyMembership()(newRequestForProfessionalBodyMembership("charteredInstituteOfManagementAccountants", ""))
      status(result) shouldBe 400
      contentAsString(result) should include("You must specify a membership number for your professional body")
    }

    "not go to the next step if professional body is invalid" in new WithApplication(FakeApplication()) {
      val result = controller.postProfessionalBodyMembership()(newRequestForProfessionalBodyMembership("sad", ""))
      status(result) shouldBe 400
      contentAsString(result) should include("Please select a valid option")
    }

    "not go to the next step if membership number is specified but not the professional body" in new WithApplication(FakeApplication()) {
      val result = controller.postProfessionalBodyMembership()(newRequestForProfessionalBodyMembership("", "asdsafd"))
      status(result) shouldBe 400
      contentAsString(result) should include("You must specify which professional body you belong to")
    }

    "go to the next step if no input data is entered" in new WithApplication(FakeApplication()) {
      val result = controller.postProfessionalBodyMembership()(newRequestForProfessionalBodyMembership("", ""))
      status(result) shouldBe 200
      verify(mockAgentMicroService).create(mockAgent)
      verify(mockKeyStoreMicroService).deleteKeyStore("Registration:wshakespeare", "agent")
    }

    "go to the next step when input data is entered" in new WithApplication(FakeApplication()) {
      val result = controller.postProfessionalBodyMembership()(newRequestForProfessionalBodyMembership("charteredInstituteOfManagementAccountants", "data"))
      status(result) shouldBe 200
      verify(mockAgentMicroService).create(mockAgent)
      verify(mockKeyStoreMicroService).deleteKeyStore("Registration:wshakespeare", "agent")
    }
  }

  def newRequestForProfessionalBodyMembership(professionalBody: String, membershipNumber: String) =
    FakeRequest().withFormUrlEncodedBody("professionalBodyMembership.professionalBody" -> professionalBody, "professionalBodyMembership.membershipNumber" -> membershipNumber)
      .withSession("userId" -> controller.encrypt(authority), "name" -> controller.encrypt("Will Shakespeare"),
        SessionTimeoutWrapper.sessionTimestampKey -> controller.now().getMillis.toString)

}
