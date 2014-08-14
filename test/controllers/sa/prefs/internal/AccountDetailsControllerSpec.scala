package controllers.sa.prefs.internal

import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.test.UnitSpec
import play.api.test.{FakeRequest, FakeApplication, WithApplication}
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.domain.User
import scala.concurrent.Future
import org.jsoup.Jsoup
import org.mockito.Mockito._
import connectors.EmailConnector
import play.api.test.Helpers._
import org.mockito.Matchers
import java.net.URI
import uk.gov.hmrc.common.crypto.Encrypted
import controllers.sa.prefs.AuthorityUtils._
import connectors.{SaEmailPreference, SaPreference, PreferencesConnector}

abstract class Setup extends WithApplication(FakeApplication()) with MockitoSugar {
  val auditConnector = mock[AuditConnector]
  val authConnector = mock[AuthConnector]
  val mockPreferencesConnector = mock[PreferencesConnector]
  val mockEmailConnector = mock[EmailConnector]
  val controller = new AccountDetailsController(auditConnector, mockPreferencesConnector,mockEmailConnector)(authConnector)

  val request = FakeRequest()
}

class AccountDetailsControllerSpec extends UnitSpec with MockitoSugar  {
  import Matchers.{any, eq => is}

  val validUtr = SaUtr("1234567890")
  val user = User(userId = "userId", userAuthority = saAuthority("userId", "1234567890"), nameFromGovernmentGateway = Some("Ciccio"), decryptedToken = None)

