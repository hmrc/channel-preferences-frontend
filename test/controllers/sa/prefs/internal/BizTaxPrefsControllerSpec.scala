package controllers.sa.prefs.internal

import play.api.test.WithApplication
import play.api.test.FakeRequest
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import uk.gov.hmrc.common.microservice.preferences.{FormattedUri, SaEmailPreference, PreferencesConnector, SaPreference}
import uk.gov.hmrc.common.BaseSpec
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import controllers.common.FrontEndRedirect
import concurrent.Future
import uk.gov.hmrc.common.microservice.sa.domain.SaRoot
import org.jsoup.Jsoup
import uk.gov.hmrc.common.microservice.email.EmailConnector
import controllers.domain.AuthorityUtils._
import scala.Some
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import play.api.test.FakeApplication
import org.mockito.Matchers
import uk.gov.hmrc.common.crypto.Encrypted
import java.net.URI

abstract class BizTaxPrefsControllerSetup extends WithApplication(FakeApplication()) with MockitoSugar {
  val auditConnector = mock[AuditConnector]
  val preferencesConnector = mock[PreferencesConnector]
  val authConnector = mock[AuthConnector]
  val emailConnector = mock[EmailConnector]
  val controller = new BizTaxPrefsController(auditConnector, preferencesConnector, emailConnector)(authConnector)

  val request = FakeRequest()
}

class BizTaxPrefsControllerSpec extends BaseSpec with MockitoSugar {
  import Matchers.{any, eq => is}
  import play.api.test.Helpers._

  val validUtr = SaUtr("1234567890")
  val saRoot = Some(SaRoot(validUtr, Map.empty[String, String]))
  val user = User(userId = "userId", userAuthority = saAuthority("userId", "1234567890"), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(sa = saRoot), decryptedToken = None)

