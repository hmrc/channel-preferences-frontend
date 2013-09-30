package controllers.sa

import play.api.test._
import uk.gov.hmrc.common.microservice.MockMicroServicesForTests
import play.api.mvc.{ Result, Request }
import org.joda.time.{ DateTimeZone, DateTime }
import java.net.{ URLDecoder, URI }
import controllers.common.SessionTimeoutWrapper._
import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import controllers.common.CookieEncryption
import uk.gov.hmrc.common.microservice.sa.domain._
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import uk.gov.hmrc.common.microservice.sa.domain.SaRoot
import uk.gov.hmrc.common.microservice.sa.domain.SaIndividualAddress
import scala.Some
import uk.gov.hmrc.common.microservice.auth.domain.Regimes
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.sa.domain.SaPerson
import play.api.test.FakeApplication
import uk.gov.hmrc.common.microservice.sa.domain.TransactionId
import uk.gov.hmrc.common.microservice.sa.domain.write.SaAddressForUpdate
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import uk.gov.hmrc.domain.SaUtr
import java.util.UUID

class SaControllerSpec extends BaseSpec with MockitoSugar with CookieEncryption {

  import play.api.test.Helpers._

  private val currentTime = new DateTime(2012, 12, 21, 12, 4, 32, DateTimeZone.UTC)

  private lazy val controller = new SaController with MockMicroServicesForTests {
    override def now = () => currentTime
  }

  private def setupUser(id: String, saUtr: String, name: String, nameFromGovernmentGateway: String): User = {
    val ua = UserAuthority(s"/personal/$saUtr", Regimes(sa = Some(URI.create(s"/personal/utr/$saUtr"))), None, Some(SaUtr("123456789012")))

    val saRoot = SaRoot(
      utr = "123456789012",
      links = Map(
        "individual/details" -> "/sa/individual/123456789012/details",
        "individual/details/main-address" -> "/sa/individual/123456789012/details/main-address")
    )

    User(id, ua, RegimeRoots(None, Some(saRoot), None, None, None), Some(nameFromGovernmentGateway), None)
  }

  private val nameFromSa = SaName("", "", None, "", Some(""))
  private val nameFromGovernmentGateway = ""

  val geoffFisher = setupUser("/auth/oid/gfisher", "123456789012", "Geoff Fisher", nameFromGovernmentGateway)

