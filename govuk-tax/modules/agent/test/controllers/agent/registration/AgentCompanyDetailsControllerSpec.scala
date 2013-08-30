package controllers.agent.registration

import play.api.test.{ FakeRequest, WithApplication }
import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._
import uk.gov.hmrc.microservice.MockMicroServicesForTests
import play.api.test.Helpers._
import org.mockito.Matchers
import controllers.agent.registration.FormNames._
import uk.gov.hmrc.microservice.domain.User
import uk.gov.hmrc.microservice.domain.RegimeRoots
import uk.gov.hmrc.microservice.paye.domain.PayeRoot
import play.api.test.FakeApplication
import scala.Some

class AgentCompanyDetailsControllerSpec extends BaseSpec with MockitoSugar {

  val id = "wshakespeare"
  val authority = s"/auth/oid/$id"
  val uri = "/personal/paye/blah"

  val payeRoot = PayeRoot("CE927349E", 1, "Mr", "Will", None, "Shakespeare", "Will Shakespeare", "1983-01-02", Map(), Map())
  val user = User(id, null, RegimeRoots(Some(payeRoot), None, None), None, None)

  private val controller = new AgentCompanyDetailsController with MockMicroServicesForTests

  "The company details page" should {

    "not go to the next step if companyName is missing" in new WithApplication(FakeApplication()) {
      controller.resetAll
      val result = controller.postCompanyDetailsAction(user, newRequestForCompanyDetails(companyName = ""))
      status(result) shouldBe 400
      contentAsString(result) should include("This field is required")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }

    "not go to the next step if companyName is blank" in new WithApplication(FakeApplication()) {
      controller.resetAll()
      val result = controller.postCompanyDetailsAction(user, newRequestForCompanyDetails(companyName = "   "))
      status(result) shouldBe 400
      contentAsString(result) should include("This field is required")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }

    "not go to the next step if both phone numbers are missing" in new WithApplication(FakeApplication()) {
      controller.resetAll
      val result = controller.postCompanyDetailsAction(user, newRequestForCompanyDetails(landlineNumber = None, mobileNumber = None))
      status(result) shouldBe 400
      contentAsString(result) should include("You must either specify a landline or mobile phone number")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }

    "not go to the next step if landline number is invalid" in new WithApplication(FakeApplication()) {
      controller.resetAll
      val result = controller.postCompanyDetailsAction(user, newRequestForCompanyDetails(landlineNumber = Some("asdf")))
      status(result) shouldBe 400
      contentAsString(result) should include("Please enter a valid phone number")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }

    "not go to the next step if mobile number is invalid" in new WithApplication(FakeApplication()) {
      controller.resetAll
      val result = controller.postCompanyDetailsAction(user, newRequestForCompanyDetails(mobileNumber = Some("asdf")))
      status(result) shouldBe 400
      contentAsString(result) should include("Please enter a valid phone number")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }

    "not go to the next step if email is missing" in new WithApplication(FakeApplication()) {
      controller.resetAll
      val result = controller.postCompanyDetailsAction(user, newRequestForCompanyDetails(email = ""))
      status(result) shouldBe 400
      contentAsString(result) should include("Valid email required")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }

    "not go to the next step if email is invalid" in new WithApplication(FakeApplication()) {
      controller.resetAll
      val result = controller.postCompanyDetailsAction(user, newRequestForCompanyDetails(email = "kdhdhdhd"))
      status(result) shouldBe 400
      contentAsString(result) should include("Valid email required")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }

    "not go to the next step if main address is missing" in new WithApplication(FakeApplication()) {
      controller.resetAll
      val result = controller.postCompanyDetailsAction(user, newRequestForCompanyDetails(mainAddress = ""))
      status(result) shouldBe 400
      contentAsString(result) should include("This address line field must not be blank")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }

    "not go to the next step if main address is blank" in new WithApplication(FakeApplication()) {
      controller.resetAll
      val result = controller.postCompanyDetailsAction(user, newRequestForCompanyDetails(mainAddress = "   "))
      status(result) shouldBe 400
      contentAsString(result) should include("This address line field must not be blank")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }

    "not go to the next step if communication address is missing" in new WithApplication(FakeApplication()) {
      controller.resetAll
      val result = controller.postCompanyDetailsAction(user, newRequestForCompanyDetails(communicationAddress = ""))
      status(result) shouldBe 400
      contentAsString(result) should include("This address line field must not be blank")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }

    "not go to the next step if communication address is blank" in new WithApplication(FakeApplication()) {
      controller.resetAll
      val result = controller.postCompanyDetailsAction(user, newRequestForCompanyDetails(communicationAddress = "   "))
      status(result) shouldBe 400
      contentAsString(result) should include("This address line field must not be blank")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }

    "not go to the next step if business address is missing" in new WithApplication(FakeApplication()) {
      controller.resetAll
      val result = controller.postCompanyDetailsAction(user, newRequestForCompanyDetails(businessAddress = ""))
      status(result) shouldBe 400
      contentAsString(result) should include("This address line field must not be blank")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }

    "not go to the next step if business address is blank" in new WithApplication(FakeApplication()) {
      controller.resetAll
      val result = controller.postCompanyDetailsAction(user, newRequestForCompanyDetails(businessAddress = "   "))
      status(result) shouldBe 400
      contentAsString(result) should include("This address line field must not be blank")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }

    "not go to the next step if SA UTR is missing" in new WithApplication(FakeApplication()) {
      controller.resetAll
      val result = controller.postCompanyDetailsAction(user, newRequestForCompanyDetails(saUtr = ""))
      status(result) shouldBe 400
      contentAsString(result) should include("Please enter a valid SA UTR value")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }

    "not go to the next step if SA UTR is invalid" in new WithApplication(FakeApplication()) {
      controller.resetAll
      val result = controller.postCompanyDetailsAction(user, newRequestForCompanyDetails(saUtr = "hello"))
      status(result) shouldBe 400
      contentAsString(result) should include("Please enter a valid SA UTR value")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }

    "not go to the next step if not registered on HMRC" in new WithApplication(FakeApplication()) {
      controller.resetAll
      val result = controller.postCompanyDetailsAction(user, newRequestForCompanyDetails(registeredOnHMRC = false))
      status(result) shouldBe 400
      contentAsString(result) should include("You must be registered with HMRC to register as an agent")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }

    "go to next step if required details are provided" in new WithApplication(FakeApplication()) {
      controller.resetAll
      val result = controller.postCompanyDetailsAction(user, newRequestForCompanyDetails())
      status(result) shouldBe 303
      verify(controller.keyStoreMicroService).addKeyStoreEntry(Matchers.eq(s"Registration:$id"), Matchers.eq("agent"), Matchers.eq(companyDetailsFormName), any[Map[String, Any]]())
    }
  }

