package controllers.agent.registration

import play.api.test.{ FakeRequest, FakeApplication, WithApplication }
import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import org.jsoup.Jsoup
import controllers.common.SessionTimeoutWrapper
import uk.gov.hmrc.microservice.MockMicroServicesForTests
import play.api.test.Helpers._

class AgentContactDetailsControllerSpec extends BaseSpec with MockitoSugar with MockAuthentication {

  private def controller = new AgentContactDetailsController with MockMicroServicesForTests {
    override val authMicroService = mockAuthMicroService
    override val payeMicroService = mockPayeMicroService
  }

  "The contact details page" should {
    "display known agent info" in new WithApplication(FakeApplication()) {
      val result = controller.contactDetailsFunction(user, null)

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

}
