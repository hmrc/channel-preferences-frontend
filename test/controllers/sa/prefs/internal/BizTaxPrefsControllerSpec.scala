package controllers.sa.prefs.internal

import connectors.{EmailConnector, PreferencesConnector, SaEmailPreference, SaPreference}
import controllers.common.FrontEndRedirect
import controllers.sa.prefs.AuthorityUtils._
import controllers.sa.prefs.ExternalUrls
import org.jsoup.Jsoup
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, Matchers}
import org.scalatest.mock.MockitoSugar
import play.api.test.{FakeApplication, FakeRequest, WithApplication}
import uk.gov.hmrc.common.microservice.audit.{AuditConnector, AuditEvent}
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.crypto.Encrypted
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.monitoring.EventTypes
import uk.gov.hmrc.test.UnitSpec

import scala.concurrent.Future

abstract class BizTaxPrefsControllerSetup extends WithApplication(FakeApplication()) with MockitoSugar {
  def assignedCohort = InterstitialPageContentCohorts.SignUpForSelfAssesment

  val auditConnector = mock[AuditConnector]
  val preferencesConnector = mock[PreferencesConnector]
  val authConnector = mock[AuthConnector]
  val emailConnector = mock[EmailConnector]
  val controller = new BizTaxPrefsController(auditConnector, preferencesConnector, emailConnector)(authConnector) {
    override def calculateCohort(user: User) = assignedCohort
  }

  val request = FakeRequest()
}

class BizTaxPrefsControllerSpec extends UnitSpec with MockitoSugar {
  import org.mockito.Matchers.{any, eq => is}
  import play.api.test.Helpers._

  val validUtr = SaUtr("1234567890")
  val user = User(userId = "userId", userAuthority = saAuthority("userId", "1234567890"), nameFromGovernmentGateway = Some("Ciccio"), decryptedToken = None)

