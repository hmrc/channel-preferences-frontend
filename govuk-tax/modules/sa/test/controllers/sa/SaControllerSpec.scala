package controllers.sa

import play.api.test.{ FakeRequest, WithApplication }
import uk.gov.hmrc.microservice.MockMicroServicesForTests
import uk.gov.hmrc.microservice.auth.AuthMicroService
import play.api.mvc.{ AnyContent, Action }
import uk.gov.hmrc.microservice.sa.SaMicroService
import org.joda.time.{ DateTimeZone, DateTime }
import java.net.URI
import controllers.common.SessionTimeoutWrapper._
import uk.gov.hmrc.common.BaseSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import controllers.common.{ SsoPayloadEncryptor, CookieEncryption }
import controllers.sa.StaticHTMLBanner._

import uk.gov.hmrc.microservice.auth.domain.{ Utr, UserAuthority, Regimes }
import play.api.libs.ws.Response
import uk.gov.hmrc.microservice.sa.domain._
import uk.gov.hmrc.common.microservice.auth.domain.Preferences
import scala.Some
import play.api.test.FakeApplication
import uk.gov.hmrc.common.microservice.auth.domain.SaPreferences
import controllers.common.service.FrontEndConfig
import uk.gov.hmrc.microservice.auth.domain.UserAuthority
import uk.gov.hmrc.microservice.sa.domain.SaRoot
import uk.gov.hmrc.microservice.sa.domain.SaIndividualAddress
import uk.gov.hmrc.common.microservice.auth.domain.Preferences
import scala.Some
import uk.gov.hmrc.microservice.auth.domain.Regimes
import uk.gov.hmrc.microservice.sa.domain.SaPerson
import play.api.test.FakeApplication
import uk.gov.hmrc.microservice.sa.domain.TransactionId
import play.api.libs.ws.Response
import uk.gov.hmrc.microservice.auth.domain.Utr
import uk.gov.hmrc.common.microservice.auth.domain.SaPreferences

class SaControllerSpec extends BaseSpec with ShouldMatchers with MockitoSugar with CookieEncryption {

  import play.api.test.Helpers._

  private val mockAuthMicroService = mock[AuthMicroService]
  private val mockSaMicroService = mock[SaMicroService]
  private val currentTime = new DateTime(2012, 12, 21, 12, 4, 32, DateTimeZone.UTC)
  val mockUtr = Utr("someUtr")

  private def controller = new SaController with MockMicroServicesForTests {
    override val authMicroService = mockAuthMicroService
    override val saMicroService = mockSaMicroService
    override def now = () => currentTime
  }

  when(mockAuthMicroService.authority("/auth/oid/gfisher")).thenReturn(
    Some(UserAuthority("someIdWeDontCareAboutHere", Regimes(paye = Some(URI.create("/personal/paye/DF334476B")), sa = Some(URI.create("/sa/individual/123456789012")), vat = Set(URI.create("/some-undecided-url"))), Some(new DateTime(1000L)), utr = Some(mockUtr))))

  when(mockSaMicroService.root("/sa/individual/123456789012")).thenReturn(
    SaRoot(
      utr = "123456789012",
      links = Map(
        "personalDetails" -> "/sa/individual/123456789012/details")
    )
  )

  val nameFromSa = "Geoff Fisher From SA"
  val nameFromGovernmentGateway = "Geoffrey From Government Gateway"

