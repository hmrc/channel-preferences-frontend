package controllers.agent.registration

import play.api.test.{ FakeRequest, WithApplication }
import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import uk.gov.hmrc.common.microservice.MockMicroServicesForTests
import play.api.test.Helpers._
import org.mockito.{ ArgumentCaptor, Matchers }
import controllers.agent.registration.FormNames._
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import play.api.test.FakeApplication
import scala.Some
import controllers.agent.registration.AgentCompanyDetailsFormFields._
import controllers.common.validators.AddressFields._
import scala.util.Success

class AgentCompanyDetailsControllerSpec extends BaseSpec with MockitoSugar {

  val id = "wshakespeare"
  val authority = s"/auth/oid/$id"
  val uri = "/personal/paye/blah"

  val payeRoot = PayeRoot("CE927349E", 1, "Mr", "Will", None, "Shakespeare", "Will Shakespeare", "1983-01-02", Map(), Map(), Map())
  val user = User(id, null, RegimeRoots(Some(payeRoot), None, None, None, None), None, None)

  private val controller = new AgentCompanyDetailsController with MockMicroServicesForTests

  "The company details page" should {

    "not go to the next step if companyName is missing" in new WithApplication(FakeApplication()) {
      controller.resetAll()
      val result = controller.postCompanyDetailsAction(user, newRequestForCompanyDetails(companyNameVal = ""))
      status(result) shouldBe 400
      contentAsString(result) should include("This field is required")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }

    "not go to the next step if companyName is blank" in new WithApplication(FakeApplication()) {
      controller.resetAll()
      val result = controller.postCompanyDetailsAction(user, newRequestForCompanyDetails(companyNameVal = "   "))
      status(result) shouldBe 400
      contentAsString(result) should include("This field is required")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }

    "not go to the next step if both phone numbers are missing" in new WithApplication(FakeApplication()) {
      controller.resetAll()
      val result = controller.postCompanyDetailsAction(user, newRequestForCompanyDetails(landlineNumberVal = None, mobileNumberVal = None))
      status(result) shouldBe 400
      contentAsString(result) should include("You must either specify a landline or mobile phone number")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }

    "not go to the next step if landline number is invalid" in new WithApplication(FakeApplication()) {
      controller.resetAll()
      val result = controller.postCompanyDetailsAction(user, newRequestForCompanyDetails(landlineNumberVal = Some("asdf")))
      status(result) shouldBe 400
      contentAsString(result) should include("Please enter a valid phone number")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }

    "not go to the next step if mobile number is invalid" in new WithApplication(FakeApplication()) {
      controller.resetAll()
      val result = controller.postCompanyDetailsAction(user, newRequestForCompanyDetails(mobileNumberVal = Some("asdf")))
      status(result) shouldBe 400
      contentAsString(result) should include("Please enter a valid phone number")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }

    "not go to the next step if email is missing" in new WithApplication(FakeApplication()) {
      controller.resetAll()
      val result = controller.postCompanyDetailsAction(user, newRequestForCompanyDetails(emailVal = ""))
      status(result) shouldBe 400
      contentAsString(result) should include("Valid email required")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }

    "not go to the next step if email is invalid" in new WithApplication(FakeApplication()) {
      controller.resetAll()
      val result = controller.postCompanyDetailsAction(user, newRequestForCompanyDetails(emailVal = "kdhdhdhd"))
      status(result) shouldBe 400
      contentAsString(result) should include("Valid email required")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }

    "not go to the next step if main address is missing" in new WithApplication(FakeApplication()) {
      controller.resetAll()
      val result = controller.postCompanyDetailsAction(user, newRequestForCompanyDetails(mainAddressLine1Val = ""))
      status(result) shouldBe 400
      contentAsString(result) should include("This address line field must not be blank")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }

    "not go to the next step if main address postcode is incorrect" in new WithApplication(FakeApplication()) {
      controller.resetAll()
      val result = controller.postCompanyDetailsAction(user, newRequestForCompanyDetails(mainAddressPostcodeVal = "1234"))
      status(result) shouldBe 400
      contentAsString(result) should include("Postcode is incorrect")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }

    "not go to the next step if main address is blank" in new WithApplication(FakeApplication()) {
      controller.resetAll()
      val result = controller.postCompanyDetailsAction(user, newRequestForCompanyDetails(mainAddressLine1Val = "   "))
      status(result) shouldBe 400
      contentAsString(result) should include("This address line field must not be blank")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }

    "not go to the next step if communication address is missing" in new WithApplication(FakeApplication()) {
      controller.resetAll()
      val result = controller.postCompanyDetailsAction(user, newRequestForCompanyDetails(communicationAddressLine1Val = ""))
      status(result) shouldBe 400
      contentAsString(result) should include("This address line field must not be blank")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }

    "not go to the next step if communication address is blank" in new WithApplication(FakeApplication()) {
      controller.resetAll()
      val result = controller.postCompanyDetailsAction(user, newRequestForCompanyDetails(communicationAddressLine1Val = "   "))
      status(result) shouldBe 400
      contentAsString(result) should include("This address line field must not be blank")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }

    "not go to the next step if communication address postcode is incorrect" in new WithApplication(FakeApplication()) {
      controller.resetAll()
      val result = controller.postCompanyDetailsAction(user, newRequestForCompanyDetails(communicationAddressPostcodeVal = "1234"))
      status(result) shouldBe 400
      contentAsString(result) should include("Postcode is incorrect")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }

    "not go to the next step if business address is missing" in new WithApplication(FakeApplication()) {
      controller.resetAll()
      val result = controller.postCompanyDetailsAction(user, newRequestForCompanyDetails(businessAddressLine1Val = ""))
      status(result) shouldBe 400
      contentAsString(result) should include("This address line field must not be blank")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }

    "not go to the next step if business address is blank" in new WithApplication(FakeApplication()) {
      controller.resetAll()
      val result = controller.postCompanyDetailsAction(user, newRequestForCompanyDetails(businessAddressLine1Val = "   "))
      status(result) shouldBe 400
      contentAsString(result) should include("This address line field must not be blank")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }

    "not go to the next step if business address postcode is incorrect" in new WithApplication(FakeApplication()) {
      controller.resetAll()
      val result = controller.postCompanyDetailsAction(user, newRequestForCompanyDetails(businessAddressPostcodeVal = "1234"))
      status(result) shouldBe 400
      contentAsString(result) should include("Postcode is incorrect")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }

    "not go to the next step if SA UTR is missing" in new WithApplication(FakeApplication()) {
      controller.resetAll()
      val result = controller.postCompanyDetailsAction(user, newRequestForCompanyDetails(saUtrVal = ""))
      status(result) shouldBe 400
      contentAsString(result) should include("Please enter a valid SA UTR value")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }

    "not go to the next step if SA UTR is invalid" in new WithApplication(FakeApplication()) {
      controller.resetAll()
      val result = controller.postCompanyDetailsAction(user, newRequestForCompanyDetails(saUtrVal = "hello"))
      status(result) shouldBe 400
      contentAsString(result) should include("Please enter a valid SA UTR value")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }

    "not go to the next step if not registered on HMRC" in new WithApplication(FakeApplication()) {
      controller.resetAll()
      val result = controller.postCompanyDetailsAction(user, newRequestForCompanyDetails(registeredOnHMRCVal = false))
      status(result) shouldBe 400
      contentAsString(result) should include("You must be registered with HMRC to register as an agent")
      verifyZeroInteractions(controller.keyStoreMicroService)
    }

    "go to next step if required details are provided" in new WithApplication(FakeApplication()) {
      controller.resetAll()
      val keyStoreDataCaptor = ArgumentCaptor.forClass(classOf[Map[String, String]])
      val request = newRequestForCompanyDetails()
      val result = controller.postCompanyDetailsAction(user, request)
      status(result) shouldBe 303
      verify(controller.keyStoreMicroService).addKeyStoreEntry(
        Matchers.eq(controller.registrationId(user)),
        Matchers.eq(controller.agent),
        Matchers.eq(companyDetailsFormName),
        keyStoreDataCaptor.capture())(Matchers.any())
      val keyStoreData: Map[String, String] = keyStoreDataCaptor.getAllValues.get(0)
      keyStoreData(companyName) should be("Alvaro Ltd")
      keyStoreData(tradingName) should be("Alvarito")
      keyStoreData(qualifiedLandlineNumber) should be("1234")
      keyStoreData(qualifiedMobileNumber) should be("5678")
      keyStoreData(website) should be("alvarito.com")
      keyStoreData(email) should be("alvaro@alvaro.com")
      keyStoreData(mainAddress + '.' + addressLine1) should be("Main line 1")
      keyStoreData(mainAddress + '.' + addressLine2) should be("Main line 2")
      keyStoreData(mainAddress + '.' + addressLine3) should be("Main line 3")
      keyStoreData(mainAddress + '.' + addressLine4) should be("Main line 4")
      keyStoreData(mainAddress + '.' + postcode) should be("E33BA")
      keyStoreData(communicationAddress + '.' + addressLine1) should be("Comm line 1")
      keyStoreData(communicationAddress + '.' + addressLine2) should be("Comm line 2")
      keyStoreData(communicationAddress + '.' + addressLine3) should be("Comm line 3")
      keyStoreData(communicationAddress + '.' + addressLine4) should be("Comm line 4")
      keyStoreData(communicationAddress + '.' + postcode) should be("E33BB")
      keyStoreData(businessAddress + '.' + addressLine1) should be("Company line 1")
      keyStoreData(businessAddress + '.' + addressLine2) should be("Company line 2")
      keyStoreData(businessAddress + '.' + addressLine3) should be("Company line 3")
      keyStoreData(businessAddress + '.' + addressLine4) should be("Company line 4")
      keyStoreData(businessAddress + '.' + postcode) should be("E33BC")
      keyStoreData(saUtr) should be("7453627123")
      keyStoreData(ctUtr) should be("ctValue")
      keyStoreData(vatVrn) should be("vatvrnValue")
      keyStoreData(payeEmpRef) should be("payempValue")
      keyStoreData(companyHouseNumber) should be("764536")
      keyStoreData(registeredOnHMRC) should be("true")
    }
  }