  "clicking on Change email address link in the account details page" should {
    "display update email address form when accessed from Account Details" in new Setup {
      val saPreferences = SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.verified)))
      when(mockPreferencesConnector.getPreferences(is(validUtr))(any())).thenReturn(Future.successful(Some(saPreferences)))

      val result = Future.successful(controller.changeEmailAddressPage(None)(user, request))

      status(result) shouldBe 200
      val page = Jsoup.parse(contentAsString(result))

      page.getElementById("current-email-address").text shouldBe "test@test.com."
      page.getElementById("email.main") shouldNot be(null)
      page.getElementById("email.main").attr("value") shouldBe ""
      page.getElementById("email.confirm") shouldNot be(null)
      page.getElementById("email.confirm").attr("value") shouldBe ""
    }


    "display update email address form with the email input field pre-populated when coming back from the warning page" in new Setup {
      val saPreferences = SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.verified)))
      when(mockPreferencesConnector.getPreferences(is(validUtr))(any())).thenReturn(Future.successful(Some(saPreferences)))

      val existingEmailAddress = "existing@email.com"
      val result = Future.successful(controller.changeEmailAddressPage(Some(Encrypted(EmailAddress(existingEmailAddress))))(user, request))

      status(result) shouldBe 200
      val page = Jsoup.parse(contentAsString(result))

      page.getElementById("current-email-address").text shouldBe "test@test.com."
      page.getElementById("email.main") shouldNot be(null)
      page.getElementById("email.main").attr("value") shouldBe existingEmailAddress
      page.getElementById("email.confirm") shouldNot be(null)
      page.getElementById("email.confirm").attr("value") shouldBe existingEmailAddress
    }

    "return bad request if the SA user has opted into paper" in new Setup {

      val saPreferences = SaPreference(false, None)
      when(mockPreferencesConnector.getPreferences(is(validUtr))(any())).thenReturn(Future.successful(Some(saPreferences)))

      val result = Future.successful(controller.changeEmailAddressPage(None)(user, request))

      status(result) shouldBe 400
    }
  }

  "Clicking Resend validation email link on account details page" should {

    "call preferences as if opting-in and send the email as a part of the process" in new Setup {

      val saPreferences = SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.pending)))

      when(mockPreferencesConnector.getPreferences(is(validUtr))(any())).thenReturn(Future.successful(Some(saPreferences)))
      when(mockPreferencesConnector.savePreferences(is(validUtr), is(true), is(Some("test@test.com")))(any())).thenReturn(Future.successful(()))

      val page = Future.successful(controller.resendValidationEmailAction(user, FakeRequest()))

      status(page) shouldBe 200
      val document = Jsoup.parse(contentAsString(page))
      document.getElementById("verification-mail-message") should not be null

      verify(mockPreferencesConnector).savePreferences(is(validUtr), is(true), is(Some("test@test.com")))(any())

    }
  }

  "Viewing the email address change thank you page" should {

    "display the confirmation page with the current email address obscured" in new Setup {
      val emailAddress = "someone@email.com"
      val saPreferences = SaPreference(true, Some(SaEmailPreference(emailAddress, SaEmailPreference.Status.verified)))

      when(mockPreferencesConnector.getPreferences(is(validUtr))(any())).thenReturn(Future.successful(Some(saPreferences)))

      val page = controller.emailAddressChangeThankYouPage(user, FakeRequest())

      status(page) shouldBe 200

      val doc = Jsoup.parse(contentAsString(page))
      doc.getElementById("updated-email-address") should have ('text ("s*****e@email.com"))
      doc.toString should not include emailAddress
    }
  }

  "A post to update email address with no emailVerifiedFlag" should {

    "validate the email address, update the address for SA user and redirect to confirmation page" in new Setup {
      val emailAddress = "someone@email.com"
      val saPreferences = SaPreference(true, Some(SaEmailPreference("oldEmailAddress@test.com", SaEmailPreference.Status.verified)))

      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(true)
      when(mockPreferencesConnector.getPreferences(is(validUtr))(any())).thenReturn(Future.successful(Some(saPreferences)))
      when(mockPreferencesConnector.savePreferences(is(validUtr), is(true), is(Some(emailAddress)))(any())).thenReturn(Future.successful(None))

      val page = Future.successful(controller.submitEmailAddressPage(user, FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress),("email.confirm", emailAddress))))

      status(page) shouldBe 303
      header("Location", page).get should include(routes.AccountDetailsController.emailAddressChangeThankYou().toString())

      verify(mockPreferencesConnector).savePreferences(is(validUtr), is(true), is(Some(emailAddress)))(any())
      verify(mockEmailConnector).isValid(is(emailAddress))(any())
      verify(mockPreferencesConnector).getPreferences(is(validUtr))(any())
      verifyNoMoreInteractions(mockPreferencesConnector, mockEmailConnector)
    }

    "show error if the 2 email address fields do not match" in new Setup {
      val saPreferences = SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.verified)))

      when(mockPreferencesConnector.getPreferences(is(validUtr))(any())).thenReturn(Future.successful(Some(saPreferences)))

      val page = Future.successful(controller.submitEmailAddressPage(user, FakeRequest().withFormUrlEncodedBody("email.main" -> "a@a.com", "email.confirm" -> "b@b.com")))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select("#form-submit-email-address .error-notification").text shouldBe "Check your email addresses - they don’t match."
    }

    "show error if the email address is not syntactically valid" in new Setup {
      val emailAddress = "invalid-email"
      val saPreferences = SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.verified)))

      when(mockPreferencesConnector.getPreferences(is(validUtr))(any())).thenReturn(Future.successful(Some(saPreferences)))
      val page = Future.successful(controller.submitEmailAddressPage(user, FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress))))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select("#form-submit-email-address .error-notification").text shouldBe "Enter a valid email address."
    }

    "show error if the email field is empty" in new Setup {
      val saPreferences = SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.verified)))

      when(mockPreferencesConnector.getPreferences(is(validUtr))(any())).thenReturn(Future.successful(Some(saPreferences)))

      val page = Future.successful(controller.submitEmailAddressPage(user, FakeRequest().withFormUrlEncodedBody(("email.main", ""))))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select("#form-submit-email-address .error-notification").text shouldBe "Enter a valid email address."
    }

    "show error if the two email fields are not equal" in new Setup {
      val emailAddress = "someone@email.com"
      val saPreferences = SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.verified)))

      when(mockPreferencesConnector.getPreferences(is(validUtr))(any())).thenReturn(Future.successful(Some(saPreferences)))

      val page = Future.successful(controller.submitEmailAddressPage(user, FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress),("email.confirm", "other"))))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select("#form-submit-email-address .error-notification").text shouldBe "Check your email addresses - they don’t match."
    }

    "show a warning page if the email has a valid structure but does not pass validation by the email micro service" in new Setup {

      val emailAddress = "someone@dodgy.domain"
      val saPreferences = SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.verified)))

      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(false)
      when(mockPreferencesConnector.getPreferences(is(validUtr))(any())).thenReturn(Future.successful(Some(saPreferences)))

      val page = Future.successful(controller.submitEmailAddressPage(user, FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress),("email.confirm", emailAddress))))

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
      val saPreferences = SaPreference(true, Some(SaEmailPreference("oldEmailAddress@test.com", SaEmailPreference.Status.verified)))

      when(mockPreferencesConnector.getPreferences(is(validUtr))(any())).thenReturn(Future.successful(Some(saPreferences)))
      when(mockPreferencesConnector.savePreferences(is(validUtr), is(true), is(Some(emailAddress)))(any())).thenReturn(Future.successful(None))

      val page = Future.successful(controller.submitEmailAddressPage(user, FakeRequest().withFormUrlEncodedBody
        (("email.main", emailAddress), ("email.confirm", emailAddress), ("emailVerified", "true"))))

      status(page) shouldBe 303
      header("Location", page).get should include(routes.AccountDetailsController.emailAddressChangeThankYou().toString())

      verify(mockPreferencesConnector).savePreferences(is(validUtr), is(true), is(Some(emailAddress)))(any())
      verify(mockPreferencesConnector).getPreferences(is(validUtr))(any())
      verifyNoMoreInteractions(mockPreferencesConnector, mockEmailConnector)
    }

    "if the verified flag is false and the email does not pass validation by the email micro service, display the verify page" in new Setup {

      val emailAddress = "someone@dodgy.domain"
      val saPreferences = SaPreference(true, Some(SaEmailPreference("oldEmailAddress@test.com", SaEmailPreference.Status.verified)))

      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(false)
      when(mockPreferencesConnector.getPreferences(is(validUtr))(any())).thenReturn(Future.successful(Some(saPreferences)))


      val page = Future.successful(controller.submitEmailAddressPage(user, FakeRequest().withFormUrlEncodedBody
        (("email.main", emailAddress), ("email.confirm", emailAddress), ("emailVerified", "false"))))

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") shouldNot be(null)
      document.select("#emailIsCorrectLink") shouldNot be(null)

      verify(mockPreferencesConnector).getPreferences(is(validUtr))(any())
      verifyNoMoreInteractions(mockPreferencesConnector)
      verify(mockEmailConnector).isValid(is(emailAddress))(any())
    }

    "if the verified flag is any value other than true, treat it as false" in new Setup {

      val emailAddress = "someone@dodgy.domain"
      val saPreferences = SaPreference(true, Some(SaEmailPreference("oldEmailAddress@test.com", SaEmailPreference.Status.verified)))

      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(false)
      when(mockPreferencesConnector.getPreferences(is(validUtr))(any())).thenReturn(Future.successful(Some(saPreferences)))


      val page = Future.successful(controller.submitEmailAddressPage(user, FakeRequest().withFormUrlEncodedBody
        (("email.main", emailAddress), ("email.confirm", emailAddress), ("emailVerified", "hjgjhghjghjgj"))))

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") shouldNot be(null)
      document.select("#emailIsCorrectLink") shouldNot be(null)

      verify(mockPreferencesConnector).getPreferences(is(validUtr))(any())
      verifyNoMoreInteractions(mockPreferencesConnector)
      verify(mockEmailConnector).isValid(is(emailAddress))(any())
    }
  }

  "clicking on opt-out of email reminders link in the account details page" should {

    "display the <are you sure> page" in new Setup {
      val saPreferences = SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.verified)))

      when(mockPreferencesConnector.getPreferences(is(validUtr))(any())).thenReturn(Future.successful(Some(saPreferences)))

      val result = controller.optOutOfEmailRemindersPage(user, request)

      status(result) shouldBe 200
      val page = Jsoup.parse(contentAsString(result))

      page.getElementById("confirm-opt-out") shouldNot be(null)
      page.getElementById("confirm-opt-out").text shouldBe "Stop your email reminders"
      page.getElementById("cancel-opt-out-link") shouldNot be(null)
      page.getElementById("cancel-opt-out-link").text shouldBe "Don’t stop your email reminders"
      page.text() should not include "test@test.com"
    }

    "return bad request if the user has not opted into digital" in new Setup{
      val saPreferences = SaPreference(false, None)
      when(mockPreferencesConnector.getPreferences(is(validUtr))(any())).thenReturn(Future.successful(Some(saPreferences)))

      val result = controller.optOutOfEmailRemindersPage(user, request)

      status(result) shouldBe 400
    }
  }

  "A post to confirm opt out of email reminders" should {

    "return a redirect to thank you page" in new Setup {
      val saPreferences = SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.verified)))

      when(mockPreferencesConnector.getPreferences(is(validUtr))(any())).thenReturn(Future.successful(Some(saPreferences)))
      when(mockPreferencesConnector.savePreferences(is(validUtr), is(false), is(None))(any())).thenReturn(Future.successful(()))

      val result = Future.successful(controller.confirmOptOutOfEmailRemindersPage(user, request))

      status(result) shouldBe 303
      header("Location", result).get should include(routes.AccountDetailsController.optedBackIntoPaperThankYou().url)
      val page = Jsoup.parse(contentAsString(result))

      verify(mockPreferencesConnector).savePreferences(is(validUtr), is(false), is(None))(any())
    }

    "return bad request if the user has not opted into digital" in new Setup {
      val saPreferences = SaPreference(false, None)
      when(mockPreferencesConnector.getPreferences(is(validUtr))(any())).thenReturn(Future.successful(Some(saPreferences)))

      val result = Future.successful(controller.confirmOptOutOfEmailRemindersPage(user, request))

      status(result) shouldBe 400
      val page = Jsoup.parse(contentAsString(result))
    }
  }

}