  "The details page" should {
    "show the individual SA address of Geoff Fisher" in new WithApplication(FakeApplication()) {

      when(mockSaMicroService.person("/sa/individual/123456789012/details")).thenReturn(
        Some(SaPerson(
          name = nameFromSa,
          utr = "123456789012",
          address = SaIndividualAddress(
            addressLine1 = "address line 1",
            addressLine2 = "address line 2",
            addressLine3 = "address line 3",
            addressLine4 = "address line 4",
            addressLine5 = "address line 5",
            postcode = "postcode",
            foreignCountry = "foreign country",
            additionalDeliveryInformation = "additional delivery info"
          )
        ))
      )

      val content = request(controller.details)

      content should include(nameFromSa)
      content should include(nameFromGovernmentGateway)
      content should include("address line 1")
      content should include("address line 2")
      content should include("address line 3")
      content should include("address line 4")
      content should include("address line 5")
      content should include("postcode")
      content should include("Change address")
    }

    "display an error page if personal details do not come back from backend service" in new WithApplication(FakeApplication()) {
      when(mockSaMicroService.person("/sa/individual/123456789012/details")).thenReturn(None)
      val result = controller.details(FakeRequest().withSession("userId" -> encrypt("/auth/oid/gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt("<governmentGatewayToken/>"), sessionTimestampKey -> controller.now().getMillis.toString))
      status(result) should be(404)
    }
  }

  "Print preferences check" should {

    val credId = "myCredId"

    "return  204 and HTML code when then authority for credId does not exist" in new WithApplication(FakeApplication()) {
      when(mockAuthMicroService.preferences(credId)).thenReturn(None)

      val encryptedJson = SsoPayloadEncryptor.encrypt(s"""{"credId" : "$credId", "time": ${currentTime.getMillis}}""")

      val result = controller.checkPrintPreferences(encryptedJson)(FakeRequest())
      status(result) should be(204)
    }

    "return 204 and no body when the are no preferences for the given credId" in new WithApplication(FakeApplication()) {

      when(mockAuthMicroService.preferences(credId)).thenReturn(None)

      val encryptedJson = SsoPayloadEncryptor.encrypt(s"""{"credId" : "$credId", "time": ${currentTime.getMillis}}""")

      val result = controller.checkPrintPreferences(encryptedJson)(FakeRequest())
      status(result) should be(204)

      val htmlBody = contentAsString(result)
      htmlBody mustBe ("")
    }

    "return 200 and HTML code when no preferences have yet been stored" in new WithApplication(FakeApplication()) {

      when(mockAuthMicroService.preferences(credId)).thenReturn(Some(Preferences(Some(SaPreferences(None, None)))))

      val encryptedJson = SsoPayloadEncryptor.encrypt(s"""{"credId" : "$credId", "time": ${currentTime.getMillis}}""")

      val result = controller.checkPrintPreferences(encryptedJson)(FakeRequest())
      status(result) should be(200)

      val htmlBody = contentAsString(result)
      htmlBody mustBe (saPreferences(s"${FrontEndConfig.frontendUrl}/sa/prefs"))
    }

    "return 200 and HTML code when no preferences for sa have yet been stored" in new WithApplication(FakeApplication()) {

      when(mockAuthMicroService.preferences(credId)).thenReturn(Some(Preferences(sa = None)))

      val encryptedJson = SsoPayloadEncryptor.encrypt(s"""{"credId" : "$credId", "time": ${currentTime.getMillis}}""")

      val result = controller.checkPrintPreferences(encryptedJson)(FakeRequest())
      status(result) should be(200)

      val htmlBody = contentAsString(result)
      htmlBody mustBe (saPreferences(s"${FrontEndConfig.frontendUrl}/sa/prefs"))
    }

    "return 204 and no body when sa preferences for printing have already been stored" in new WithApplication(FakeApplication()) {
      when(mockAuthMicroService.preferences(credId)).thenReturn(Some(Preferences(Some(SaPreferences(Some(false), None)))))

      val encryptedJson = SsoPayloadEncryptor.encrypt(s"""{"credId" : "$credId", "time": ${currentTime.getMillis}}""")

      val result = controller.checkPrintPreferences(encryptedJson)(FakeRequest())
      status(result) should be(204)

      val htmlBody = contentAsString(result)
      htmlBody mustBe ("")
    }

    "return 400 if the json body timestamp is more than 5 minutes old" in new WithApplication(FakeApplication()) {
      val encryptedJson = SsoPayloadEncryptor.encrypt(s"""{"credId" : "$credId", "time": ${currentTime.minusMinutes(6).getMillis}}""")

      val result = controller.checkPrintPreferences(encryptedJson)(FakeRequest())
      status(result) should be(400)
    }

  }

  "Print preferences details page " should {

    "render a form with print preference fields to be entered" in new WithApplication(FakeApplication()) {

      val result = controller.prefsForm()(FakeRequest("GET", "/prefs?rd=redirest_url").withFormUrlEncodedBody("email" -> "someuser@test.com")
        .withSession("userId" -> encrypt("/auth/oid/gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt("<governmentGatewayToken/>"), sessionTimestampKey -> controller.now().getMillis.toString))

      status(result) should be(200)

      val htmlBody = contentAsString(result)
      htmlBody should include("Print preferences")
      htmlBody should include("prefs_suppressPrinting")
      htmlBody should include("email")
      htmlBody should include("redirectUrl")
    }

    "return a NotFound when the redirect url is missing" in new WithApplication(FakeApplication()) {

      val result = controller.prefsForm()(FakeRequest().withFormUrlEncodedBody("email" -> "someuser@test.com")
        .withSession("userId" -> encrypt("/auth/oid/gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt("<governmentGatewayToken/>"), sessionTimestampKey -> controller.now().getMillis.toString))

      status(result) should be(404)
    }
  }

  "Print preferences detail page for submit " should {

    " show the email address error message the email is missing but print suppression is set to yes" in new WithApplication(FakeApplication()) {

      val result = controller.submitPrefsForm()(FakeRequest().withFormUrlEncodedBody("prefs.suppressPrinting" -> "true")
        .withSession("userId" -> encrypt("/auth/oid/gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt("<governmentGatewayToken/>"), sessionTimestampKey -> controller.now().getMillis.toString))

      status(result) shouldBe 400
      val requestBenefits = contentAsString(result)
      requestBenefits should include("Email address must be provided")

    }

    " show the email address error message the email structure is invalid " in new WithApplication(FakeApplication()) {

      val result = controller.submitPrefsForm()(FakeRequest().withFormUrlEncodedBody("prefs.email" -> "some@user@test.com", "prefs.suppressPrinting" -> "true")
        .withSession("userId" -> encrypt("/auth/oid/gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt("<governmentGatewayToken/>"), sessionTimestampKey -> controller.now().getMillis.toString))

      status(result) shouldBe 400
      val requestBenefits = contentAsString(result)
      requestBenefits should include("Valid email required")

    }

    " call the auth service to persist the preference data if the data entered is valid with print suppression and email supplied" in new WithApplication(FakeApplication()) {
      val emailAddress = "someuser@test.com"
      val mockResponse = mock[Response]
      val redirectUrl = "www.some.redirect.url"
      when(mockAuthMicroService.savePreferences("/auth/oid/gfisher", Preferences(sa = Some(SaPreferences(Some(true), Some(emailAddress)))))).thenReturn(Some(mockResponse))

      val result = controller.submitPrefsForm()(FakeRequest().withFormUrlEncodedBody("prefs.email" -> emailAddress, "prefs.suppressPrinting" -> "true", "redirectUrl" -> redirectUrl)
        .withSession("userId" -> encrypt("/auth/oid/gfisher"),
          "name" -> encrypt(nameFromGovernmentGateway),
          "token" -> encrypt("<governmentGatewayToken/>"), sessionTimestampKey -> controller.now().getMillis.toString))

      status(result) shouldBe 303
      redirectLocation(result).get shouldBe redirectUrl
    }

    " call the auth service to persist the preference data if the data entered is valid with print suppression false and no email address supplied" in new WithApplication(FakeApplication()) {
      val emailAddress = "someuser@test.com"
      val mockResponse = mock[Response]
      val redirectUrl = "www.some.redirect.url"
      when(mockAuthMicroService.savePreferences("/auth/oid/gfisher", Preferences(sa = Some(SaPreferences(Some(false), None))))).thenReturn(Some(mockResponse))

      val result = controller.submitPrefsForm()(FakeRequest().withFormUrlEncodedBody("prefs.suppressPrinting" -> "false", "redirectUrl" -> redirectUrl)
        .withSession("userId" -> encrypt("/auth/oid/gfisher"),
          "name" -> encrypt(nameFromGovernmentGateway),
          "token" -> encrypt("<governmentGatewayToken/>"), sessionTimestampKey -> controller.now().getMillis.toString))

      val prefsPageContent = contentAsString(result)
      println(prefsPageContent)

      status(result) shouldBe 303
      redirectLocation(result).get shouldBe redirectUrl
    }
  }

  "Change Address Page " should {
    "render a form with address fields to be entered when a user is logged in and authorised for SA" in new WithApplication(FakeApplication()) {

      val result = controller.changeMyAddressForm()(FakeRequest("GET", "/prefs?rd=redirest_url").withFormUrlEncodedBody("email" -> "someuser@test.com")
        .withSession("userId" -> encrypt("/auth/oid/gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt("<governmentGatewayToken/>"), sessionTimestampKey -> controller.now().getMillis.toString))

      status(result) should be(200)

      val htmlBody = contentAsString(result)
      htmlBody should include("additionalDeliveryInfo")
      htmlBody should include("addressLine1")
      htmlBody should include("addressLine2")
      htmlBody should include("addressLine3")
      htmlBody should include("addressLine4")
      htmlBody should not include ("addressLine5")
      htmlBody should include("postcode")
      htmlBody should not include ("country")
    }
  }

  val expectedInvalidCharacterErrorMessage = """This line contains an invalid character.  Valid characters are: A-Z a-z 0-9 -  , / &amp; space"""
  "Submit Change Address Page " should {
    " show the address line 1 error message if it is missing " in new WithApplication(FakeApplication()) {
      val result = controller.submitChangeAddressForm()(FakeRequest()
        .withFormUrlEncodedBody("addressLine2" -> "addressline2data")
        .withSession("userId" -> encrypt("/auth/oid/gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt("<governmentGatewayToken/>"), sessionTimestampKey -> controller.now().getMillis.toString))

      status(result) shouldBe 400
      val changeAddressSource = contentAsString(result)
      println(changeAddressSource)
      changeAddressSource should include("Address Line 1 is required")
    }

    " show the address line 1 error message if the data is greater than 28 characters " in new WithApplication(FakeApplication()) {
      val result = controller.submitChangeAddressForm()(FakeRequest()
        .withFormUrlEncodedBody("addressLine1" -> "12345678901234567890123456789", "addressLine2" -> "addressline2data")
        .withSession("userId" -> encrypt("/auth/oid/gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt("<governmentGatewayToken/>"), sessionTimestampKey -> controller.now().getMillis.toString))

      status(result) shouldBe 400
      val changeAddressSource = contentAsString(result)
      println(changeAddressSource)
      changeAddressSource should include("This address line field must not be greater than 28 characters")
    }

    " show the address line 2 error message if the data is greater than 28 characters " in new WithApplication(FakeApplication()) {
      val result = controller.submitChangeAddressForm()(FakeRequest()
        .withFormUrlEncodedBody("addressLine1" -> "addressline1data", "addressLine2" -> "12345678901234567890123456789")
        .withSession("userId" -> encrypt("/auth/oid/gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt("<governmentGatewayToken/>"), sessionTimestampKey -> controller.now().getMillis.toString))

      status(result) shouldBe 400
      val changeAddressSource = contentAsString(result)
      println(changeAddressSource)
      changeAddressSource should include("This address line field must not be greater than 28 characters")
    }

    " show the address line 3 error message if the data is greater than 18 characters " in new WithApplication(FakeApplication()) {
      val result = controller.submitChangeAddressForm()(FakeRequest()
        .withFormUrlEncodedBody("addressLine1" -> "addressline1data", "addressLine2" -> "addressline2data", "optionalAddressLines.addressLine3" -> "1234567890123456789")
        .withSession("userId" -> encrypt("/auth/oid/gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt("<governmentGatewayToken/>"), sessionTimestampKey -> controller.now().getMillis.toString))

      status(result) shouldBe 400
      val changeAddressSource = contentAsString(result)
      println(changeAddressSource)
      changeAddressSource should include("This address line field must not be greater than 18 characters")
    }

    " show the address line 4 error message if the data is greater than 18 characters " in new WithApplication(FakeApplication()) {
      val result = controller.submitChangeAddressForm()(FakeRequest()
        .withFormUrlEncodedBody("addressLine1" -> "addressline1data", "addressLine2" -> "addressline2data", "optionalAddressLines.addressLine3" -> "addressline3data", "optionalAddressLines.addressLine4" -> "1234567890123456789")
        .withSession("userId" -> encrypt("/auth/oid/gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt("<governmentGatewayToken/>"), sessionTimestampKey -> controller.now().getMillis.toString))

      status(result) shouldBe 400
      val changeAddressSource = contentAsString(result)
      println(changeAddressSource)
      changeAddressSource should include("This address line field must not be greater than 18 characters")
    }

    " show the address line 2 error message if it is missing " in new WithApplication(FakeApplication()) {
      val result = controller.submitChangeAddressForm()(FakeRequest()
        .withFormUrlEncodedBody("addressLine1" -> "addressline1data")
        .withSession("userId" -> encrypt("/auth/oid/gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt("<governmentGatewayToken/>"), sessionTimestampKey -> controller.now().getMillis.toString))

      status(result) shouldBe 400
      val changeAddressSource = contentAsString(result)
      println(changeAddressSource)
      changeAddressSource should include("Address Line 2 is required")
    }

    " show the address line 3 error message when address line 4 is present " in new WithApplication(FakeApplication()) {
      val result = controller.submitChangeAddressForm()(FakeRequest()
        .withFormUrlEncodedBody("addressLine1" -> "addressline1data", "addressLine2" -> "addressline2data", "optionalAddressLines.addressLine4" -> "addressline4data")
        .withSession("userId" -> encrypt("/auth/oid/gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt("<governmentGatewayToken/>"), sessionTimestampKey -> controller.now().getMillis.toString))

      status(result) shouldBe 400
      val changeAddressSource = contentAsString(result)
      println(changeAddressSource)
      changeAddressSource should include("Address Line 3 is required when using Address Line 4")
    }

    "show the address line 1 error message if it contains an invalid character" in new WithApplication(FakeApplication()) {
      val result = controller.submitChangeAddressForm()(FakeRequest()
        .withFormUrlEncodedBody("addressLine1" -> "address_Line1BadData", "addressLine2" -> "addressLine2Data")
        .withSession("userId" -> encrypt("/auth/oid/gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt("<governmentGatewayToken/>"), sessionTimestampKey -> controller.now().getMillis.toString))

      status(result) shouldBe 400
      val changeAddressSource = contentAsString(result)
      println(changeAddressSource)
      changeAddressSource should include(expectedInvalidCharacterErrorMessage)
    }

    "show the address line 2 error message if it contains an invalid character" in new WithApplication(FakeApplication()) {
      val result = controller.submitChangeAddressForm()(FakeRequest()
        .withFormUrlEncodedBody("addressLine1" -> "addressLine1Data", "addressLine2" -> "addressLine2|BadData")
        .withSession("userId" -> encrypt("/auth/oid/gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt("<governmentGatewayToken/>"), sessionTimestampKey -> controller.now().getMillis.toString))

      status(result) shouldBe 400
      val changeAddressSource = contentAsString(result)
      println(changeAddressSource)
      changeAddressSource should include(expectedInvalidCharacterErrorMessage)
    }

    "show the address line 3 error message if it contains an invalid character" in new WithApplication(FakeApplication()) {
      val result = controller.submitChangeAddressForm()(FakeRequest()
        .withFormUrlEncodedBody("addressLine1" -> "addressLine1Data", "addressLine2" -> "addressLine2Data", "optionalAddressLines.addressLine3" -> "addressLine4~Bad")
        .withSession("userId" -> encrypt("/auth/oid/gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt("<governmentGatewayToken/>"), sessionTimestampKey -> controller.now().getMillis.toString))

      status(result) shouldBe 400
      val changeAddressSource = contentAsString(result)
      println(changeAddressSource)
      changeAddressSource should include(expectedInvalidCharacterErrorMessage)
    }

    "show the address line 4 error message if it contains an invalid character" in new WithApplication(FakeApplication()) {
      val result = controller.submitChangeAddressForm()(FakeRequest()
        .withFormUrlEncodedBody("addressLine1" -> "addressLine1Data", "addressLine2" -> "addressLine2Data", "optionalAddressLines.addressLine3" -> "addressLine3Data", "optionalAddressLines.addressLine4" -> "addressLine4!Bad")
        .withSession("userId" -> encrypt("/auth/oid/gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt("<governmentGatewayToken/>"), sessionTimestampKey -> controller.now().getMillis.toString))

      status(result) shouldBe 400
      val changeAddressSource = contentAsString(result)
      println(changeAddressSource)
      changeAddressSource should include(expectedInvalidCharacterErrorMessage)
    }

    "allow all valid characters in address lines" in new WithApplication(FakeApplication()) {
      val result = controller.submitChangeAddressForm()(FakeRequest()
        .withFormUrlEncodedBody("addressLine1" -> "ABCDEFGHIJKLMNOPQRSTUVWXYZab", "addressLine2" -> "cdefghijklmnopqrstuvwxyz0123", "optionalAddressLines.addressLine3" -> "4567890 ,/&'-", "optionalAddressLines.addressLine4" -> "all valid")
        .withSession("userId" -> encrypt("/auth/oid/gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt("<governmentGatewayToken/>"), sessionTimestampKey -> controller.now().getMillis.toString))

      status(result) shouldBe 200
    }

    "take the user to a confirmation page that displays the form values entered" in new WithApplication(FakeApplication()) {
      val additionalDeliveryInfomation = "someAdditionalDeliveryInfo"
      val addressData1 = "ad1"
      val addressData2 = "ad2"
      val addressData3 = "ad3"
      val addressData4 = "ad4"
      val postcode = "XX1 0YY"

      val result = controller.submitChangeAddressForm()(FakeRequest().withFormUrlEncodedBody("additionalDeliveryInfo" -> additionalDeliveryInfomation, "addressLine1" -> addressData1, "addressLine2" -> addressData2, "optionalAddressLines.addressLine3" -> addressData3, "optionalAddressLines.addressLine4" -> addressData4, "postcode" -> postcode)
        .withSession("userId" -> encrypt("/auth/oid/gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt("<governmentGatewayToken/>"), sessionTimestampKey -> controller.now().getMillis.toString))

      status(result) should be(200)

      // Assert displaying of values
      val htmlBody = contentAsString(result)
      htmlBody should include(additionalDeliveryInfomation)
      htmlBody should include(addressData1)
      htmlBody should include(addressData2)
      htmlBody should include(addressData3)
      htmlBody should include(addressData4)
      htmlBody should include(postcode)

      // Assert hidden form
      htmlBody should include("""form action="/changeAddressConfirm" method="POST"""")
      htmlBody should include(s"""input type="hidden" name="additionalDeliveryInfo" id="additionalDeliveryInfo" value="${additionalDeliveryInfomation}" """)
      htmlBody should include(s"""input type="hidden" name="addressLine1" id="addressLine1" value="${addressData1}" """)
      htmlBody should include(s"""input type="hidden" name="addressLine2" id="addressLine2" value="${addressData2}" """)
      htmlBody should include(s"""input type="hidden" name="optionalAddressLines.addressLine3" id="optionalAddressLines_addressLine3" value="${addressData3}" """)
      htmlBody should include(s"""input type="hidden" name="optionalAddressLines.addressLine4" id="optionalAddressLines_addressLine4" value="${addressData4}" """)
      htmlBody should include(s"""input type="hidden" name="postcode" id="postcode" value="${postcode}" """)

      htmlBody should not include ("addressLine5")
      htmlBody should not include ("country")
    }
  }
  "Submit Change Address Confirmation Page  " should {
    // TODO: Post payload validation tests
    " use the post payload to submit the changed address to the SA service" in new WithApplication(FakeApplication()) {

      val add1 = "add1"
      val add2 = "add2"
      val utr = "someUtr"
      val updateAddressUri = s"/sa/individual/${utr}/main-address"

      //val mainAddress = MainAddress(None, Some(add1), Some(add2), None, None,None)

      val transactionId = "sometransactionid"
      when(mockSaMicroService.updateMainAddress(updateAddressUri, None, addressLine1 = add1, addressLine2 = add2, None, None, None)).thenReturn(Some(TransactionId(transactionId)))

      val result = controller.submitConfirmChangeMyAddressForm()(FakeRequest()
        .withFormUrlEncodedBody("addressLine1" -> add1, "addressLine2" -> add2)
        .withSession("userId" -> encrypt("/auth/oid/gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt("<governmentGatewayToken/>"), sessionTimestampKey -> controller.now().getMillis.toString))

      status(result) shouldBe 200
      val htmlBody = contentAsString(result)
      htmlBody should include("Thank you for telling us about the change to your details.")
      htmlBody should include("Transaction ID:")

      verify(mockSaMicroService).updateMainAddress(updateAddressUri, None, addressLine1 = add1, addressLine2 = add2, None, None, None)
    }

  }

  //Valid Characters Alphanumeric (A-Z, a-z, 0-9), hyphen( - ), apostrophe ( ' ), comma ( , ), forward slash ( / ) ampersand ( & ) and space
  // (48 to 57 0-9) (65 to 90 A-Z) (97 to 122 a-z) (32 space) (38 ampersand ( & )) (39 apostrophe ( ' )) (44 comma ( , ))   (45 hyphen( - )) (47 forward slash ( / ))

  " Valid character checker " should {
    " return false if an invalid character is present in an input " in {
      var digits = for (i <- 48 to 57) yield i
      var lowerCaseLetters = for (i <- 97 to 122) yield i
      var upperCaseLetters = for (i <- 65 to 90) yield i
      val specialCharacters = List(32, 38, 39, 44, 45, 47)

      val validCharacters = digits ++ lowerCaseLetters ++ upperCaseLetters ++ specialCharacters

      for (chr <- 0 to 1000) {
        val c = chr.toChar
        val str = s"this $chr contains $c"
        characterValidator.isValid(Some(str)) match {
          case true => validCharacters.contains(chr) must be(true)
          case false => validCharacters.contains(chr) must be(false)
        }
      }
    }
    " return true when None is passed as the value" in {
      characterValidator.isValid(None) must be(true)
    }
  }

  def request(action: Action[AnyContent]): String = {
    val result = action(FakeRequest().withSession("userId" -> encrypt("/auth/oid/gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt("<governmentGatewayToken/>"), sessionTimestampKey -> controller.now().getMillis.toString))

    status(result) should be(200)

    contentAsString(result)
  }
}