  "The preferences action on login" should {

    "redirect to the homepage when preferences already exist for a specific utr" in new BizTaxPrefsControllerSetup {
      val preferencesAlreadyCreated = SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.verified)))
      when(preferencesConnector.getPreferences(is(validUtr))(any())).thenReturn(Some(preferencesAlreadyCreated))

      val page = Future.successful(controller.redirectToBizTaxOrEmailPrefEntryIfNotSetAction(user, request))

      status(page) shouldBe 303
      header("Location", page).get should include(FrontEndRedirect.businessTaxHome)
    }

    "render an email input field with no value if no email address is supplied" in new BizTaxPrefsControllerSetup {
      when(preferencesConnector.getPreferences(is(validUtr))(any())).thenReturn(None)

      val page = Future.successful(controller.redirectToBizTaxOrEmailPrefEntryIfNotSetAction(user, request))

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))

      document.getElementById("email.main") shouldNot be(null)
      document.getElementById("email.main").attr("value") shouldBe ""
      document.getElementById("email.confirm") shouldNot be(null)
      document.getElementById("email.confirm").attr("value") shouldBe ""
    }

    "include a link to keep paper preference" in new BizTaxPrefsControllerSetup {
      when(preferencesConnector.getPreferences(is(validUtr))(any())).thenReturn(None)
      val page = Future.successful(controller.redirectToBizTaxOrEmailPrefEntryIfNotSetAction(user, request))
      status(page) shouldBe 200
      val document = Jsoup.parse(contentAsString(page))
      document.getElementById("keep-paper-link").attr("value") shouldBe "Continue to get letters"
    }
  }

  "The preferences action on non interstitial page" should {
    "show main banner" in new BizTaxPrefsControllerSetup {

      val page = Future.successful(controller.displayPrefsFormAction(None)(user, request))
      status(page) shouldBe 200
      val document = Jsoup.parse(contentAsString(page))
      document.getElementsByTag("nav").attr("id") shouldBe "proposition-menu"
    }

    "have correct form action to save prefrences" in new BizTaxPrefsControllerSetup {
      val page = Future.successful(controller.displayPrefsFormAction(None)(user, request))
      status(page) shouldBe 200
      val document = Jsoup.parse(contentAsString(page))
      document.select("#form-submit-email-address").attr("action") should endWith("opt-in-email-reminders")
    }

  }

  "The preferences form" should {

    "render an email input field with no value if no email address is supplied" in new BizTaxPrefsControllerSetup {
      val page = Future.successful(controller.displayPrefsFormAction(None)(user, request))

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))

      document.getElementById("email.main").attr("value") shouldBe ""
      document.getElementById("email.confirm").attr("value") shouldBe ""
    }

    "render an email input field populated with the supplied email address" in new BizTaxPrefsControllerSetup {
      val emailAddress = "bob@bob.com"

      val page = Future.successful(controller.displayPrefsFormAction(Some(Encrypted(emailAddress)))(user, request))

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))

      document.getElementById("email.main") shouldNot be(null)
      document.getElementById("email.main").attr("value") shouldBe emailAddress
      document.getElementById("email.confirm") shouldNot be(null)
      document.getElementById("email.confirm").attr("value") shouldBe emailAddress

    }
  }

  "A post to set preferences with no emailVerifiedFlag" should {

    "show an error if the email is incorrectly formatted" in new BizTaxPrefsControllerSetup {
      val emailAddress = "invalid-email"

      val page = Future.successful(controller.submitPrefsFormAction(user, FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress))))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select("#form-submit-email-address .error-notification").text shouldBe "Enter a valid email address."
      verifyZeroInteractions(preferencesConnector, emailConnector)
    }

    "show an error if the email is not set" in new BizTaxPrefsControllerSetup {

      val page = Future.successful(controller.submitPrefsFormAction(user, FakeRequest().withFormUrlEncodedBody(("email.main", ""))))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select("#form-submit-email-address .error-notification").text shouldBe "Enter a valid email address."
      verifyZeroInteractions(preferencesConnector, emailConnector)
    }

    "show error if the two email fields are not equal" in new BizTaxPrefsControllerSetup {
      val emailAddress = "someone@email.com"

      val page = Future.successful(controller.submitPrefsFormAction(user, FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress), ("email.confirm", "other"))))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select("#form-submit-email-address .error-notification").text shouldBe "Check your email addresses - they don’t match."
      verifyZeroInteractions(preferencesConnector, emailConnector)
    }

    "show a warning page if the email has a valid structure but does not pass validation by the email micro service" in new BizTaxPrefsControllerSetup {

      val emailAddress = "someone@dodgy.domain"
      when(emailConnector.validateEmailAddress(is(emailAddress))(any())).thenReturn(false)

      val page = Future.successful(controller.submitPrefsFormAction(user, FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress), ("email.confirm", emailAddress))))

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") shouldNot be(null)
      document.select("#emailIsCorrectLink") shouldNot be(null)
    }

    "validate the email address, save the preference and redirect to the thank you page" in new BizTaxPrefsControllerSetup {
      val emailAddress = "someone@email.com"
      when(emailConnector.validateEmailAddress(is(emailAddress))(any())).thenReturn(true)
      when(preferencesConnector.savePreferences(is(validUtr), is(true), is(Some(emailAddress)))(any())).thenReturn(Future.successful(None))

      val page = Future.successful(controller.submitPrefsFormAction(user, FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress), ("email.confirm", emailAddress))))

      status(page) shouldBe 303
      header("Location", page).get should include(routes.BizTaxPrefsController.thankYou().toString())

      verify(preferencesConnector).savePreferences(is(validUtr), is(true), is(Some(emailAddress)))(any())
      verify(emailConnector).validateEmailAddress(is(emailAddress))(any())
      verifyNoMoreInteractions(preferencesConnector, emailConnector)
    }
  }

  "A post to set preferences with an emailVerifiedFlag" should {

    "if the verified flag is true, save the preference and redirect to the thank you page without verifying the email address again" in new BizTaxPrefsControllerSetup {
      val emailAddress = "someone@email.com"
      when(preferencesConnector.savePreferences(is(validUtr), is(true), is(Some(emailAddress)))(any())).thenReturn(Future.successful(None))

      val page = Future.successful(controller.submitPrefsFormAction(user, FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress), ("email.confirm", emailAddress), ("emailVerified", "true"))))

      status(page) shouldBe 303
      header("Location", page).get should include(routes.BizTaxPrefsController.thankYou().toString())

      verify(preferencesConnector).savePreferences(is(validUtr), is(true), is(Some(emailAddress)))(any())
      verifyNoMoreInteractions(preferencesConnector, emailConnector)
    }

    "if the verified flag is false and the email does not pass validation by the email micro service, display the verify page" in new BizTaxPrefsControllerSetup {

      val emailAddress = "someone@dodgy.domain"
      when(emailConnector.validateEmailAddress(is(emailAddress))(any())).thenReturn(false)

      val page = Future.successful(controller.submitPrefsFormAction(user, FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress), ("email.confirm", emailAddress), ("emailVerified", "false"))))

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") shouldNot be(null)
      document.select("#emailIsCorrectLink") shouldNot be(null)

      verifyZeroInteractions(preferencesConnector)
    }

    "if the verified flag is any value other than true, treat it as false" in new BizTaxPrefsControllerSetup {

      val emailAddress = "someone@dodgy.domain"
      when(emailConnector.validateEmailAddress(is(emailAddress))(any())).thenReturn(false)

      val page = Future.successful(controller.submitPrefsFormAction(user, FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress), ("email.confirm", emailAddress), ("emailVerified", "hjgjhghjghjgj"))))

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") shouldNot be(null)
      document.select("#emailIsCorrectLink") shouldNot be(null)
    }
  }

  "Opting to keep paper notifications" should {

    "save the preference and redirect to the home page" in new BizTaxPrefsControllerSetup {

      when(preferencesConnector.savePreferences(is(validUtr), is(false), is(None))(any())).thenReturn(Future.successful(Some(FormattedUri(URI.create("/someuri")))))

      val page = Future.successful(controller.submitKeepPaperFormAction(user, request))
      status(page) shouldBe 303
      header("Location", page).get should include(FrontEndRedirect.businessTaxHome)

      verify(preferencesConnector).savePreferences(is(validUtr), is(false), is(None))(any())
      verifyNoMoreInteractions(preferencesConnector, emailConnector)
    }
  }
}