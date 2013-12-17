package controllers.bt

import uk.gov.hmrc.common.BaseSpec
import play.api.test.{FakeRequest, FakeApplication, WithApplication}
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.common.microservice.sa.domain.SaRoot
import uk.gov.hmrc.common.microservice.domain.{RegimeRoots, User}
import scala.concurrent.Future
import org.jsoup.Jsoup
import uk.gov.hmrc.common.microservice.preferences.{SaEmailPreference, SaPreference, PreferencesConnector}
import org.mockito.Mockito._
import controllers.common.actions.HeaderCarrier
import uk.gov.hmrc.common.microservice.email.EmailConnector
import play.api.test.Helpers._

abstract class Setup extends WithApplication(FakeApplication()) with MockitoSugar {
  val auditConnector = mock[AuditConnector]
  val authConnector = mock[AuthConnector]
  val mockPreferencesConnector = mock[PreferencesConnector]
  val mockEmailConnector = mock[EmailConnector]
  val controller = new AccountDetailsController(auditConnector, mockPreferencesConnector,mockEmailConnector)(authConnector)

  val request = FakeRequest()
}

class AccountDetailsControllerSpec extends BaseSpec with MockitoSugar  {
  import controllers.domain.AuthorityUtils._

  val validUtr = SaUtr("1234567890")
  val saRoot = Some(SaRoot(validUtr, Map.empty[String, String]))
  val user = User(userId = "userId", userAuthority = saAuthority("userId", "1234567890"), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(sa = saRoot), decryptedToken = None)

