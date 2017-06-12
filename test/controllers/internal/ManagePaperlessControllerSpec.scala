package controllers.internal

import connectors._
import controllers.AuthorityUtils._
import helpers.{ConfigHelper, TestFixtures}
import model.Encrypted
import org.jsoup.Jsoup
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HttpResponse
import uk.gov.hmrc.play.test.UnitSpec
import NewPreferenceResponse._


import scala.concurrent.Future

abstract class Setup extends MockitoSugar {
  val mockAuditConnector = mock[AuditConnector]
  val mockAuthConnector = mock[AuthConnector]
  val mockEntityResolverConnector = mock[EntityResolverConnector]
  val mockEmailConnector = mock[EmailConnector]

  val controller = new ManagePaperlessController {
    implicit val authConnector = mockAuthConnector
    val entityResolverConnector = mockEntityResolverConnector
    val emailConnector = mockEmailConnector
    val auditConnector = mockAuditConnector
  }

  val request = FakeRequest()
}

class ManagePaperlessControllerSpec extends UnitSpec with MockitoSugar with OneAppPerSuite {
  import org.mockito.Matchers.{any, eq => is}

  val validUtr = SaUtr("1234567890")
  val user = AuthContext(authority = saAuthority("userId", "1234567890"), nameFromSession = Some("Ciccio"), governmentGatewayToken = None)

  override implicit lazy val app : Application = ConfigHelper.fakeApp

