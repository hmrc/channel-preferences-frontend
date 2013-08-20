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
import uk.gov.hmrc.common.microservice.auth.domain.{ SaPreferences, Preferences }
import uk.gov.hmrc.microservice.auth.domain.UserAuthority
import uk.gov.hmrc.microservice.sa.domain.SaRoot
import uk.gov.hmrc.microservice.sa.domain.SaIndividualAddress
import uk.gov.hmrc.common.microservice.auth.domain.Preferences
import scala.Some
import uk.gov.hmrc.microservice.auth.domain.Regimes
import uk.gov.hmrc.microservice.sa.domain.SaPerson
import play.api.test.FakeApplication
import uk.gov.hmrc.common.BaseSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import controllers.common.{ SsoPayloadEncryptor, CookieEncryption }
import uk.gov.hmrc.common.microservice.auth.domain.{ SaPreferences, Preferences }
import controllers.sa.StaticHTMLBanner._

import uk.gov.hmrc.common.microservice.auth.domain.SaPreferences
import play.api.libs.ws.Response
import play.api.test.Helpers._
import uk.gov.hmrc.microservice.auth.domain.UserAuthority
import play.api.libs.ws.Response
import uk.gov.hmrc.microservice.sa.domain.SaRoot
import uk.gov.hmrc.microservice.sa.domain.SaIndividualAddress
import uk.gov.hmrc.common.microservice.auth.domain.Preferences
import scala.Some
import uk.gov.hmrc.microservice.auth.domain.Regimes
import uk.gov.hmrc.microservice.sa.domain.SaPerson
import play.api.test.FakeApplication
import uk.gov.hmrc.common.microservice.auth.domain.SaPreferences
import controllers.common.service.FrontEndConfig

class SaControllerSpec extends BaseSpec with ShouldMatchers with MockitoSugar with CookieEncryption {

  import play.api.test.Helpers._

  private val mockAuthMicroService = mock[AuthMicroService]
  private val mockSaMicroService = mock[SaMicroService]
  private val currentTime = new DateTime(2012, 12, 21, 12, 4, 32, DateTimeZone.UTC)

  private def controller = new SaController with MockMicroServicesForTests {
    override val authMicroService = mockAuthMicroService
    override val saMicroService = mockSaMicroService
    override def now = () => currentTime
  }

  when(mockAuthMicroService.authority("/auth/oid/gfisher")).thenReturn(
    Some(UserAuthority("someIdWeDontCareAboutHere", Regimes(paye = Some(URI.create("/personal/paye/DF334476B")), sa = Some(URI.create("/personal/sa/123456789012")), vat = Set(URI.create("/some-undecided-url"))), Some(new DateTime(1000L)))))

  when(mockSaMicroService.root("/personal/sa/123456789012")).thenReturn(
    SaRoot(
      utr = "123456789012",
      links = Map(
        "personalDetails" -> "/personal/sa/123456789012/details")
    )
  )

  val nameFromSa = "Geoff Fisher From SA"
  val nameFromGovernmentGateway = "Geoffrey From Government Gateway"

  "The details page" should {
    "show the individual SA address of Geoff Fisher" in new WithApplication(FakeApplication()) {

      when(mockSaMicroService.person("/personal/sa/123456789012/details")).thenReturn(
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
    }

    "display an error page if personal details do not come back from backend service" in new WithApplication(FakeApplication()) {
      when(mockSaMicroService.person("/personal/sa/123456789012/details")).thenReturn(None)
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
      htmlBody should include("suppressPrinting_true")
      htmlBody should include("suppressPrinting_false")
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

    //TODO: Added validation that checks another form parameter for this test case
    //    " show the email address error message the email is missing but print suppression is set to yes" in new WithApplication(FakeApplication()){
    //
    //      val result = controller.submitPrefsForm()(FakeRequest().withFormUrlEncodedBody("email" -> "", "suppressPrinting" -> "true")
    //        .withSession("userId" -> encrypt("/auth/oid/gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt("<governmentGatewayToken/>"), sessionTimestampKey -> controller.now().getMillis.toString))
    //
    //      status(result) shouldBe 400
    //      val requestBenefits = contentAsString(result)
    //      requestBenefits should include("Invalid Email format")
    //
    //    }

    " show the email address error message the email structure is invalid " in new WithApplication(FakeApplication()) {

      val result = controller.submitPrefsForm()(FakeRequest().withFormUrlEncodedBody("email" -> "some@user@test.com", "suppressPrinting" -> "true")
        .withSession("userId" -> encrypt("/auth/oid/gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt("<governmentGatewayToken/>"), sessionTimestampKey -> controller.now().getMillis.toString))

      status(result) shouldBe 400
      val requestBenefits = contentAsString(result)
      requestBenefits should include("Invalid Email format")

    }

    " show the suppress printing error message if no option is selected " in new WithApplication(FakeApplication()) {
      val result = controller.submitPrefsForm()(FakeRequest().withFormUrlEncodedBody("email" -> "someuser@test.com")
        .withSession("userId" -> encrypt("/auth/oid/gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt("<governmentGatewayToken/>"), sessionTimestampKey -> controller.now().getMillis.toString))

      status(result) shouldBe 400

      val requestBenefits = contentAsString(result)
      requestBenefits should include("Please select a Print Suppression Option")
    }

    " call the auth service to persist the preference data if the data entered is valid " in new WithApplication(FakeApplication()) {
      val emailAddress = "someuser@test.com"
      val mockResponse = mock[Response]
      val redirectUrl = "www.some.redirect.url"
      when(mockAuthMicroService.savePreferences("/auth/oid/gfisher", Preferences(sa = Some(SaPreferences(Some(true), Some(emailAddress)))))).thenReturn(Some(mockResponse))

      val result = controller.submitPrefsForm()(FakeRequest().withFormUrlEncodedBody("email" -> emailAddress, "suppressPrinting" -> "true", "redirectUrl" -> redirectUrl)
        .withSession("userId" -> encrypt("/auth/oid/gfisher"),
          "name" -> encrypt(nameFromGovernmentGateway),
          "token" -> encrypt("<governmentGatewayToken/>"), sessionTimestampKey -> controller.now().getMillis.toString))

      status(result) shouldBe 303
      redirectLocation(result).get shouldBe redirectUrl
    }
  }

  def request(action: Action[AnyContent]): String = {
    val result = action(FakeRequest().withSession("userId" -> encrypt("/auth/oid/gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt("<governmentGatewayToken/>"), sessionTimestampKey -> controller.now().getMillis.toString))

    status(result) should be(200)

    contentAsString(result)
  }
}