  def newRequestForCompanyDetails(companyName: String = "Alvaro Ltd", tradingName: Option[String] = Some("Alvarito"), landlineNumber: Option[String] = Some("1234"), mobileNumber: Option[String] = Some("5678"),
    website: Option[String] = Some("alvarito.com"), email: String = "alvaro@alvaro.com", mainAddress: String = "Alvaro's house", communicationAddress: String = "Alvaro's house in Murcia",
    businessAddress: String = "Alvaro's Company Location", saUtr: String = "7453627123", ctUtr: Option[String] = Some("ct"),
    vatVrn: Option[String] = Some("vatvrn"), payeEmpRef: Option[String] = Some("payemp"), companyHouseNumber: Option[String] = Some("764536"), registeredOnHMRC: Boolean = true) =
    FakeRequest().withFormUrlEncodedBody("companyName" -> companyName, "tradingName" -> tradingName.get, "phoneNumbers.landlineNumber" -> landlineNumber.getOrElse(""), "phoneNumbers.mobileNumber" -> mobileNumber.getOrElse(""), "website" -> website.get, "email" -> email,
      "mainAddress.addressLine1" -> mainAddress, "communicationAddress.addressLine1" -> communicationAddress, "businessAddress.addressLine1" -> businessAddress, "saUtr" -> saUtr, "ctUtr" -> ctUtr.get, "vaVrn" -> vatVrn.get, "payeEmpRef" -> payeEmpRef.get, "companyHouseNumber" -> companyHouseNumber.get, "registeredOnHMRC" -> registeredOnHMRC.toString
    )
}