  "The details page" should {
    "show the individual SA address of Geoff Fisher" in new WithApplication(FakeApplication()) {
      controller.resetAll()

      when(controller.saConnector.person("/sa/individual/123456789012/details")).thenReturn(
        Some(SaPerson(
          name = nameFromSa,
          utr = "123456789012",
          address = SaIndividualAddress(
            addressLine1 = "address line 1",
            addressLine2 = "address line 2",
            addressLine3 = Some("address line 3"),
            addressLine4 = Some("address line 4"),
            addressLine5 = Some("address line 5"),
            postcode = Some("postcode"),
            foreignCountry = Some("foreign country"),
            additionalDeliveryInformation = Some("additional delivery information")
          )
        ))
      )

      val content = request(geoffFisher, controller.detailsAction)

      content should include(nameFromSa.forename)
      content should include(nameFromSa.surname)
      content should include(nameFromGovernmentGateway)
      content should include("address line 1")
      content should include("address line 2")
      content should include("address line 3")
      content should include("address line 4")
      content should include("address line 5")
      content should include("postcode")
      content should include("Change this address")
    }

    "display an error page if personal details do not come back from backend service" in new WithApplication(FakeApplication()) {
      controller.resetAll()

      when(controller.saConnector.person("/sa/individual/123456789012/details")).thenReturn(None)
      val result = controller.detailsAction(geoffFisher, FakeRequest().withSession("sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"), "userId" -> encrypt("/auth/oid/gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt("<governmentGatewayToken/>"), lastRequestTimestampKey -> controller.now().getMillis.toString))
      status(result) should be(404)
    }
  }

  "Change Address Page " should {

    "render a form with address fields to be entered when a user is logged in and authorised for SA" in new WithApplication(FakeApplication()) {
      controller.resetAll()

      val result = controller.changeAddressAction(geoffFisher, FakeRequest("GET", "/prefs?rd=redirest_url").withFormUrlEncodedBody("email" -> "someuser@test.com"))

      status(result) should be(200)

      val htmlBody = contentAsString(result)
      htmlBody should include("addressLine1")
      htmlBody should include("addressLine2")
      htmlBody should include("addressLine3")
      htmlBody should include("addressLine4")
      htmlBody should not include "addressLine5"
      htmlBody should include("postcode")
      htmlBody should not include "country"
      htmlBody should include("additionalDeliveryInformation")
    }
  }

  "Submit Change Address Page with Postcode" should {
    val expectedInvalidCharacterErrorMessageForPostcode = """This line contains an invalid character.  Valid characters are: A-Z a-z 0-9 space"""
    val missingErrorMessageForPostcode = """Postcode is required"""
    val invalidMessageForPostcode = """Postcode is incorrect"""

    "show the postcode error message if the postcode field is missing" in new WithApplication(FakeApplication()) {
      controller.resetAll()

      val result = controller.submitChangeAddressAction(geoffFisher, FakeRequest()
        .withFormUrlEncodedBody("postcode" -> "", "addressLine1" -> "ABCDEFGHIJKLMNOPQRSTUVWXYZab", "addressLine2" -> "cdefghijklmnopqrstuvwxyz0123"))

      status(result) shouldBe 400
      val changeAddressSource = contentAsString(result)
      println(changeAddressSource)
      changeAddressSource should include(missingErrorMessageForPostcode)
    }

    "show the postcode error message if the postcode field is blank" in new WithApplication(FakeApplication()) {
      controller.resetAll()

      val result = controller.submitChangeAddressAction(geoffFisher, FakeRequest()
        .withFormUrlEncodedBody("postcode" -> "    ", "addressLine1" -> "ABCDEFGHIJKLMNOPQRSTUVWXYZab", "addressLine2" -> "cdefghijklmnopqrstuvwxyz0123"))

      status(result) shouldBe 400
      val changeAddressSource = contentAsString(result)
      println(changeAddressSource)
      changeAddressSource should include(missingErrorMessageForPostcode)
    }

    "show the postcode error message if it contains an invalid character" in new WithApplication(FakeApplication()) {
      controller.resetAll()

      val result = controller.submitChangeAddressAction(geoffFisher, FakeRequest()
        .withFormUrlEncodedBody("postcode" -> "Â^GYaaa", "addressLine1" -> "ABCDEFGHIJKLMNOPQRSTUVWXYZab", "addressLine2" -> "cdefghijklmnopqrstuvwxyz0123"))

      status(result) shouldBe 400
      val changeAddressSource = contentAsString(result)
      println(changeAddressSource)
      changeAddressSource should include(expectedInvalidCharacterErrorMessageForPostcode)
    }

    "show the postcode error message if it contains an invalid character that is accepted in address" in new WithApplication(FakeApplication()) {
      controller.resetAll()

      val result = controller.submitChangeAddressAction(geoffFisher, FakeRequest()
        .withFormUrlEncodedBody("postcode" -> "sw, 45-", "addressLine1" -> "ABCDEFGHIJKLMNOPQRSTUVWXYZab", "addressLine2" -> "cdefghijklmnopqrstuvwxyz0123"))

      status(result) shouldBe 400
      val changeAddressSource = contentAsString(result)
      println(changeAddressSource)
      changeAddressSource should include(expectedInvalidCharacterErrorMessageForPostcode)
    }

    "show the postcode error message if it contains more than 7 characters (excluding space)" in new WithApplication(FakeApplication()) {
      controller.resetAll()

      val result = controller.submitChangeAddressAction(geoffFisher, FakeRequest()
        .withFormUrlEncodedBody("postcode" -> "ABC 12345", "addressLine1" -> "ABCDEFGHIJKLMNOPQRSTUVWXYZab", "addressLine2" -> "cdefghijklmnopqrstuvwxyz0123"))

      status(result) shouldBe 400
      val changeAddressSource = contentAsString(result)
      println(changeAddressSource)
      changeAddressSource should include(invalidMessageForPostcode)
    }

    "accept a valid postcode with blank spaces" in new WithApplication(FakeApplication()) {
      controller.resetAll()

      val result = controller.submitChangeAddressAction(geoffFisher, FakeRequest()
        .withFormUrlEncodedBody("postcode" -> " SW95  8UT ", "addressLine1" -> "ABCDEFGHIJKLMNOPQRSTUVWXYZab", "addressLine2" -> "cdefghijklmnopqrstuvwxyz0123"))

      status(result) shouldBe 200
    }

    "accept a valid postcode of minimum length" in new WithApplication(FakeApplication()) {
      controller.resetAll()

      val result = controller.submitChangeAddressAction(geoffFisher, FakeRequest()
        .withFormUrlEncodedBody("postcode" -> "SW958", "addressLine1" -> "ABCDEFGHIJKLMNOPQRSTUVWXYZab", "addressLine2" -> "cdefghijklmnopqrstuvwxyz0123"))

      status(result) shouldBe 200
    }

    "show the postcode error message when length below 5" in new WithApplication(FakeApplication()) {
      controller.resetAll()

      val result = controller.submitChangeAddressAction(geoffFisher, FakeRequest()
        .withFormUrlEncodedBody("postcode" -> " S  8UT ", "addressLine1" -> "ABCDEFGHIJKLMNOPQRSTUVWXYZab", "addressLine2" -> "cdefghijklmnopqrstuvwxyz0123"))

      status(result) shouldBe 400
      val changeAddressSource = contentAsString(result)
      println(changeAddressSource)
      changeAddressSource should include(invalidMessageForPostcode)
    }
  }

  val expectedInvalidCharacterErrorMessageForAddress = """This line contains an invalid character.  Valid characters are: A-Z a-z 0-9 -  , / &amp; space"""

  "Submit Change Address Page " should {
    "show the address line 1 error message if it is missing" in new WithApplication(FakeApplication()) {
      controller.resetAll()

      val result = controller.submitChangeAddressAction(geoffFisher, FakeRequest().withFormUrlEncodedBody("addressLine1" -> "", "addressLine2" -> "addressline2data"))

      status(result) shouldBe 400
      val changeAddressSource = contentAsString(result)
      println(changeAddressSource)
      changeAddressSource should include("Address Line 1 is required")
    }

    "show the address line 1 error message if the data is greater than 28 characters" in new WithApplication(FakeApplication()) {
      controller.resetAll()

      val result = controller.submitChangeAddressAction(geoffFisher,
        FakeRequest().withFormUrlEncodedBody("addressLine1" -> "12345678901234567890123456789", "addressLine2" -> "addressline2data"))

      status(result) shouldBe 400
      val changeAddressSource = contentAsString(result)
      println(changeAddressSource)
      changeAddressSource should include("This address line field must not be greater than 28 characters")
    }

    "show the address line 2 error message if the data is greater than 28 characters" in new WithApplication(FakeApplication()) {
      controller.resetAll()

      val result = controller.submitChangeAddressAction(geoffFisher, FakeRequest()
        .withFormUrlEncodedBody("addressLine1" -> "addressline1data", "addressLine2" -> "12345678901234567890123456789"))

      status(result) shouldBe 400
      val changeAddressSource = contentAsString(result)
      println(changeAddressSource)
      changeAddressSource should include("This address line field must not be greater than 28 characters")
    }

    "show the address line 3 error message if the data is greater than 18 characters" in new WithApplication(FakeApplication()) {
      controller.resetAll()

      val result = controller.submitChangeAddressAction(geoffFisher, FakeRequest()
        .withFormUrlEncodedBody("addressLine1" -> "addressline1data", "addressLine2" -> "addressline2data", "optionalAddressLines.addressLine3" -> "1234567890123456789"))

      status(result) shouldBe 400
      val changeAddressSource = contentAsString(result)
      println(changeAddressSource)
      changeAddressSource should include("This address line field must not be greater than 18 characters")
    }

    "show the address line 4 error message if the data is greater than 18 characters" in new WithApplication(FakeApplication()) {
      controller.resetAll()

      val result = controller.submitChangeAddressAction(geoffFisher, FakeRequest()
        .withFormUrlEncodedBody("addressLine1" -> "addressline1data", "addressLine2" -> "addressline2data", "optionalAddressLines.addressLine3" -> "addressline3data", "optionalAddressLines.addressLine4" -> "1234567890123456789"))

      status(result) shouldBe 400
      val changeAddressSource = contentAsString(result)
      println(changeAddressSource)
      changeAddressSource should include("This address line field must not be greater than 18 characters")
    }

    "show the address line 2 error message if it is missing" in new WithApplication(FakeApplication()) {
      controller.resetAll()

      val result = controller.submitChangeAddressAction(geoffFisher, FakeRequest()
        .withFormUrlEncodedBody("addressLine2" -> "", "addressLine1" -> "addressline1data"))

      status(result) shouldBe 400
      val changeAddressSource = contentAsString(result)
      println(changeAddressSource)
      changeAddressSource should include("Address Line 2 is required")
    }

    "show the address line 3 error message when address line 4 is present" in new WithApplication(FakeApplication()) {
      controller.resetAll()

      val result = controller.submitChangeAddressAction(geoffFisher, FakeRequest()
        .withFormUrlEncodedBody("addressLine1" -> "addressline1data", "addressLine2" -> "addressline2data", "optionalAddressLines.addressLine4" -> "addressline4data"))

      status(result) shouldBe 400
      val changeAddressSource = contentAsString(result)
      println(changeAddressSource)
      changeAddressSource should include("Address Line 3 is required when using Address Line 4")
    }

    "show the address line 1 error message if it contains an invalid character" in new WithApplication(FakeApplication()) {
      controller.resetAll()

      val result = controller.submitChangeAddressAction(geoffFisher, FakeRequest()
        .withFormUrlEncodedBody("addressLine1" -> "address_Line1BadData", "addressLine2" -> "addressLine2Data"))

      status(result) shouldBe 400
      val changeAddressSource = contentAsString(result)
      println(changeAddressSource)
      changeAddressSource should include(expectedInvalidCharacterErrorMessageForAddress)
    }

    "show the address line 2 error message if it contains an invalid character" in new WithApplication(FakeApplication()) {
      controller.resetAll()

      val result = controller.submitChangeAddressAction(geoffFisher, FakeRequest()
        .withFormUrlEncodedBody("addressLine1" -> "addressLine1Data", "addressLine2" -> "addressLine2|BadData"))

      status(result) shouldBe 400
      val changeAddressSource = contentAsString(result)
      println(changeAddressSource)
      changeAddressSource should include(expectedInvalidCharacterErrorMessageForAddress)
    }

    "show the address line 3 error message if it contains an invalid character" in new WithApplication(FakeApplication()) {
      controller.resetAll()

      val result = controller.submitChangeAddressAction(geoffFisher, FakeRequest()
        .withFormUrlEncodedBody("addressLine1" -> "addressLine1Data", "addressLine2" -> "addressLine2Data", "optionalAddressLines.addressLine3" -> "addressLine4~Bad"))

      status(result) shouldBe 400
      val changeAddressSource = contentAsString(result)
      println(changeAddressSource)
      changeAddressSource should include(expectedInvalidCharacterErrorMessageForAddress)
    }

    "show the address line 4 error message if it contains an invalid character" in new WithApplication(FakeApplication()) {
      controller.resetAll()

      val result = controller.submitChangeAddressAction(geoffFisher, FakeRequest()
        .withFormUrlEncodedBody("addressLine1" -> "addressLine1Data", "addressLine2" -> "addressLine2Data",
          "optionalAddressLines.addressLine3" -> "addressLine3Data", "optionalAddressLines.addressLine4" -> "addressLine4!Bad"))

      status(result) shouldBe 400
      val changeAddressSource = contentAsString(result)
      println(changeAddressSource)
      changeAddressSource should include(expectedInvalidCharacterErrorMessageForAddress)
    }

    "allow all valid characters in address lines" in new WithApplication(FakeApplication()) {

      controller.resetAll()

      val result = controller.submitChangeAddressAction(geoffFisher, FakeRequest()
        .withFormUrlEncodedBody("addressLine1" -> "ABCDEFGHIJKLMNOPQRSTUVWXYZab", "addressLine2" -> "cdefghijklmnopqrstuvwxyz0123",
          "optionalAddressLines.addressLine3" -> "4567890 ,/&'-", "optionalAddressLines.addressLine4" -> "all valid", "postcode" -> "N1 9BA"))

      status(result) shouldBe 200
    }

    "take the user to a confirmation page that displays the form values entered" in new WithApplication(FakeApplication()) {
      controller.resetAll()

      val addressData1 = "ad1"
      val addressData2 = "ad2"
      val addressData3 = "ad3"
      val addressData4 = "ad4"
      val postcode = "XX1 0YY"
      val additionalDeliveryInformation = "someAdditionalDeliveryInformation"

      val result = controller.submitChangeAddressAction(geoffFisher,
        FakeRequest().withFormUrlEncodedBody(
          "addressLine1" -> addressData1, "addressLine2" -> addressData2, "optionalAddressLines.addressLine3" -> addressData3,
          "optionalAddressLines.addressLine4" -> addressData4, "postcode" -> postcode,
          "additionalDeliveryInformation" -> additionalDeliveryInformation))

      status(result) should be(200)

      // Assert displaying of values
      val htmlBody = contentAsString(result)
      htmlBody should include(addressData1)
      htmlBody should include(addressData2)
      htmlBody should include(addressData3)
      htmlBody should include(addressData4)
      htmlBody should include(postcode)
      htmlBody should include(additionalDeliveryInformation)

      // Assert hidden form
      htmlBody should include("""form action="/confirmChangeAddress" method="POST"""")
      htmlBody should include(s"""input type="hidden" name="additionalDeliveryInformation" id="additionalDeliveryInformation" value="$additionalDeliveryInformation" """)
      htmlBody should include(s"""input type="hidden" name="addressLine1" id="addressLine1" value="$addressData1" """)
      htmlBody should include(s"""input type="hidden" name="addressLine2" id="addressLine2" value="$addressData2" """)
      htmlBody should include(s"""input type="hidden" name="optionalAddressLines.addressLine3" id="optionalAddressLines_addressLine3" value="$addressData3" """)
      htmlBody should include(s"""input type="hidden" name="optionalAddressLines.addressLine4" id="optionalAddressLines_addressLine4" value="$addressData4" """)
      htmlBody should include(s"""input type="hidden" name="postcode" id="postcode" value="$postcode" """)

      htmlBody should not include "addressLine5"
      htmlBody should not include "country"
    }
  }

  "Submit Change Address Confirmation Page" should {
    // TODO: Post payload validation tests
    "use the post payload to submit the changed address to the SA service" in new WithApplication(FakeApplication()) {
      controller.resetAll()

      val add1 = "add1"
      val add2 = "add2"
      val utr = SaUtr("123456789012")
      val uri = s"/sa/individual/$utr/details/main-address"

      val postcodeValid = "ABC 123"

      val transactionId = "sometransactionid"

      val addressForUpdate = SaAddressForUpdate(addressLine1 = add1, addressLine2 = add2, addressLine3 = None, addressLine4 = None, postcode = Some(postcodeValid), additionalDeliveryInformation = None)

      when(controller.saConnector.updateMainAddress(uri, addressForUpdate)).thenReturn(Right(TransactionId(transactionId)))

      val result = controller.confirmChangeAddressAction(geoffFisher, FakeRequest()
        .withFormUrlEncodedBody("postcode" -> postcodeValid, "addressLine1" -> add1, "addressLine2" -> add2))

      val encodedTransactionId = SecureParameter(transactionId, currentTime).encrypt

      status(result) shouldBe 303
      redirectLocation(result).map(URLDecoder.decode(_, "UTF-8")) shouldBe Some(s"/changeAddressComplete?id=$encodedTransactionId")

      verify(controller.saConnector).updateMainAddress(uri, addressForUpdate)
    }

    "redirect to the change address failed page if the address cannot be updated" in new WithApplication(FakeApplication()) {
      controller.resetAll()

      val add1 = "add1"
      val add2 = "add2"
      val utr = SaUtr("123456789012")
      val uri = s"/sa/individual/$utr/details/main-address"
      val postcodeValid = "ABC 123"

      val transactionId = "sometransactionid"

      val addressForUpdate = SaAddressForUpdate(addressLine1 = add1, addressLine2 = add2, addressLine3 = None, addressLine4 = None, postcode = Some(postcodeValid), additionalDeliveryInformation = None)

      val errorMessage = "some error occurred"
      when(controller.saConnector.updateMainAddress(uri, addressForUpdate)).thenReturn(Left(errorMessage))

      val result = controller.confirmChangeAddressAction(geoffFisher, FakeRequest()
        .withFormUrlEncodedBody("postcode" -> postcodeValid, "addressLine1" -> add1, "addressLine2" -> add2))

      val encodedErrorMessage = SecureParameter(errorMessage, currentTime).encrypt

      status(result) shouldBe 303
      redirectLocation(result).map(URLDecoder.decode(_, "UTF-8")) shouldBe Some(s"/changeAddressFailed?id=$encodedErrorMessage")
      verify(controller.saConnector).updateMainAddress(uri, addressForUpdate)
    }

  }

  "The changeAddressCompleteAction method" should {

    "display a success message with the transaction id" in new WithApplication(FakeApplication()) {

      val transactionId = "sometransactionid"

      val encodedTransactionId = SecureParameter(transactionId, currentTime).encrypt

      val result = controller.changeAddressCompleteAction(encodedTransactionId)

      val htmlBody = contentAsString(result)
      htmlBody should include("Thanks")
      htmlBody should include("Transaction ID:")
      htmlBody should include(transactionId)
    }
  }

  "The changeAddressFailedAction method" should {

    "display a failure message with the correct error" in new WithApplication(FakeApplication()) {

      val errorMessage = "some error occurred"
      val encodedErrorMessage = SecureParameter(errorMessage, currentTime).encrypt

      val result = controller.changeAddressFailedAction(encodedErrorMessage)

      status(result) shouldBe 200
      val htmlBody = contentAsString(result)
      htmlBody should include("Sorry")
      htmlBody should include("we are unable to update your details at this time.")
      htmlBody should include(errorMessage)
      htmlBody should not include "Transaction ID"
      htmlBody should not include "Thank you for telling us about the change to your details"
    }
  }

  "The redisplayChangeAddressAction" should {

    "Display the Change Address page with the form fields populated" in new WithApplication(FakeApplication()) {
      controller.resetAll()

      val addressLine1 = "xxx address line 1 xxx"
      val addressLine2 = "xxx address line 2 xxx"
      val addressLine3 = "xxx adr line 3 xxx"
      val addressLine4 = "xxx adr line 4 xxx"
      val postcode = "SE22 1BB"
      val additionalInfo = "xxx additional delivery information xxx"

      val result = controller.redisplayChangeAddressAction(geoffFisher, FakeRequest("POST", "/not-used").withFormUrlEncodedBody(
        "addressLine1" -> addressLine1,
        "addressLine2" -> addressLine2,
        "optionalAddressLines.addressLine3" -> addressLine3,
        "optionalAddressLines.addressLine4" -> addressLine4,
        "postcode" -> postcode,
        "additionalDeliveryInformation" -> additionalInfo
      ))

      status(result) should be(200)

      val htmlBody = contentAsString(result)
      htmlBody should include("addressLine1")
      htmlBody should include("addressLine2")
      htmlBody should include("addressLine3")
      htmlBody should include("addressLine4")
      htmlBody should not include "addressLine5"
      htmlBody should include("postcode")
      htmlBody should not include "country"
      htmlBody should include("additionalDeliveryInformation")

      htmlBody should include(addressLine1)
      htmlBody should include(addressLine2)
      htmlBody should include(addressLine3)
      htmlBody should include(addressLine4)
      htmlBody should include(postcode)
      htmlBody should include(additionalInfo)
    }
  }

  private def request(user: User, action: (User, Request[_]) => Result): String = {
    val result = action(user, FakeRequest())

    status(result) should be(200)

    contentAsString(result)
  }
}
