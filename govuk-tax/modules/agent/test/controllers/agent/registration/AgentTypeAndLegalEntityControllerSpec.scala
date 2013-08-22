package controllers.agent.registration

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.microservice.auth.AuthMicroService
import uk.gov.hmrc.microservice.paye.PayeMicroService
import org.mockito.Mockito._
import org.mockito.Matchers
import java.net.URI
import uk.gov.hmrc.microservice.MockMicroServicesForTests
import play.api.test.{FakeRequest, FakeApplication, WithApplication}
import uk.gov.hmrc.microservice.auth.domain.UserAuthority
import uk.gov.hmrc.microservice.auth.domain.Regimes
import uk.gov.hmrc.microservice.domain.User
import uk.gov.hmrc.microservice.domain.RegimeRoots
import uk.gov.hmrc.microservice.paye.domain.PayeRoot
import scala.Some
import controllers.common.SessionTimeoutWrapper


class AgentTypeAndLegalEntityControllerSpec extends BaseSpec with MockitoSugar {

  import play.api.test.Helpers._

  val mockAuthMicroService = mock[AuthMicroService]
  val mockPayeMicroService = mock[PayeMicroService]

  val id = "wshakespeare"
  val authority = s"/auth/oid/$id"
  val uri = "/personal/paye/blah"

  val payeRoot = PayeRoot("CE927349E", 1, "Mr", "Will", None, "Shakespeare", "Will Shakespeare", "1983-01-02", Map(), Map())
  val user = User(id, null, RegimeRoots(Some(payeRoot), None, None), None, None)

  when(mockPayeMicroService.root(uri)).thenReturn(payeRoot)
  when(mockAuthMicroService.authority(Matchers.anyString())).thenReturn(Some(UserAuthority(authority, Regimes(paye = Some(URI.create(uri))))))

  private def controller = new AgentTypeAndLegalEntityController with MockMicroServicesForTests {
    override val authMicroService = mockAuthMicroService
    override val payeMicroService = mockPayeMicroService
  }

  "The agent type and legal entity" should {
    "not go to the next step if no agent type is chosen" in new WithApplication(FakeApplication()) {
      val result = controller.postDetails()(newRequest("", "employer"))
      status(result) shouldBe 400
      contentAsString(result) should include("This field is required")
    }

    "not go to the next step if no legal entity is chosen" in new WithApplication(FakeApplication()) {
      val result = controller.postDetails()(newRequest("inBusiness", ""))
      status(result) shouldBe 400
      contentAsString(result) should include("This field is required")
    }
    "go to the next step if all items are chosen" in new WithApplication(FakeApplication()) {
      val result = controller.postDetails()(newRequest("inBusiness", "employer"))
      status(result) shouldBe 303
    }
    "not go to the next step if an illegal legal entity is chosen" in new WithApplication(FakeApplication()) {
      val result = controller.postDetails()(newRequest("inBusiness", "invalid"))
      status(result) shouldBe 400
      contentAsString(result) should include("Please select a valid option")
    }
    "not go to the next step if an illegal agent type is chosen" in new WithApplication(FakeApplication()) {
      val result = controller.postDetails()(newRequest("aslkjddhjks", "employer"))
      status(result) shouldBe 400
      contentAsString(result) should include("Please select a valid option")
    }
  }

  def newRequest(agentType: String, legalEntity: String) =
    FakeRequest().withFormUrlEncodedBody("agentType" -> agentType, "legalEntity" -> legalEntity)
      .withSession("userId" -> controller.encrypt(authority), "name" -> controller.encrypt("Will Shakespeare"),
      SessionTimeoutWrapper.sessionTimestampKey -> controller.now().getMillis.toString)

}