  "clicking on Change email address link in the account details page" should {
    "display update email address form when accessed from Account Details" in new Setup {
      val saPreferences = SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.Verified))).toNewPreference()
      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(Future.successful(Some(saPreferences)))

      val result = Future.successful(controller._displayChangeEmailAddress(None)(user, request, TestFixtures.sampleHostContext))

      status(result) shouldBe 200
      val page = Jsoup.parse(contentAsString(result))

      page.getElementById("current-email-address").text shouldBe "test@test.com."
      page.getElementById("email.main") shouldNot be(null)
      page.getElementById("email.main").attr("value") shouldBe ""
      page.getElementById("email.confirm") shouldNot be(null)
      page.getElementById("email.confirm").attr("value") shouldBe ""
    }


    "display update email address form with the email input field pre-populated when coming back from the warning page" in new Setup {
      val saPreferences = SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.Verified))).toNewPreference()
      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(Future.successful(Some(saPreferences)))

      val existingEmailAddress = "existing@email.com"
      val result = Future.successful(controller._displayChangeEmailAddress(Some(Encrypted(EmailAddress(existingEmailAddress))))(user, request, TestFixtures.sampleHostContext))

      status(result) shouldBe 200
      val page = Jsoup.parse(contentAsString(result))

      page.getElementById("current-email-address").text shouldBe "test@test.com."
      page.getElementById("email.main") shouldNot be(null)
      page.getElementById("email.main").attr("value") shouldBe existingEmailAddress
      page.getElementById("email.confirm") shouldNot be(null)
      page.getElementById("email.confirm").attr("value") shouldBe existingEmailAddress
    }

    "return bad request if the SA user has opted into paper" in new Setup {

      val saPreferences = SaPreference(false, None).toNewPreference()
      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(Future.successful(Some(saPreferences)))

      val result = Future.successful(controller._displayChangeEmailAddress(None)(user, request, TestFixtures.sampleHostContext))

      status(result) shouldBe 400
    }
  }

  "Clicking Resend validation email link on account details page" should {

    "call preferences as if opting-in and send the email as a part of the process" in new Setup {

      val saPreferences = SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.Pending))).toNewPreference()

      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(Future.successful(Some(saPreferences)))
      when(mockEntityResolverConnector.changeEmailAddress(is("test@test.com"))(any())).thenReturn(Future.successful((HttpResponse(OK))))

      val page = Future.successful(controller._resendVerificationEmail(user, FakeRequest(), TestFixtures.sampleHostContext))

      status(page) shouldBe 200
      val document = Jsoup.parse(contentAsString(page))
      document.getElementById("verification-mail-message") should not be null
      document.getElementById("return-to-dashboard-button").attr("href") should be(TestFixtures.sampleHostContext.returnUrl)

      verify(mockEntityResolverConnector).changeEmailAddress(is("test@test.com"))(any())
    }
  }

  "Viewing the email address change thank you page" should {

    "display the confirmation page with the current email address obscured" in new Setup {
      val emailAddress = "someone@email.com"
      val saPreferences = SaPreference(true, Some(SaEmailPreference(emailAddress, SaEmailPreference.Status.Verified))).toNewPreference()

      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(Future.successful(Some(saPreferences)))

      val page = controller._displayChangeEmailAddressConfirmed(user, FakeRequest(), TestFixtures.sampleHostContext)

      status(page) shouldBe 200

      val doc = Jsoup.parse(contentAsString(page))
      doc.getElementById("updated-email-address") should have ('text ("s*****e@email.com"))
      doc.toString should not include emailAddress
    }
  }

  "A post to update email address with no emailVerifiedFlag" should {

    "validate the email address, update the address for SA user and redirect to confirmation page" in new Setup {
      val emailAddress = "someone@email.com"
      val saPreferences = SaPreference(true, Some(SaEmailPreference("oldEmailAddress@test.com", SaEmailPreference.Status.Verified))).toNewPreference()

      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(true)
      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(Future.successful(Some(saPreferences)))
      when(mockEntityResolverConnector.changeEmailAddress(is(emailAddress))(any())).thenReturn(Future.successful(HttpResponse(NOT_FOUND)))

      val page = Future.successful(controller._submitChangeEmailAddress(user, FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress),("email.confirm", emailAddress)), TestFixtures.sampleHostContext))

      status(page) shouldBe 303
      header("Location", page).get should include(routes.ManagePaperlessController.displayChangeEmailAddressConfirmed(TestFixtures.sampleHostContext).toString())

      verify(mockEntityResolverConnector).changeEmailAddress(is(emailAddress))(any())
      verify(mockEmailConnector).isValid(is(emailAddress))(any())
      verify(mockEntityResolverConnector).getPreferences()(any())
      verifyNoMoreInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "show error if the 2 email address fields do not match" in new Setup {
      val saPreferences = SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.Verified))).toNewPreference()

      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(Future.successful(Some(saPreferences)))

      val page = Future.successful(controller._submitChangeEmailAddress(user, FakeRequest().withFormUrlEncodedBody("email.main" -> "a@a.com", "email.confirm" -> "b@b.com"), TestFixtures.sampleHostContext))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select("#form-submit-email-address .error-notification").text shouldBe "Check your email addresses - they don't match."
    }

    "show error if the email address is not syntactically valid" in new Setup {
      val emailAddress = "invalid-email"
      val saPreferences = SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.Verified))).toNewPreference()

      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(Future.successful(Some(saPreferences)))
      val page = Future.successful(controller._submitChangeEmailAddress(user, FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress)), TestFixtures.sampleHostContext))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select("#form-submit-email-address .error-notification").text shouldBe "Enter a valid email address."
    }

    "show error if the email field is empty" in new Setup {
      val saPreferences = SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.Verified))).toNewPreference()

      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(Future.successful(Some(saPreferences)))

      val page = Future.successful(controller._submitChangeEmailAddress(user, FakeRequest().withFormUrlEncodedBody(("email.main", "")), TestFixtures.sampleHostContext))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select("#form-submit-email-address .error-notification").text shouldBe "Enter a valid email address."
    }

    "show a warning page if the email has a valid structure but does not pass validation by the email micro service" in new Setup {

      val emailAddress = "someone@dodgy.domain"
      val saPreferences = SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.Verified))).toNewPreference()

      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(false)
      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(Future.successful(Some(saPreferences)))

      val page = Future.successful(controller._submitChangeEmailAddress(user, FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress),("email.confirm", emailAddress)), TestFixtures.sampleHostContext))

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") shouldNot be(null)
      document.select("#emailIsCorrectLink") shouldNot be(null)

      verify(mockEmailConnector).isValid(is(emailAddress))(any())
    }

  }

  "A post to set preferences with an emailVerifiedFlag" should {

    "if the verified flag is true, save the preference and redirect to the thank you page without verifying the email address again" in new Setup {
      val emailAddress = "someone@email.com"
      val saPreferences = SaPreference(true, Some(SaEmailPreference("oldEmailAddress@test.com", SaEmailPreference.Status.Verified))).toNewPreference()

      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(Future.successful(Some(saPreferences)))
      when(mockEntityResolverConnector.changeEmailAddress(is(emailAddress))(any())).thenReturn(Future.successful(HttpResponse(OK)))

      val page = Future.successful(controller._submitChangeEmailAddress(user, FakeRequest().withFormUrlEncodedBody
        (("email.main", emailAddress), ("email.confirm", emailAddress), ("emailVerified", "true")), TestFixtures.sampleHostContext))

      status(page) shouldBe 303
      header("Location", page).get should include(routes.ManagePaperlessController.displayChangeEmailAddressConfirmed(TestFixtures.sampleHostContext).toString())

      verify(mockEntityResolverConnector).changeEmailAddress(is(emailAddress))(any())
      verify(mockEntityResolverConnector).getPreferences()(any())
      verifyNoMoreInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "if the verified flag is false and the email does not pass validation by the email micro service, display the verify page" in new Setup {

      val emailAddress = "someone@dodgy.domain"
      val saPreferences = SaPreference(true, Some(SaEmailPreference("oldEmailAddress@test.com", SaEmailPreference.Status.Verified))).toNewPreference()

      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(false)
      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(Future.successful(Some(saPreferences)))


      val page = Future.successful(controller._submitChangeEmailAddress(user, FakeRequest().withFormUrlEncodedBody
        (("email.main", emailAddress), ("email.confirm", emailAddress), ("emailVerified", "false")), TestFixtures.sampleHostContext))

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") shouldNot be(null)
      document.select("#emailIsCorrectLink") shouldNot be(null)

      verify(mockEntityResolverConnector).getPreferences()(any())
      verifyNoMoreInteractions(mockEntityResolverConnector)
      verify(mockEmailConnector).isValid(is(emailAddress))(any())
    }

    "if the verified flag is any value other than true, treat it as false" in new Setup {

      val emailAddress = "someone@dodgy.domain"
      val saPreferences = SaPreference(true, Some(SaEmailPreference("oldEmailAddress@test.com", SaEmailPreference.Status.Verified))).toNewPreference()

      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(false)
      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(Future.successful(Some(saPreferences)))


      val page = Future.successful(controller._submitChangeEmailAddress(user, FakeRequest().withFormUrlEncodedBody
        (("email.main", emailAddress), ("email.confirm", emailAddress), ("emailVerified", "hjgjhghjghjgj")), TestFixtures.sampleHostContext))

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") shouldNot be(null)
      document.select("#emailIsCorrectLink") shouldNot be(null)

      verify(mockEntityResolverConnector).getPreferences()(any())
      verifyNoMoreInteractions(mockEntityResolverConnector)
      verify(mockEmailConnector).isValid(is(emailAddress))(any())
    }
  }

  "clicking on opt-out of email reminders link in the account details page" should {

    "display the <are you sure> page" in new Setup {
      val saPreferences = SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.Verified))).toNewPreference()

      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(Future.successful(Some(saPreferences)))

      val result = controller._displayStopPaperless(user, request, TestFixtures.sampleHostContext)

      status(result) shouldBe 200
      val page = Jsoup.parse(contentAsString(result))

      page.getElementById("confirm-opt-out").text shouldBe "Stop paperless notifications"
      page.getElementById("cancel-link").text shouldBe "Cancel"
      page.text() should not include "test@test.com"
    }

    "return bad request if the user has not opted into digital" in new Setup{
      val saPreferences = SaPreference(false, None).toNewPreference()
      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(Future.successful(Some(saPreferences)))

      val result = controller._displayStopPaperless(user, request, TestFixtures.sampleHostContext)

      status(result) shouldBe 400
    }
  }

  "A post to confirm opt out of email reminders" should {

    "return a redirect to thank you page" in new Setup {
      val saPreferences = SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.Verified))).toNewPreference()

      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(Future.successful(Some(saPreferences)))
      when(mockEntityResolverConnector.updateTermsAndConditions(is(Generic -> TermsAccepted(false)), is(None))(any())).thenReturn(Future.successful(PreferencesExists))

      val result = Future.successful(controller._submitStopPaperless(user, request, TestFixtures.sampleHostContext))

      status(result) shouldBe 303
      header("Location", result).get should include(routes.ManagePaperlessController.displayStopPaperlessConfirmed(TestFixtures.sampleHostContext).url)
      val page = Jsoup.parse(contentAsString(result))

      verify(mockEntityResolverConnector).updateTermsAndConditions(is(Generic -> TermsAccepted(false)), is(None)) (any())
    }
  }

}