  "The preferences action on login" should {

    "redirect to BTA when preferences already exist" in new BizTaxPrefsControllerSetup {
      val preferencesAlreadyCreated = SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.verified)))
      when(preferencesConnector.getPreferences(is(validUtr))(any())).thenReturn(Some(preferencesAlreadyCreated))

      val page = Future.successful(controller.redirectToBTAOrInterstitialPageAction(user, request))

      status(page) shouldBe 303
      header("Location", page).get should include(ExternalUrls.businessTaxHome)
    }

    "redirect to interstiatial page for the matching cohort if they have no preference set" in new BizTaxPrefsControllerSetup {
      when(preferencesConnector.getPreferences(is(validUtr))(any())).thenReturn(None)

      val page = controller.redirectToBTAOrInterstitialPageAction(user, request)

      status(page) shouldBe 303
      header("Location", page).get should include(routes.BizTaxPrefsController.displayInterstitialPrefsForm(assignedCohort).url)
    }

  }

  "The preferences interstitial page" should {

    "redirect to BTA when preferences already exist" in new BizTaxPrefsControllerSetup {
      val preferencesAlreadyCreated = SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.verified)))
      when(preferencesConnector.getPreferences(is(validUtr))(any())).thenReturn(Some(preferencesAlreadyCreated))

      val page = controller.displayInterstitialPrefsFormAction(user, request, assignedCohort)

      status(page) shouldBe 303
      header("Location", page).get should include(ExternalUrls.businessTaxHome)
    }

    "render the form in the correct initial state when no preferences exist" in new BizTaxPrefsControllerSetup {
      when(preferencesConnector.getPreferences(is(validUtr))(any())).thenReturn(None)

      val page = controller.displayInterstitialPrefsFormAction(user, request, assignedCohort)

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))

      document.getElementById("email.main") shouldNot be(null)
      document.getElementById("email.main").attr("value") shouldBe ""

      document.getElementById("email.confirm") shouldNot be(null)
      document.getElementById("email.confirm").attr("value") shouldBe ""

      document.getElementById("opt-in-in") shouldNot be(null)
      document.getElementById("opt-in-in").attr("checked") shouldBe ""

      document.getElementById("opt-in-out") shouldNot be(null)
      document.getElementById("opt-in-out").attr("checked") shouldBe ""
    }

    "audit the cohort information for GetSelfAssesment" in new BizTaxPrefsControllerSetup {
      override def assignedCohort = InterstitialPageContentCohorts.GetSelfAssesment
      when(preferencesConnector.getPreferences(is(validUtr))(any())).thenReturn(None)

      val page = controller.displayInterstitialPrefsFormAction(user, request, assignedCohort)
      status(page) shouldBe 200

      val eventArg : ArgumentCaptor[AuditEvent] = ArgumentCaptor.forClass(classOf[AuditEvent])
      verify(auditConnector).audit(eventArg.capture())(any())

      private val value: AuditEvent = eventArg.getValue
      value.auditSource  shouldBe "preferences-frontend"
      value.auditType shouldBe EventTypes.Succeeded
      value.tags should contain ("transactionName" -> "Show Print Preference Option")
      value.detail should contain ("cohort" -> "GetSelfAssesment")
      value.detail should contain ("utr" -> validUtr.value)
    }

    "audit the cohort information for SignUpForSelfAssesment" in new BizTaxPrefsControllerSetup {
      override def assignedCohort = InterstitialPageContentCohorts.SignUpForSelfAssesment
      when(preferencesConnector.getPreferences(is(validUtr))(any())).thenReturn(None)

      val page = controller.displayInterstitialPrefsFormAction(user, request, assignedCohort)
      status(page) shouldBe 200

      val eventArg : ArgumentCaptor[AuditEvent] = ArgumentCaptor.forClass(classOf[AuditEvent])
      verify(auditConnector).audit(eventArg.capture())(any())

      private val value: AuditEvent = eventArg.getValue
      value.auditSource  shouldBe "preferences-frontend"
      value.auditType shouldBe EventTypes.Succeeded
      value.tags should contain ("transactionName" -> "Show Print Preference Option")
      value.detail should contain ("cohort" -> "SignUpForSelfAssesment")
      value.detail should contain ("utr" -> validUtr.value)
    }

  }

  "The preferences action on non interstitial page" should {
    "show main banner" in new BizTaxPrefsControllerSetup {

      val page = controller.displayPrefsFormAction(None, assignedCohort)(user, request)
      status(page) shouldBe 200
      val document = Jsoup.parse(contentAsString(page))
      document.getElementsByTag("nav").attr("id") shouldBe "proposition-menu"
    }

    "have correct form action to save prefrences" in new BizTaxPrefsControllerSetup {
      val page = controller.displayPrefsFormAction(None, assignedCohort)(user, request)
      status(page) shouldBe 200
      val document = Jsoup.parse(contentAsString(page))
      document.select("#form-submit-email-address").attr("action") should endWith("opt-in-email-reminders")
    }
  }

  "The preferences form" should {

    "render an email input field with no value if no email address is supplied, and no option selected" in new BizTaxPrefsControllerSetup {
      val page = controller.displayPrefsFormAction(None, assignedCohort)(user, request)

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))

      document.getElementById("email.main").attr("value") shouldBe ""
      document.getElementById("email.confirm").attr("value") shouldBe ""
      document.getElementById("opt-in-in").attr("checked") should be (empty)
      document.getElementById("opt-in-out").attr("checked") should be (empty)
    }

    "render an email input field populated with the supplied email address, and the Opt-in option selected" in new BizTaxPrefsControllerSetup {
      val emailAddress = "bob@bob.com"

      val page = controller.displayPrefsFormAction(Some(Encrypted(EmailAddress(emailAddress))), assignedCohort)(user, request)

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))

      document.getElementById("email.main") shouldNot be(null)
      document.getElementById("email.main").attr("value") shouldBe emailAddress
      document.getElementById("email.confirm") shouldNot be(null)
      document.getElementById("email.confirm").attr("value") shouldBe emailAddress
      document.getElementById("opt-in-in").attr("checked") should be ("checked")
      document.getElementById("opt-in-out").attr("checked") should be (empty)
    }
  }

  "A post to set preferences with no emailVerifiedFlag" should {

    "show an error if no opt-in preference has been chosen" in new BizTaxPrefsControllerSetup {
      val page = Future.successful(controller.submitPrefsFormAction(user, FakeRequest().withFormUrlEncodedBody()))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select(".error-notification").text shouldBe "Confirm if you want Self Assessment email reminders"
      verifyZeroInteractions(preferencesConnector, emailConnector)
    }

    "show an error when opting-in if the email is incorrectly formatted" in new BizTaxPrefsControllerSetup {
      val emailAddress = "invalid-email"

      val page = Future.successful(controller.submitPrefsFormAction(user, FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", "email.main" -> emailAddress)))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select("#form-submit-email-address .error-notification").text shouldBe "Enter a valid email address."
      verifyZeroInteractions(preferencesConnector, emailConnector)
    }

    "show an error when opting-in if the email is not set" in new BizTaxPrefsControllerSetup {

      val page = Future.successful(controller.submitPrefsFormAction(user, FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", "email.main" -> "")))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select(".error-notification").text shouldBe "As you would like to opt in, please enter an email address."
      verifyZeroInteractions(preferencesConnector, emailConnector)
    }

    "show an error when opting-in if the two email fields are not equal" in new BizTaxPrefsControllerSetup {
      val emailAddress = "someone@email.com"

      val page = Future.successful(controller.submitPrefsFormAction(user, FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", "email.main" -> emailAddress, "email.confirm" -> "other")))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select("#form-submit-email-address .error-notification").text shouldBe "Check your email addresses - they don’t match."
      verifyZeroInteractions(preferencesConnector, emailConnector)
    }

    "show a warning page when opting-in if the email has a valid structure but does not pass validation by the email micro service" in new BizTaxPrefsControllerSetup {

      val emailAddress = "someone@dodgy.domain"
      when(emailConnector.isValid(is(emailAddress))(any())).thenReturn(false)

      val page = Future.successful(controller.submitPrefsFormAction(user, FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress),("email.confirm", emailAddress))))

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") shouldNot be(null)
      document.select("#emailIsCorrectLink") shouldNot be(null)
    }

    "when opting-in, validate the email address, save the preference and redirect to the thank you page" in new BizTaxPrefsControllerSetup {
      val emailAddress = "someone@email.com"
      when(emailConnector.isValid(is(emailAddress))(any())).thenReturn(true)
      when(preferencesConnector.savePreferences(is(validUtr), is(true), is(Some(emailAddress)))(any())).thenReturn(Future.successful(None))

      val page = Future.successful(controller.submitPrefsFormAction(user, FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress),("email.confirm", emailAddress))))

      status(page) shouldBe 303
      header("Location", page).get should include(routes.BizTaxPrefsController.thankYou().toString())

      verify(preferencesConnector).savePreferences(is(validUtr), is(true), is(Some(emailAddress)))(any())
      verify(emailConnector).isValid(is(emailAddress))(any())
      verifyNoMoreInteractions(preferencesConnector, emailConnector)
    }

    "when opting-out, save the preference and redirect to the thank you page" in new BizTaxPrefsControllerSetup {
      when(preferencesConnector.savePreferences(is(validUtr), is(false), is(None))(any())).thenReturn(Future.successful(None))

      val page = Future.successful(controller.submitPrefsFormAction(user, FakeRequest().withFormUrlEncodedBody("opt-in" -> "false")))

      status(page) shouldBe 303
      header("Location", page).get should include(FrontEndRedirect.businessTaxHome)

      verify(preferencesConnector).savePreferences(is(validUtr), is(false), is(None))(any())
      verifyNoMoreInteractions(preferencesConnector, emailConnector)
    }
  }

  "A post to set preferences with an emailVerifiedFlag" should {

    "if the verified flag is true, save the preference and redirect to the thank you page without verifying the email address again" in new BizTaxPrefsControllerSetup {
      val emailAddress = "someone@email.com"
      when(preferencesConnector.savePreferences(is(validUtr), is(true), is(Some(emailAddress)))(any())).thenReturn(Future.successful(None))

      val page = Future.successful(controller.submitPrefsFormAction(user, FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), ("email.confirm", emailAddress), ("emailVerified", "true"))))

      status(page) shouldBe 303
      header("Location", page).get should include(routes.BizTaxPrefsController.thankYou().toString())

      verify(preferencesConnector).savePreferences(is(validUtr), is(true), is(Some(emailAddress)))(any())
      verifyNoMoreInteractions(preferencesConnector, emailConnector)
    }

    "if the verified flag is false and the email does not pass validation by the email micro service, display the verify page" in new BizTaxPrefsControllerSetup {

      val emailAddress = "someone@dodgy.domain"
      when(emailConnector.isValid(is(emailAddress))(any())).thenReturn(false)

      val page = Future.successful(controller.submitPrefsFormAction(user, FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), ("email.confirm", emailAddress), ("emailVerified", "false"))))

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") shouldNot be(null)
      document.select("#emailIsCorrectLink") shouldNot be(null)

      verifyZeroInteractions(preferencesConnector)
    }

    "if the verified flag is any value other than true, treat it as false" in new BizTaxPrefsControllerSetup {

      val emailAddress = "someone@dodgy.domain"
      when(emailConnector.isValid(is(emailAddress))(any())).thenReturn(false)

      val page = Future.successful(controller.submitPrefsFormAction(user, FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), ("email.confirm", emailAddress), ("emailVerified", "hjgjhghjghjgj"))))

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") shouldNot be(null)
      document.select("#emailIsCorrectLink") shouldNot be(null)
    }
  }

  "An audit event" should {
    "be created when submitting a print preference from GetSelfAssesment" in new BizTaxPrefsControllerSetup {

      override def assignedCohort = InterstitialPageContentCohorts.GetSelfAssesment

      val emailAddress = "someone@email.com"
      when(emailConnector.isValid(is(emailAddress))(any())).thenReturn(true)
      when(preferencesConnector.savePreferences(is(validUtr), is(true), is(Some(emailAddress)))(any())).thenReturn(Future.successful(None))

      val page = Future.successful(controller.submitPrefsFormAction(user, FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress),("email.confirm", emailAddress))))

      status(page) shouldBe 303

      val eventArg : ArgumentCaptor[AuditEvent] = ArgumentCaptor.forClass(classOf[AuditEvent])
      verify(auditConnector).audit(eventArg.capture())(any())

      private val value: AuditEvent = eventArg.getValue
      value.auditSource  shouldBe "preferences-frontend"
      value.auditType shouldBe EventTypes.Succeeded
      value.tags should contain ("transactionName" -> "Set Print Preference")
      value.detail should contain ("cohort" -> "GetSelfAssesment")
      value.detail should contain ("utr" -> validUtr.value)
      value.detail should contain ("email" -> "someone@email.com")
      value.detail should contain ("digital" -> "true")

    }
    "be created when submitting a print preference from SignUpForSelfAssesment" in new BizTaxPrefsControllerSetup {

      override def assignedCohort = InterstitialPageContentCohorts.SignUpForSelfAssesment
      val emailAddress = "someone@email.com"
      when(emailConnector.isValid(is(emailAddress))(any())).thenReturn(true)
      when(preferencesConnector.savePreferences(is(validUtr), is(true), is(Some(emailAddress)))(any())).thenReturn(Future.successful(None))

      val page = Future.successful(controller.submitPrefsFormAction(user, FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress),("email.confirm", emailAddress))))

      status(page) shouldBe 303

      val eventArg : ArgumentCaptor[AuditEvent] = ArgumentCaptor.forClass(classOf[AuditEvent])
      verify(auditConnector).audit(eventArg.capture())(any())

      private val value: AuditEvent = eventArg.getValue
      value.auditSource  shouldBe "preferences-frontend"
      value.auditType shouldBe EventTypes.Succeeded
      value.tags should contain ("transactionName" -> "Set Print Preference")
      value.detail should contain ("cohort" -> "SignUpForSelfAssesment")
      value.detail should contain ("utr" -> validUtr.value)
      value.detail should contain ("email" -> "someone@email.com")
      value.detail should contain ("digital" -> "true")

    }

    "be created when choosing to not accept email reminders from GetSelfAssesment" in new BizTaxPrefsControllerSetup {

      override def assignedCohort = InterstitialPageContentCohorts.GetSelfAssesment
      when(preferencesConnector.savePreferences(is(validUtr), is(false), is(None))(any())).thenReturn(Future.successful(None))

      val page = Future.successful(controller.submitPrefsFormAction(user, FakeRequest().withFormUrlEncodedBody("opt-in" -> "false")))

      status(page) shouldBe 303

      val eventArg : ArgumentCaptor[AuditEvent] = ArgumentCaptor.forClass(classOf[AuditEvent])
      verify(auditConnector).audit(eventArg.capture())(any())

      private val value: AuditEvent = eventArg.getValue
      value.auditSource  shouldBe "preferences-frontend"
      value.auditType shouldBe EventTypes.Succeeded
      value.tags should contain ("transactionName" -> "Set Print Preference")
      value.detail should contain ("cohort" -> "GetSelfAssesment")
      value.detail should contain ("utr" -> validUtr.value)
      value.detail should not contain ("email" -> "someone@email.com")
      value.detail should contain ("digital" -> "false")

    }

    "be created when choosing to not accept email reminders from SignUpForSelfAssesment" in new BizTaxPrefsControllerSetup {

      override def assignedCohort = InterstitialPageContentCohorts.SignUpForSelfAssesment
      when(preferencesConnector.savePreferences(is(validUtr), is(false), is(None))(any())).thenReturn(Future.successful(None))

      val page = Future.successful(controller.submitPrefsFormAction(user, FakeRequest().withFormUrlEncodedBody("opt-in" -> "false")))

      status(page) shouldBe 303

      val eventArg : ArgumentCaptor[AuditEvent] = ArgumentCaptor.forClass(classOf[AuditEvent])
      verify(auditConnector).audit(eventArg.capture())(any())

      private val value: AuditEvent = eventArg.getValue
      value.auditSource  shouldBe "preferences-frontend"
      value.auditType shouldBe EventTypes.Succeeded
      value.tags should contain ("transactionName" -> "Set Print Preference")
      value.detail should contain ("cohort" -> "SignUpForSelfAssesment")
      value.detail should contain ("utr" -> validUtr.value)
      value.detail should not contain ("email" -> "someone@email.com")
      value.detail should contain ("digital" -> "false")

    }
  }
}