  def newRequestForCompanyDetails(
    companyNameVal: String = "Alvaro Ltd",
    tradingNameVal: Option[String] = Some("Alvarito"),
    landlineNumberVal: Option[String] = Some("1234"),
    mobileNumberVal: Option[String] = Some("5678"),
    websiteVal: Option[String] = Some("alvarito.com"),
    emailVal: String = "alvaro@alvaro.com",
    mainAddressLine1Val: String = "Main line 1",
    mainAddressLine2Val: String = "Main line 2",
    mainAddressLine3Val: String = "Main line 3",
    mainAddressLine4Val: String = "Main line 4",
    mainAddressPostcodeVal: String = "E33BA",
    communicationAddressLine1Val: String = "Comm line 1",
    communicationAddressLine2Val: String = "Comm line 2",
    communicationAddressLine3Val: String = "Comm line 3",
    communicationAddressLine4Val: String = "Comm line 4",
    communicationAddressPostcodeVal: String = "E33BB",
    businessAddressLine1Val: String = "Company line 1",
    businessAddressLine2Val: String = "Company line 2",
    businessAddressLine3Val: String = "Company line 3",
    businessAddressLine4Val: String = "Company line 4",
    businessAddressPostcodeVal: String = "E33BC",
    saUtrVal: String = "7453627123",
    ctUtrVal: Option[String] = Some("ctValue"),
    vatVrnVal: Option[String] = Some("vatvrnValue"),
    payeEmpRefVal: Option[String] = Some("payempValue"),
    companyHouseNumberVal: Option[String] = Some("764536"),
    registeredOnHMRCVal: Boolean = true) =
    FakeRequest().withFormUrlEncodedBody(
      companyName -> companyNameVal,
      tradingName -> tradingNameVal.get,
      qualifiedLandlineNumber -> landlineNumberVal.getOrElse(""),
      qualifiedMobileNumber -> mobileNumberVal.getOrElse(""),
      website -> websiteVal.get,
      email -> emailVal,
      mainAddress + '.' + addressLine1 -> mainAddressLine1Val,
      mainAddress + '.' + addressLine2 -> mainAddressLine2Val,
      mainAddress + '.' + addressLine3 -> mainAddressLine3Val,
      mainAddress + '.' + addressLine4 -> mainAddressLine4Val,
      mainAddress + '.' + postcode -> mainAddressPostcodeVal,
      communicationAddress + '.' + addressLine1 -> communicationAddressLine1Val,
      communicationAddress + '.' + addressLine2 -> communicationAddressLine2Val,
      communicationAddress + '.' + addressLine3 -> communicationAddressLine3Val,
      communicationAddress + '.' + addressLine4 -> communicationAddressLine4Val,
      communicationAddress + '.' + postcode -> communicationAddressPostcodeVal,
      businessAddress + '.' + addressLine1 -> businessAddressLine1Val,
      businessAddress + '.' + addressLine2 -> businessAddressLine2Val,
      businessAddress + '.' + addressLine3 -> businessAddressLine3Val,
      businessAddress + '.' + addressLine4 -> businessAddressLine4Val,
      businessAddress + '.' + postcode -> businessAddressPostcodeVal,
      saUtr -> saUtrVal,
      ctUtr -> ctUtrVal.get,
      vatVrn -> vatVrnVal.get,
      payeEmpRef -> payeEmpRefVal.get,
      companyHouseNumber -> companyHouseNumberVal.get,
      registeredOnHMRC -> registeredOnHMRCVal.toString
    )
}
