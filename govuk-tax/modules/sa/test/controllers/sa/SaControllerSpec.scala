package controllers.sa

import play.api.test.{ FakeRequest, WithApplication }
import uk.gov.hmrc.microservice.{ MicroServiceException, MockMicroServicesForTests }
import uk.gov.hmrc.microservice.auth.AuthMicroService
import play.api.mvc.{ AnyContent, Action }
import uk.gov.hmrc.microservice.sa.SaMicroService
import org.joda.time.DateTime
import java.net.URI
import controllers.common.SessionTimeoutWrapper._
import uk.gov.hmrc.microservice.auth.domain.UserAuthority
import uk.gov.hmrc.microservice.sa.domain.SaRoot
import uk.gov.hmrc.microservice.sa.domain.SaIndividualAddress
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

class SaControllerSpec extends BaseSpec with ShouldMatchers with MockitoSugar with CookieEncryption {

  import play.api.test.Helpers._

  private val mockAuthMicroService = mock[AuthMicroService]
  private val mockSaMicroService = mock[SaMicroService]

  private def controller = new SaController with MockMicroServicesForTests {
    override val authMicroService = mockAuthMicroService
    override val saMicroService = mockSaMicroService
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

    "return 200 and HTML code when the are no preferences for the given credId" in new WithApplication(FakeApplication()) {

      when(mockAuthMicroService.preferences(credId)).thenReturn(None)

      val encryptedJson = SsoPayloadEncryptor.encrypt("{ \"credId\":\"" + credId + "\", \"timestamp\":1376576265070}")

      val result = controller.checkPrintPreferences(encryptedJson)(FakeRequest())
      status(result) should be(200)

      val htmlBody = contentAsString(result)
      htmlBody mustBe ("some text")
    }

    "return 200 and HTML code when the preferences for print have not been stored yet" in new WithApplication(FakeApplication()) {

      when(mockAuthMicroService.preferences(credId)).thenReturn(Some(Preferences(Some(SaPreferences(None, None)))))

      val encryptedJson = SsoPayloadEncryptor.encrypt("{ \"credId\":\"" + credId + "\", \"timestamp\":1376576265070}")

      val result = controller.checkPrintPreferences(encryptedJson)(FakeRequest())
      status(result) should be(200)

      val htmlBody = contentAsString(result)
      htmlBody mustBe ("some text")
    }

    "return 200 and HTML code when the preferences for sa have not been stored yet" in new WithApplication(FakeApplication()) {

      when(mockAuthMicroService.preferences(credId)).thenReturn(Some(Preferences(sa = None)))

      val encryptedJson = SsoPayloadEncryptor.encrypt("{ \"credId\":\"" + credId + "\", \"timestamp\":1376576265070}")

      val result = controller.checkPrintPreferences(encryptedJson)(FakeRequest())
      status(result) should be(200)

      val htmlBody = contentAsString(result)
      htmlBody mustBe ("some text")
    }

    "return 200 and no body when the preferences for print have been stored before" in new WithApplication(FakeApplication()) {
      when(mockAuthMicroService.preferences(credId)).thenReturn(Some(Preferences(Some(SaPreferences(Some(false), None)))))

      val encryptedJson = SsoPayloadEncryptor.encrypt("{ \"credId\":\"" + credId + "\", \"timestamp\":1376576265070}")

      val result = controller.checkPrintPreferences(encryptedJson)(FakeRequest())
      status(result) should be(200)

      val htmlBody = contentAsString(result)
      htmlBody mustBe ("")
    }

    "return 400 and no body when the authority for credId doesn't exist" in new WithApplication(FakeApplication()) {
      when(mockAuthMicroService.preferences(credId)).thenThrow(new MicroServiceException("Bad request", null))

      val encryptedJson = SsoPayloadEncryptor.encrypt("{ \"credId\":\"" + credId + "\", \"timestamp\":1376576265070}")

      val result = controller.checkPrintPreferences(encryptedJson)(FakeRequest())
      status(result) should be(400)
    }

    "return 400 if the json body's timestamp is more than 5 minutes old" in new WithApplication(FakeApplication()) {
      pending
    }

  }

  def request(action: Action[AnyContent]): String = {
    val result = action(FakeRequest().withSession("userId" -> encrypt("/auth/oid/gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt("<governmentGatewayToken/>"), sessionTimestampKey -> controller.now().getMillis.toString))

    status(result) should be(200)

    contentAsString(result)
  }
}
