package controllers.agent

import play.api.test.{ FakeRequest, FakeApplication, WithApplication }
import uk.gov.hmrc.common.BaseSpec
import controllers.common.service.Encryption
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
import uk.gov.hmrc.microservice.auth.domain.{Regimes, UserAuthority}
import java.net.URI
import org.scalatest.BeforeAndAfterAll

class AgentControllerSpec extends BaseSpec with Encryption with MockitoSugar with BeforeAndAfterAll {

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

  private def controller = new AgentController with MockMicroServicesForTests {
    override val authMicroService = mockAuthMicroService
    override val payeMicroService = mockPayeMicroService
  }

  "The sro check page" should {
    "include two agreements" in new WithApplication(FakeApplication()) {
      val content = contentAsString(controller.sroCheck()(FakeRequest()))
      content should include("Yes, I am the Senior Responsible Officer")
      content should include("I accept the Terms &amp; Conditions")
    }

    "Return a BadRequest error if agreements are not checked" in new WithApplication(FakeApplication()) {

      val result = controller.submitAgreement()(newRequest("false", "false"))
      status(result) shouldBe 400
      contentAsString(result) should include("Please specify that you are the Senior Responsible Officer")
      contentAsString(result) should include("Please accept the terms and conditions")

    }

    "Return a Redirect if agreements are checked" in new WithApplication(FakeApplication()) {

      val result = controller.submitAgreement()(newRequest("true", "true"))

      status(result) shouldBe 303
      header("Location", result) shouldBe Some("/home")

    }
  }

  "The submit agreement page" should {
    "add a register agent entry in the session" in new WithApplication(FakeApplication()) {
      val result = controller.submitAgreement()(newRequest("true", "true"))

      session(result).data("register agent") should equal("true")
    }
  }

  "The contact details page" should {
    "display known agent info" in new WithApplication(FakeApplication()) {
      val result = new AgentController().contactDetailsFunction(user, null)

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#title").first().`val` should be("Mr")
      doc.select("#firstName").first().`val` should be("Will")
      doc.select("#middleName").first().`val` should be("")
      doc.select("#lastName").first().`val` should be("Shakespeare")
      doc.select("#nino").first().`val` should be("CE927349E")
      doc.select("#dateOfBirth").first().`val` should be("1983-01-02")
    }

    "not go to the next step if phone number is missing" in new WithApplication(FakeApplication()) {
      val result = controller.postContacts()(newRequestForContactDetails("", "07777777777", "email@company.com"))
      status(result) shouldBe 400
      contentAsString(result) should include("Please enter a valid phone number")
    }

    "not go to the next step if mobile number is missing" in new WithApplication(FakeApplication()) {
      val result = controller.postContacts()(newRequestForContactDetails("07777777777", "", "email@company.com"))
      status(result) shouldBe 400
      contentAsString(result) should include("Please enter a valid phone number")
    }

    "not go to the next step if phone number is not a number" in new WithApplication(FakeApplication()) {
      val result = controller.postContacts()(newRequestForContactDetails("a", "07777777777", "email@company.com"))
      status(result) shouldBe 400
      contentAsString(result) should include("Please enter a valid phone number")
    }

    "not go to the next step if mobile number is not a number" in new WithApplication(FakeApplication()) {
      val result = controller.postContacts()(newRequestForContactDetails("07777777777", "a", "email@company.com"))
      status(result) shouldBe 400
      contentAsString(result) should include("Please enter a valid phone number")
    }

    "not go to the next step if email address is missing" in new WithApplication(FakeApplication()) {
      val result = controller.postContacts()(newRequestForContactDetails("07777777777", "0777777777", ""))
      status(result) shouldBe 400
      contentAsString(result) should include("Valid email required")
    }

    "not go to the next step if email address is invalid" in new WithApplication(FakeApplication()) {
      val result = controller.postContacts()(newRequestForContactDetails("07777777777", "0777777777", "a@"))
      status(result) shouldBe 400
      contentAsString(result) should include("Valid email required")
    }
  }

  def newRequestForContactDetails(daytimePhoneNumber: String, mobilePhoneNumber: String, emailAddress: String) =
    FakeRequest().withFormUrlEncodedBody("daytimePhoneNumber" -> daytimePhoneNumber, "mobilePhoneNumber" -> mobilePhoneNumber, "emailAddress" -> emailAddress)
      .withSession("userId" -> controller.encrypt(authority), "name" -> controller.encrypt("Will Shakespeare"),
      SessionTimeoutWrapper.sessionTimestampKey -> controller.now().getMillis.toString)

  def newRequest(sro: String, tnc: String) =
    FakeRequest().withFormUrlEncodedBody("sroAgreement" -> sro, "tncAgreement" -> tnc)

  override val encryptionKey = "eU1qMlpESFRPN0hRNGJxNg=="
}
