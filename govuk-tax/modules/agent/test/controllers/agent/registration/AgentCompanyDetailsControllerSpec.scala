package controllers.agent.registration

import play.api.test.{ FakeRequest, FakeApplication, WithApplication }
import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import controllers.common.SessionTimeoutWrapper
import uk.gov.hmrc.microservice.MockMicroServicesForTests
import play.api.test.Helpers._

class AgentCompanyDetailsControllerSpec extends BaseSpec with MockitoSugar with MockAuthentication {

  private def controller = new AgentCompanyDetailsController with MockMicroServicesForTests {
    override val authMicroService = mockAuthMicroService
    override val payeMicroService = mockPayeMicroService
  }

  "The company details page" should {

    "not go to the next step if companyName is missing" in new WithApplication(FakeApplication()) {
      val result = controller.postCompanyDetails()(newRequestForCompanyDetails(companyName = ""))
      status(result) shouldBe 400
      contentAsString(result) should include("This field is required")
    }

    "not go to the next step if both phone numbers are missing" in new WithApplication(FakeApplication()) {
      val result = controller.postCompanyDetails()(newRequestForCompanyDetails(landlineNumber = None, mobileNumber = None))
      status(result) shouldBe 400
      contentAsString(result) should include("You must either specify a landline or mobile phone number")
    }

    "not go to the next step if landline number is invalid" in new WithApplication(FakeApplication()) {
      val result = controller.postCompanyDetails()(newRequestForCompanyDetails(landlineNumber = Some("asdf")))
      status(result) shouldBe 400
      contentAsString(result) should include("Please enter a valid phone number")
    }

    "not go to the next step if mobile number is invalid" in new WithApplication(FakeApplication()) {
      val result = controller.postCompanyDetails()(newRequestForCompanyDetails(mobileNumber = Some("asdf")))
      status(result) shouldBe 400
      contentAsString(result) should include("Please enter a valid phone number")
    }

    "not go to the next step if email is missing" in new WithApplication(FakeApplication()) {
      val result = controller.postCompanyDetails()(newRequestForCompanyDetails(email = ""))
      status(result) shouldBe 400
      contentAsString(result) should include("Valid email required")
    }

    "not go to the next step if email is invalid" in new WithApplication(FakeApplication()) {
      val result = controller.postCompanyDetails()(newRequestForCompanyDetails(email = "kdhdhdhd"))
      status(result) shouldBe 400
      contentAsString(result) should include("Valid email required")
    }

    "not go to the next step if main address is missing" in new WithApplication(FakeApplication()) {
      val result = controller.postCompanyDetails()(newRequestForCompanyDetails(mainAddress = ""))
      status(result) shouldBe 400
      contentAsString(result) should include("This field is required")
    }

    "not go to the next step if communication address is missing" in new WithApplication(FakeApplication()) {
      val result = controller.postCompanyDetails()(newRequestForCompanyDetails(communicationAddress = ""))
      status(result) shouldBe 400
      contentAsString(result) should include("This field is required")
    }

    "not go to the next step if business address is missing" in new WithApplication(FakeApplication()) {
      val result = controller.postCompanyDetails()(newRequestForCompanyDetails(businessAddress = ""))
      status(result) shouldBe 400
      contentAsString(result) should include("This field is required")
    }

    "not go to the next step if SA UTR is missing" in new WithApplication(FakeApplication()) {
      val result = controller.postCompanyDetails()(newRequestForCompanyDetails(saUtr = ""))
      status(result) shouldBe 400
      contentAsString(result) should include("Please enter a valid SA UTR value")
    }

    "not go to the next step if SA UTR is invalid" in new WithApplication(FakeApplication()) {
      val result = controller.postCompanyDetails()(newRequestForCompanyDetails(saUtr = "hello"))
      status(result) shouldBe 400
      contentAsString(result) should include("Please enter a valid SA UTR value")
    }

    "not go to the next step if not registered on HMRC" in new WithApplication(FakeApplication()) {
      val result = controller.postCompanyDetails()(newRequestForCompanyDetails(registeredOnHMRC = false))
      status(result) shouldBe 400
      contentAsString(result) should include("You must be registered with HMRC to register as an agent")
    }

    "go to next step if required details are provided" in new WithApplication(FakeApplication()) {
      val result = controller.postCompanyDetails()(newRequestForCompanyDetails())
      status(result) shouldBe 303
    }

  }

  def newRequestForCompanyDetails(companyName: String = "Alvaro Ltd", tradingName: Option[String] = Some("Alvarito"), landlineNumber: Option[String] = Some("1234"), mobileNumber: Option[String] = Some("5678"),
    website: Option[String] = Some("alvarito.com"), email: String = "alvaro@alvaro.com", mainAddress: String = "Alvaro's house", communicationAddress: String = "Alvaro's house in Murcia",
    businessAddress: String = "Alvaro's Company Location", saUtr: String = "7453627123", ctUtr: Option[String] = Some("ct"),
    vatVrn: Option[String] = Some("vatvrn"), payeEmpRef: Option[String] = Some("payemp"), companyHouseNumber: Option[String] = Some("764536"), registeredOnHMRC: Boolean = true) =
    FakeRequest().withFormUrlEncodedBody("companyName" -> companyName, "tradingName" -> tradingName.get, "phoneNumbers.landlineNumber" -> landlineNumber.getOrElse(""), "phoneNumbers.mobileNumber" -> mobileNumber.getOrElse(""), "website" -> website.get, "email" -> email,
      "mainAddress" -> mainAddress, "communicationAddress" -> communicationAddress, "businessAddress" -> businessAddress, "saUtr" -> saUtr, "ctUtr" -> ctUtr.get, "vaVrn" -> vatVrn.get, "payeEmpRef" -> payeEmpRef.get, "companyHouseNumber" -> companyHouseNumber.get, "registeredOnHMRC" -> registeredOnHMRC.toString
    )
      .withSession("userId" -> controller.encrypt(authority), "name" -> controller.encrypt("Will Shakespeare"),
        SessionTimeoutWrapper.sessionTimestampKey -> controller.now().getMillis.toString)

}