  "show account details" should {

    "include Change email address section for SA customer with set email preference" in new Setup {

      val saPreferences = SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.verified)))
      when(mockPreferencesConnector.getPreferences(validUtr)(HeaderCarrier())).thenReturn(Future.successful(Some(saPreferences)))

      val result = Future.successful(controller.accountDetailsPage(user, request))

      status(result) shouldBe 200
      val page = Jsoup.parse(contentAsString(result))
      val changeEmailAddressLink = page.getElementById("change-email-address-link")
      changeEmailAddressLink should not be null
      changeEmailAddressLink.text shouldBe "Change your email address"
      changeEmailAddressLink.attr("href") shouldBe routes.AccountDetailsController.changeEmailAddress(None).url

      val optOutOfEmailLink = page.getElementById("opt-out-of-email-link")
      optOutOfEmailLink should not be null
      optOutOfEmailLink.text shouldBe "Opt-out of email reminders"
      optOutOfEmailLink.attr("href") shouldBe routes.AccountDetailsController.optOutOfEmailReminders.url

      page.getElementById("revalidate-email-link") shouldBe null

      verify(mockPreferencesConnector).getPreferences(validUtr)
    }

    "include Re-validate email address section for SA customer with email preference set as 'bounced'" in new Setup {

      val saPreferences = SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.bounced, Some("User does not exist"))))
      when(mockPreferencesConnector.getPreferences(validUtr)(HeaderCarrier())).thenReturn(Future.successful(Some(saPreferences)))

      val result = Future.successful(controller.accountDetailsPage(user, request))

      status(result) shouldBe 200
      val page = Jsoup.parse(contentAsString(result))
      val revalidateEmailAddressLink = page.getElementById("revalidate-email-link")
      revalidateEmailAddressLink should not be null
      revalidateEmailAddressLink.text shouldBe "Change / re-validate your email address"
      revalidateEmailAddressLink.attr("href") shouldBe routes.AccountDetailsController.changeEmailAddress(None).url

      page.getElementById("opt-out-of-email-link") shouldBe null
      page.getElementById("change-email-address-link") shouldBe null


      verify(mockPreferencesConnector).getPreferences(validUtr)
    }

    "include validation reminder section for SA customer with email preference set as 'pending'" in new Setup {

      val saPreferences = SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.pending)))
      when(mockPreferencesConnector.getPreferences(validUtr)(HeaderCarrier())).thenReturn(Future.successful(Some(saPreferences)))

      val result = Future.successful(controller.accountDetailsPage(user, request))

      status(result) shouldBe 200
      val page = Jsoup.parse(contentAsString(result))
      val revalidateEmailAddressLink = page.getElementById("revalidate-email-link")
      revalidateEmailAddressLink should not be null
      revalidateEmailAddressLink.text shouldBe "Change your email address"
      revalidateEmailAddressLink.attr("href") shouldBe routes.AccountDetailsController.changeEmailAddress(None).url

      val resendEmailButton = page.getElementById("resend-email-button")
      resendEmailButton should not be null
      resendEmailButton.text shouldBe "Resend validation email to your current address"
      val resendForm = page.getElementsByTag("form")
      resendForm.attr("action") shouldBe routes.AccountDetailsController.resendValidationEmail.toString()

      page.getElementById("opt-out-of-email-link") shouldBe null
      page.getElementById("change-email-address-link") shouldBe null

      verify(mockPreferencesConnector).getPreferences(validUtr)
    }

    "not include Change email address section for non-SA customer" in new Setup {

      val nonSaUser = User(userId = "userId", userAuthority = ctAuthority("userId", "1234567890"), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(), decryptedToken = None)

      val result = Future.successful(controller.accountDetailsPage(nonSaUser, request))

      status(result) shouldBe 200
      val page = Jsoup.parse(contentAsString(result))
      page.getElementById("change-email-address-link") shouldBe null
      page.getElementById("opt-out-of-email-link") shouldBe null

      verifyZeroInteractions(mockPreferencesConnector)
    }

    "not include Change email address section for a SA customer who has not yet set his email preference" in new Setup {

      when(mockPreferencesConnector.getPreferences(validUtr)(HeaderCarrier())).thenReturn(Future.successful(None))

      val result = Future.successful(controller.accountDetailsPage(user, request))

      status(result) shouldBe 200
      val page = Jsoup.parse(contentAsString(result))
      page.getElementById("change-email-address-link") shouldBe null

      verify(mockPreferencesConnector).getPreferences(validUtr)
    }

    "not include Change email address section for a SA customer who has opted into paper notification" in new Setup {

      val saPreferences = SaPreference(false, None)
      when(mockPreferencesConnector.getPreferences(validUtr)(HeaderCarrier())).thenReturn(Future.successful(Some(saPreferences)))

      val result = Future.successful(controller.accountDetailsPage(user, request))

      status(result) shouldBe 200
      val page = Jsoup.parse(contentAsString(result))
      page.getElementById("change-email-address-link") shouldBe null

      verify(mockPreferencesConnector).getPreferences(validUtr)
    }
  }

  "clicking on Change email address link in the account details page" should {
    "display update email address form when accessed from Account Details" in new Setup {
      val saPreferences = SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.verified)))
      when(mockPreferencesConnector.getPreferences(validUtr)(HeaderCarrier())).thenReturn(Future.successful(Some(saPreferences)))

      val result = Future.successful(controller.changeEmailAddressPage(None)(user, request))

      status(result) shouldBe 200
      val page = Jsoup.parse(contentAsString(result))

      page.getElementById("current-email-address").text shouldBe "test@test.com"
      page.getElementById("email.main") shouldNot be(null)
      page.getElementById("email.main").attr("value") shouldBe ""
      page.getElementById("email.confirm") shouldNot be(null)
      page.getElementById("email.confirm").attr("value") shouldBe ""
    }


    "display update email address form with the email input field pre-populated when coming back from the warning page" in new Setup {
      val saPreferences = SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.verified)))
      when(mockPreferencesConnector.getPreferences(validUtr)(HeaderCarrier())).thenReturn(Future.successful(Some(saPreferences)))

      val existingEmailAddress = "existing@email.com"
      val result = Future.successful(controller.changeEmailAddressPage(Some(existingEmailAddress))(user, request))

      status(result) shouldBe 200
      val page = Jsoup.parse(contentAsString(result))

      page.getElementById("current-email-address").text shouldBe "test@test.com"
      page.getElementById("email.main") shouldNot be(null)
      page.getElementById("email.main").attr("value") shouldBe existingEmailAddress
      page.getElementById("email.confirm") shouldNot be(null)
      page.getElementById("email.confirm").attr("value") shouldBe existingEmailAddress
    }

    "return bad request if the SA user has opted into paper" in new Setup {

      val saPreferences = SaPreference(false, None)
      when(mockPreferencesConnector.getPreferences(validUtr)(HeaderCarrier())).thenReturn(Future.successful(Some(saPreferences)))

      val result = Future.successful(controller.changeEmailAddressPage(None)(user, request))

      status(result) shouldBe 400
    }
  }

  "Clicking Resend validation email link on account details page" should {

    "call preferences as if opting-in and send the email as a part of the process" in new Setup {

      val saPreferences = SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.pending)))

      when(mockPreferencesConnector.getPreferences(validUtr)).thenReturn(Future.successful(Some(saPreferences)))
      when(mockPreferencesConnector.savePreferences(validUtr, true, Some("test@test.com"))).thenReturn(Future.successful(None))

      val page = Future.successful(controller.resendValidationEmailAction(user, FakeRequest()))

      status(page) shouldBe 303
      header("Location", page).get should include(routes.BusinessTaxController.home.toString())

      verify(mockPreferencesConnector).savePreferences(validUtr, true, Some("test@test.com"))

    }

  }

  "A post to update email address with no emailVerifiedFlag" should {

    "validate the email address, update the address for SA user and redirect to confirmation page" in new Setup {
      val emailAddress = "someone@email.com"
      val saPreferences = SaPreference(true, Some(SaEmailPreference("oldEmailAddress@test.com", SaEmailPreference.Status.verified)))

      when(mockEmailConnector.validateEmailAddress(emailAddress)).thenReturn(true)
      when(mockPreferencesConnector.getPreferences(validUtr)).thenReturn(Future.successful(Some(saPreferences)))
      when(mockPreferencesConnector.savePreferences(validUtr, true, Some(emailAddress))).thenReturn(Future.successful(None))

      val page = Future.successful(controller.submitEmailAddressPage(user, FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress),("email.confirm", emailAddress))))

      status(page) shouldBe 303
      header("Location", page).get should include(routes.AccountDetailsController.emailAddressChangeThankYou().toString())

      verify(mockPreferencesConnector).savePreferences(validUtr, true, Some(emailAddress))
      verify(mockEmailConnector).validateEmailAddress(emailAddress)
      verify(mockPreferencesConnector).getPreferences(validUtr)
      verifyNoMoreInteractions(mockPreferencesConnector, mockEmailConnector)
    }

    "show error if the 2 email address fields do not match" in new Setup {
      val saPreferences = SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.verified)))

      when(mockPreferencesConnector.getPreferences(validUtr)).thenReturn(Future.successful(Some(saPreferences)))

      val page = Future.successful(controller.submitEmailAddressPage(user, FakeRequest().withFormUrlEncodedBody("email.main" -> "a@a.com", "email.confirm" -> "b@b.com")))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select(".error-notification").text shouldBe "The email addresses entered do not match"
      verifyZeroInteractions(mockEmailConnector)
    }

    "show error if the email address is not syntactically valid" in new Setup {
      val emailAddress = "invalid-email"
      val saPreferences = SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.verified)))

      when(mockPreferencesConnector.getPreferences(validUtr)).thenReturn(Future.successful(Some(saPreferences)))
      val page = Future.successful(controller.submitEmailAddressPage(user, FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress))))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select(".error-notification").text shouldBe "Please provide a valid email address"
      verifyZeroInteractions(mockEmailConnector)
    }

    "show error if the email field is empty" in new Setup {
      val saPreferences = SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.verified)))

      when(mockPreferencesConnector.getPreferences(validUtr)).thenReturn(Future.successful(Some(saPreferences)))

      val page = Future.successful(controller.submitEmailAddressPage(user, FakeRequest().withFormUrlEncodedBody(("email.main", ""))))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select(".error-notification").text shouldBe "Please provide a valid email address"
      verifyZeroInteractions(mockEmailConnector)
    }

    "show error if the two email fields are not equal" in new Setup {
      val emailAddress = "someone@email.com"
      val saPreferences = SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.verified)))

      when(mockPreferencesConnector.getPreferences(validUtr)).thenReturn(Future.successful(Some(saPreferences)))

      val page = Future.successful(controller.submitEmailAddressPage(user, FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress),("email.confirm", "other"))))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select(".error-notification").text shouldBe "The email addresses entered do not match"
      verifyZeroInteractions(mockEmailConnector)
    }

    "show a warning page if the email has a valid structure but does not pass validation by the email micro service" in new Setup {

      val emailAddress = "someone@dodgy.domain"
      val saPreferences = SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.verified)))

      when(mockEmailConnector.validateEmailAddress(emailAddress)).thenReturn(false)
      when(mockPreferencesConnector.getPreferences(validUtr)).thenReturn(Future.successful(Some(saPreferences)))

      val page = Future.successful(controller.submitEmailAddressPage(user, FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress),("email.confirm", emailAddress))))

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") shouldNot be(null)
      document.select("#emailIsCorrectLink") shouldNot be(null)

      verify(mockEmailConnector).validateEmailAddress(emailAddress)
    }

  }

  "A post to set preferences with an emailVerifiedFlag" should {

    "if the verified flag is true, save the preference and redirect to the thank you page without verifying the email address again" in new Setup {
      val emailAddress = "someone@email.com"
      val saPreferences = SaPreference(true, Some(SaEmailPreference("oldEmailAddress@test.com", SaEmailPreference.Status.verified)))

      when(mockPreferencesConnector.getPreferences(validUtr)).thenReturn(Future.successful(Some(saPreferences)))
      when(mockPreferencesConnector.savePreferences(validUtr, true, Some(emailAddress))).thenReturn(Future.successful(None))

      val page = Future.successful(controller.submitEmailAddressPage(user, FakeRequest().withFormUrlEncodedBody
        (("email.main", emailAddress), ("email.confirm", emailAddress), ("emailVerified", "true"))))

      status(page) shouldBe 303
      header("Location", page).get should include(routes.AccountDetailsController.emailAddressChangeThankYou().toString())

      verify(mockPreferencesConnector).savePreferences(validUtr, true, Some(emailAddress))
      verify(mockPreferencesConnector).getPreferences(validUtr)
      verifyNoMoreInteractions(mockPreferencesConnector, mockEmailConnector)
    }

    "if the verified flag is false and the email does not pass validation by the email micro service, display the verify page" in new Setup {

      val emailAddress = "someone@dodgy.domain"
      val saPreferences = SaPreference(true, Some(SaEmailPreference("oldEmailAddress@test.com", SaEmailPreference.Status.verified)))

      when(mockEmailConnector.validateEmailAddress(emailAddress)).thenReturn(false)
      when(mockPreferencesConnector.getPreferences(validUtr)).thenReturn(Future.successful(Some(saPreferences)))


      val page = Future.successful(controller.submitEmailAddressPage(user, FakeRequest().withFormUrlEncodedBody
        (("email.main", emailAddress), ("email.confirm", emailAddress), ("emailVerified", "false"))))

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") shouldNot be(null)
      document.select("#emailIsCorrectLink") shouldNot be(null)

      verify(mockPreferencesConnector).getPreferences(validUtr)
      verifyNoMoreInteractions(mockPreferencesConnector)
      verify(mockEmailConnector).validateEmailAddress(emailAddress)
    }

    "if the verified flag is any value other than true, treat it as false" in new Setup {

      val emailAddress = "someone@dodgy.domain"
      val saPreferences = SaPreference(true, Some(SaEmailPreference("oldEmailAddress@test.com", SaEmailPreference.Status.verified)))

      when(mockEmailConnector.validateEmailAddress(emailAddress)).thenReturn(false)
      when(mockPreferencesConnector.getPreferences(validUtr)).thenReturn(Future.successful(Some(saPreferences)))


      val page = Future.successful(controller.submitEmailAddressPage(user, FakeRequest().withFormUrlEncodedBody
        (("email.main", emailAddress), ("email.confirm", emailAddress), ("emailVerified", "hjgjhghjghjgj"))))

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") shouldNot be(null)
      document.select("#emailIsCorrectLink") shouldNot be(null)

      verify(mockPreferencesConnector).getPreferences(validUtr)
      verifyNoMoreInteractions(mockPreferencesConnector)
      verify(mockEmailConnector).validateEmailAddress(emailAddress)
    }
  }

  "clicking on opt-out of email reminders link in the account details page" should {

    "display the <are you sure> page" in new Setup {
      val saPreferences = SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.verified)))

      when(mockPreferencesConnector.getPreferences(validUtr)(HeaderCarrier())).thenReturn(Future.successful(Some(saPreferences)))

      val result = Future.successful(controller.optOutOfEmailRemindersPage(user, request))

      status(result) shouldBe 200
      val page = Jsoup.parse(contentAsString(result))

      page.getElementById("confirm-opt-out") shouldNot be(null)
      page.getElementById("confirm-opt-out").text shouldBe "Opt me out of digital"
      page.getElementById("cancel-opt-out-link") shouldNot be(null)
      page.getElementById("cancel-opt-out-link").text shouldBe "Never mind - I want to stay digital"
    }

    "return bad request if the user has not opted into digital" in new Setup{
      val saPreferences = SaPreference(false, None)
      when(mockPreferencesConnector.getPreferences(validUtr)(HeaderCarrier())).thenReturn(Future.successful(Some(saPreferences)))

      val result = Future.successful(controller.optOutOfEmailRemindersPage(user, request))

      status(result) shouldBe 400
    }
  }

  "A post to confirm opt out of email reminders" should {

    "return a redirect to thank you page" in new Setup {
      val saPreferences = SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.verified)))

      when(mockPreferencesConnector.getPreferences(validUtr)(HeaderCarrier())).thenReturn(Future.successful(Some(saPreferences)))

      val result = Future.successful(controller.confirmOptOutOfEmailRemindersPage(user, request))

      status(result) shouldBe 303
      header("Location", result).get should include(routes.AccountDetailsController.optedBackIntoPaperThankYou().url)
      val page = Jsoup.parse(contentAsString(result))

      verify(mockPreferencesConnector).savePreferences(validUtr, false, None)
    }

    "return bad request if the user has not opted into digital" in new Setup {
      val saPreferences = SaPreference(false, None)
      when(mockPreferencesConnector.getPreferences(validUtr)(HeaderCarrier())).thenReturn(Future.successful(Some(saPreferences)))

      val result = Future.successful(controller.confirmOptOutOfEmailRemindersPage(user, request))

      status(result) shouldBe 400
      val page = Jsoup.parse(contentAsString(result))
    }
  }

}
