package controllers.agent.registration

import play.api.test.{ FakeRequest, WithApplication }
import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import org.jsoup.Jsoup
import play.api.test.Helpers._
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, Matchers}
import controllers.agent.registration.FormNames._
import controllers.agent.registration.AgentContactDetailsFormFields._
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import org.scalatest.TestData
import concurrent.Future
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import scala.Some
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import play.api.test.FakeApplication

class AgentContactDetailsControllerSpec extends BaseSpec with MockitoSugar {

  val id = "wshakespeare"
  val authority = s"/auth/oid/$id"
  val uri = "/personal/paye/blah"

  val payeRoot = PayeRoot("CE927349E", 1, "Mr", "Will", None, "Shakespeare", "Will Shakespeare", "1983-01-02", Map(), Map(), Map())
  val user = User(id, null, RegimeRoots(paye = Some(payeRoot)), None, None)

  val keyStoreConnector = mock[KeyStoreConnector]

  private val controller = new AgentContactDetailsController(null, keyStoreConnector)(null)

  override protected def beforeEach(testData: TestData): Unit = {
    reset(keyStoreConnector)
  }

  "The contact details page" should {
    "display known agent info" in new WithApplication(FakeApplication()) {
      val result = Future.successful(controller.contactDetailsAction(user, null))

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#title").first().`val` should be("Mr")
      doc.select("#firstName").first().`val` should be("Will")
      doc.select("#middleName").first().`val` should be("")
      doc.select("#lastName").first().`val` should be("Shakespeare")
      doc.select("#nino").first().`val` should be("CE927349E")
      doc.select("#dateOfBirth").first().`val` should be("1983-01-02")
    }

    "not go to the next step if phone number is missing" in new WithApplication(FakeApplication()) {
      val result = Future.successful(controller.postContactDetailsAction(user, newRequestForContactDetails("", "07777777777", "email@company.com")))
      status(result) shouldBe 400
      contentAsString(result) should include("Please enter a valid phone number")
      verifyZeroInteractions(keyStoreConnector)
    }

    "not go to the next step if mobile number is missing" in new WithApplication(FakeApplication()) {
      val result = Future.successful(controller.postContactDetailsAction(user, newRequestForContactDetails("07777777777", "", "email@company.com")))
      status(result) shouldBe 400
      contentAsString(result) should include("Please enter a valid phone number")
      verifyZeroInteractions(keyStoreConnector)
    }

    "not go to the next step if phone number is not a number" in new WithApplication(FakeApplication()) {
      val result = Future.successful(controller.postContactDetailsAction(user, newRequestForContactDetails("a", "07777777777", "email@company.com")))
      status(result) shouldBe 400
      contentAsString(result) should include("Please enter a valid phone number")
      verifyZeroInteractions(keyStoreConnector)
    }

    "not go to the next step if mobile number is not a number" in new WithApplication(FakeApplication()) {
      val result = Future.successful(controller.postContactDetailsAction(user, newRequestForContactDetails("07777777777", "a", "email@company.com")))
      status(result) shouldBe 400
      contentAsString(result) should include("Please enter a valid phone number")
      verifyZeroInteractions(keyStoreConnector)
    }

    "not go to the next step if email address is missing" in new WithApplication(FakeApplication()) {
      val result = Future.successful(controller.postContactDetailsAction(user, newRequestForContactDetails("07777777777", "0777777777", "")))
      status(result) shouldBe 400
      contentAsString(result) should include("Valid email required")
      verifyZeroInteractions(keyStoreConnector)
    }

    "not go to the next step if email address is invalid" in new WithApplication(FakeApplication()) {
      val result = Future.successful(controller.postContactDetailsAction(user, newRequestForContactDetails("07777777777", "0777777777", "a@")))
      status(result) shouldBe 400
      contentAsString(result) should include("Valid email required")
      verifyZeroInteractions(keyStoreConnector)
    }

    "go to the agent type page if valid email address and phone numbers are entered and store result in keystore, including the contact details" in new WithApplication(FakeApplication()) {
      val keyStoreDataCaptor = ArgumentCaptor.forClass(classOf[Map[String, String]])
      val result = Future.successful(controller.postContactDetailsAction(user, newRequestForContactDetails("07777777777", "0777777771", "a@a.a")))
      status(result) shouldBe 303
      headers(result).get("Location") should contain("/agent-type")
      verify(keyStoreConnector).addKeyStoreEntry(Matchers.eq(s"Registration:$id"), Matchers.eq("agent"), Matchers.eq(contactFormName), keyStoreDataCaptor.capture())(Matchers.any())
      val keyStoreData: Map[String, String] = keyStoreDataCaptor.getAllValues.get(0)
      keyStoreData(title) should be(payeRoot.title)
      keyStoreData(firstName) should be(payeRoot.firstName)
      keyStoreData(lastName) should be(payeRoot.surname)
      keyStoreData(dateOfBirth) should be(payeRoot.dateOfBirth)
      keyStoreData(nino) should be(payeRoot.nino)
      keyStoreData(daytimePhoneNumber) should be("07777777777")
      keyStoreData(mobilePhoneNumber) should be("0777777771")
      keyStoreData(emailAddress) should be("a@a.a")
    }
  }

  def newRequestForContactDetails(daytimePhoneNumberVal: String, mobilePhoneNumberVal: String, emailAddressVal: String) =
    FakeRequest().withFormUrlEncodedBody(daytimePhoneNumber -> daytimePhoneNumberVal, mobilePhoneNumber -> mobilePhoneNumberVal, emailAddress -> emailAddressVal)
}
