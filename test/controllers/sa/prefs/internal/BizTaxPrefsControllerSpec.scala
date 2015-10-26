package controllers.sa.prefs.internal

import connectors._
import controllers.sa.prefs.Encrypted
import controllers.sa.prefs.AuthorityUtils._
import controllers.sa.prefs.ExternalUrls
import controllers.sa.prefs.internal.EmailOptInJourney._
import helpers.{TestFixtures, ConfigHelper}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.JsString
import play.api.test.{FakeRequest, WithApplication}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{EventTypes, ExtendedDataEvent}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future


abstract class BizTaxPrefsControllerSetup extends WithApplication(ConfigHelper.fakeApp) with MockitoSugar {
  def assignedCohort: OptInCohort = IPage

  val mockAuditConnector = mock[AuditConnector]
  val mockPreferencesConnector = mock[PreferencesConnector]
  val mockAuthConnector = mock[AuthConnector]
  val mockEmailConnector = mock[EmailConnector]

  val controller = new BizTaxPrefsController {

    def calculateCohort(user: AuthContext) = assignedCohort

    override def preferencesConnector: PreferencesConnector = mockPreferencesConnector

    override def emailConnector: EmailConnector = mockEmailConnector

    override def auditConnector: AuditConnector = mockAuditConnector

    override protected implicit def authConnector: AuthConnector = mockAuthConnector
  }

  val request = FakeRequest()

  when(mockPreferencesConnector.saveCohort(any(), any())(any())).thenReturn(Future.successful(()))
}

class BizTaxPrefsControllerSpec extends UnitSpec with MockitoSugar {
  import org.mockito.Matchers.{any, eq => is}
  import play.api.test.Helpers._

  val validUtr = SaUtr("1234567890")
  val user = AuthContext(authority = saAuthority("userId", "1234567890"), nameFromSession = Some("Ciccio"), governmentGatewayToken = None)

  "The preferences action on login" should {

    "redirect to BTA when preferences already exist" in new BizTaxPrefsControllerSetup {
      val preferencesAlreadyCreated = SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.Verified)))
      when(mockPreferencesConnector.getPreferences(is(validUtr), any())(any())).thenReturn(Some(preferencesAlreadyCreated))

      val page = Future.successful(controller.redirectToBTAOrInterstitialPageAction(user, request))

      status(page) shouldBe 303
      header("Location", page).get should include(ExternalUrls.businessTaxHome)
    }

    "redirect to interstitial page for the matching cohort if they have no preference set" in new BizTaxPrefsControllerSetup {
      when(mockPreferencesConnector.getPreferences(is(validUtr), any())(any())).thenReturn(None)

      val page = controller.redirectToBTAOrInterstitialPageAction(user, request)

      status(page) shouldBe 303
      header("Location", page).get should include(routes.DeprecatedYTABizTaxPrefsController.displayInterstitialPrefsFormForCohort(Some(assignedCohort)).url)
    }

    "redirect to interstitial page for the matching cohort if they are currently opted out" in new BizTaxPrefsControllerSetup {
      val preferencesAlreadyCreated = SaPreference(false, None)
      when(mockPreferencesConnector.getPreferences(is(validUtr), any())(any())).thenReturn(Some(preferencesAlreadyCreated))

      val page = controller.redirectToBTAOrInterstitialPageAction(user, request)

      status(page) shouldBe 303
      header("Location", page).get should include(routes.DeprecatedYTABizTaxPrefsController.displayInterstitialPrefsFormForCohort(Some(assignedCohort)).url)
    }

  }

  "The preferences interstitial page" should {

    "redirect to BTA when preferences already exist" in new BizTaxPrefsControllerSetup {
      val preferencesAlreadyCreated = SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.Verified)))
      when(mockPreferencesConnector.getPreferences(is(validUtr), any())(any())).thenReturn(Some(preferencesAlreadyCreated))

      val page = controller.displayInterstitialPrefsFormAction(user, request, Some(assignedCohort), TestFixtures.sampleHostContext)

      status(page) shouldBe 303
      header("Location", page).get should include(ExternalUrls.businessTaxHome)
    }

    "redirect to a re-calculated cohort when no cohort is supplied" in new BizTaxPrefsControllerSetup {
      when(mockPreferencesConnector.getPreferences(is(validUtr), any())(any())).thenReturn(None)

      val page = controller.displayInterstitialPrefsFormAction(user, request, possibleCohort = None, TestFixtures.sampleHostContext)

      status(page) shouldBe 303
      header("Location", page).get should be (routes.DeprecatedYTABizTaxPrefsController.displayInterstitialPrefsFormForCohort(Some(assignedCohort)).url)
    }

    "render the form in the correct initial state when no preferences exist" in new BizTaxPrefsControllerSetup {
      when(mockPreferencesConnector.getPreferences(is(validUtr), any())(any())).thenReturn(None)

      val page = controller.displayInterstitialPrefsFormAction(user, request, Some(assignedCohort), TestFixtures.sampleHostContext)

      status(page) shouldBe 200

      allGoPaperlessFormElementsArePresent(Jsoup.parse(contentAsString(page)))
    }

    "render the form in the correct initial state when user is currently opted out" in new BizTaxPrefsControllerSetup {
      when(mockPreferencesConnector.getPreferences(is(validUtr), any())(any())).thenReturn(Some(SaPreference(false, None)))

      val page = controller.displayInterstitialPrefsFormAction(user, request, Some(assignedCohort), TestFixtures.sampleHostContext)

      status(page) shouldBe 200

      allGoPaperlessFormElementsArePresent(Jsoup.parse(contentAsString(page)))
    }

    "audit the cohort information for IPage" in new BizTaxPrefsControllerSetup {
      override def assignedCohort = IPage
      when(mockPreferencesConnector.getPreferences(is(validUtr), any())(any())).thenReturn(None)

      val page = controller.displayInterstitialPrefsFormAction(user, request, Some(assignedCohort), TestFixtures.sampleHostContext)
      status(page) shouldBe 200

      val eventArg : ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      verify(mockAuditConnector).sendEvent(eventArg.capture())(any(), any())

      private val value: ExtendedDataEvent = eventArg.getValue
      value.auditSource  shouldBe "preferences-frontend"
      value.auditType shouldBe EventTypes.Succeeded
      value.tags should contain ("transactionName" -> "Show Print Preference Option")
      value.detail \ "cohort" shouldBe JsString("IPage")
      value.detail \ "journey" shouldBe JsString("Interstitial")
      value.detail \ "utr" shouldBe JsString(validUtr.value)
    }
  }

  def allGoPaperlessFormElementsArePresent(document: Document) {
    document.getElementById("email.main") shouldNot be(null)
    document.getElementById("email.main").attr("value") shouldBe ""

    document.getElementById("email.confirm") shouldNot be(null)
    document.getElementById("email.confirm").attr("value") shouldBe ""

    document.getElementById("opt-in-in") shouldNot be(null)
    document.getElementById("opt-in-in").attr("checked") shouldBe "checked"

    document.getElementById("opt-in-out") shouldNot be(null)
    document.getElementById("opt-in-out").attr("checked") shouldBe ""

    document.getElementById("terms-and-conditions").attr("href") should endWith("terms-and-conditions")
  }

  "The terms and conditions page" should {

    "contain correct content" in new BizTaxPrefsControllerSetup {
      val page = controller.termsAndConditionsPage()(request, user)

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))

      document.getElementById("success-heading").text() shouldBe "Self Assessment terms and conditions"

      document.getElementById("secure-mailbox") shouldNot be(null)
      document.getElementById("statutory") shouldNot be(null)
    }

    "contain correct contents navigation panel" in new BizTaxPrefsControllerSetup {
      val page = controller.termsAndConditionsPage()(request, user)

      status(page) shouldBe 200
      val document = Jsoup.parse(contentAsString(page))

      document.getElementById("secure-mailbox-link").attr("href") should be ("#secure-mailbox")
      document.getElementById("statutory-link").attr("href") should be ("#statutory")
    }

    "link to full terms and conditions page" in new BizTaxPrefsControllerSetup {
      val page = controller.termsAndConditionsPage()(request, user)

      status(page) shouldBe 200
      val document = Jsoup.parse(contentAsString(page))

      document.getElementById("full-terms-link").attr("href") should be ("https://online.hmrc.gov.uk/information/terms")
    }
  }

  "The preferences action on non interstitial page" should {
    "show main banner" in new BizTaxPrefsControllerSetup {

      val page = controller.displayPrefsFormAction(None, Some(assignedCohort))(user, request, TestFixtures.sampleHostContext)
      status(page) shouldBe 200
      val document = Jsoup.parse(contentAsString(page))
      document.getElementsByTag("nav").attr("id") shouldBe "proposition-menu"
    }

    "have correct form action to save preferences" in new BizTaxPrefsControllerSetup {
      val page = controller.displayPrefsFormAction(None, Some(assignedCohort))(user, request, TestFixtures.sampleHostContext)
      status(page) shouldBe 200
      val document = Jsoup.parse(contentAsString(page))
      document.select("#form-submit-email-address").attr("action") should endWith(routes.BizTaxPrefsController.submitPrefsFormForNonInterstitial(TestFixtures.sampleHostContext).url)
    }

    "audit the cohort information for the account details page" in new BizTaxPrefsControllerSetup {
      val page = controller.displayPrefsFormAction(None, Some(assignedCohort))(user, request, TestFixtures.sampleHostContext)
      status(page) shouldBe 200

      val eventArg : ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      verify(mockAuditConnector).sendEvent(eventArg.capture())(any(), any())

      private val value: ExtendedDataEvent = eventArg.getValue
      value.auditSource  shouldBe "preferences-frontend"
      value.auditType shouldBe EventTypes.Succeeded
      value.tags should contain ("transactionName" -> "Show Print Preference Option")
      value.detail \ "cohort" shouldBe JsString(assignedCohort.toString)
      value.detail \ "journey" shouldBe JsString("AccountDetails")
      value.detail \ "utr" shouldBe JsString(validUtr.value)
    }

    "redirect to a re-calculated cohort when no cohort is supplied" in new BizTaxPrefsControllerSetup {
      when(mockPreferencesConnector.getPreferences(is(validUtr), any())(any())).thenReturn(None)

      val page = controller.displayPrefsFormAction(emailAddress = None, possibleCohort = None)(user, request, TestFixtures.sampleHostContext)

      status(page) shouldBe 303
      header("Location", page).get should be (routes.BizTaxPrefsController.displayPrefsFormForCohort(Some(assignedCohort), None, TestFixtures.sampleHostContext).url)
    }
  }

  "The preferences form" should {

    "render an email input field with no value if no email address is supplied, and no option selected" in new BizTaxPrefsControllerSetup {
      val page = controller.displayPrefsFormAction(None, Some(assignedCohort))(user, request, TestFixtures.sampleHostContext)

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))

      document.getElementById("email.main").attr("value") shouldBe ""
      document.getElementById("email.confirm").attr("value") shouldBe ""
      document.getElementById("opt-in-in").attr("checked") should be ("checked")
      document.getElementById("opt-in-out").attr("checked") should be (empty)
    }

    "render an email input field populated with the supplied email address, and the Opt-in option selected" in new BizTaxPrefsControllerSetup {
      val emailAddress = "bob@bob.com"

      val page = controller.displayPrefsFormAction(Some(Encrypted(EmailAddress(emailAddress))), Some(assignedCohort))(user, request, TestFixtures.sampleHostContext)

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
      val page = Future.successful(controller.submitPrefsFormAction(AccountDetails)(user, FakeRequest().withFormUrlEncodedBody(), hostContext = TestFixtures.sampleHostContext))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select(".error-notification").text shouldBe "Confirm if you want paperless notifications"
      verifyZeroInteractions(mockPreferencesConnector, mockEmailConnector)
    }

    "show an error when opting-in if the email is incorrectly formatted" in new BizTaxPrefsControllerSetup {
      val emailAddress = "invalid-email"

      val page = Future.successful(controller.submitPrefsFormAction(AccountDetails)(user, FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", "email.main" -> emailAddress), hostContext = TestFixtures.sampleHostContext))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select("#form-submit-email-address .error-notification").text shouldBe "Enter a valid email address. You must accept the terms and conditions"
      verifyZeroInteractions(mockPreferencesConnector, mockEmailConnector)
    }

    "show an error when opting-in if the T&C's are not accepted" in new BizTaxPrefsControllerSetup {
      override def assignedCohort = IPage

      val emailAddress = "someone@email.com"
      val page = Future.successful(controller.submitPrefsFormAction(AccountDetails)(user,
        FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", "email.main" -> emailAddress, "email.confirm" -> emailAddress, "accept-tc" -> "false"), hostContext = TestFixtures.sampleHostContext))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select(".error-notification").text shouldBe "You must accept the terms and conditions"
      verifyZeroInteractions(mockPreferencesConnector, mockEmailConnector)
    }

    "show an error when opting-in if the T&C's accepted flag is not present" in new BizTaxPrefsControllerSetup {
      override def assignedCohort = IPage

      val emailAddress = "someone@email.com"
      val page = Future.successful(controller.submitPrefsFormAction(AccountDetails)(user,
        FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", "email.main" -> emailAddress, "email.confirm" -> emailAddress), hostContext = TestFixtures.sampleHostContext))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select(".error-notification").text shouldBe "You must accept the terms and conditions"
      verifyZeroInteractions(mockPreferencesConnector, mockEmailConnector)
    }

    "show an error when opting-in if the email is not set" in new BizTaxPrefsControllerSetup {

      val page = Future.successful(controller.submitPrefsFormAction(AccountDetails)(user, FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", "email.main" -> "", "accept-tc" -> "true"), hostContext = TestFixtures.sampleHostContext))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select(".error-notification").text shouldBe "As you would like to opt in, please enter an email address."
      verifyZeroInteractions(mockPreferencesConnector, mockEmailConnector)
    }

    "show an error when opting-in if the two email fields are not equal" in new BizTaxPrefsControllerSetup {
      val emailAddress = "someone@email.com"

      val page = Future.successful(controller.submitPrefsFormAction(AccountDetails)(user, FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", "email.main" -> emailAddress, "email.confirm" -> "other", "accept-tc" -> "true"), hostContext = TestFixtures.sampleHostContext))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select("#form-submit-email-address .error-notification").text shouldBe "Check your email addresses - they don't match."
      verifyZeroInteractions(mockPreferencesConnector, mockEmailConnector)
    }

    "show a warning page when opting-in if the email has a valid structure but does not pass validation by the email micro service" in new BizTaxPrefsControllerSetup {

      val emailAddress = "someone@dodgy.domain"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(false)

      val page = Future.successful(controller.submitPrefsFormAction(AccountDetails)(user, FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress),("email.confirm", emailAddress), "accept-tc" -> "true"), hostContext = TestFixtures.sampleHostContext))

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") shouldNot be(null)
      document.select("#emailIsCorrectLink") shouldNot be(null)
    }

    "when opting-in, validate the email address, save the preference and redirect to the thank you page with the email address encrpyted" in new BizTaxPrefsControllerSetup {
      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(true)
      when(mockPreferencesConnector.addTermsAndConditions(is(validUtr), is(Generic -> TermsAccepted(true)), is(Some(emailAddress)))(any())).thenReturn(Future.successful(true))
      when(mockPreferencesConnector.activateUser(is(validUtr), anyString)(any())).thenReturn(Future.successful(true))

      val page = Future.successful(controller.submitPrefsFormAction(AccountDetails)(user, FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress),("email.confirm", emailAddress), "accept-tc" -> "true"), hostContext = TestFixtures.sampleHostContext))

      status(page) shouldBe 303
      header("Location", page).get should include(routes.DeprecatedYTABizTaxPrefsController.thankYou(Some(Encrypted(EmailAddress(emailAddress)))).toString())

      verify(mockPreferencesConnector).saveCohort(is(validUtr), is(assignedCohort))(any())
      verify(mockPreferencesConnector).addTermsAndConditions(is(validUtr), is(Generic -> TermsAccepted(true)), is(Some(emailAddress)))(any())
      verify(mockPreferencesConnector).activateUser(is(validUtr), anyString)(any())
      verify(mockEmailConnector).isValid(is(emailAddress))(any())

      verifyNoMoreInteractions(mockPreferencesConnector, mockEmailConnector)
    }

    "when opting-in, validate the email address, failed to save the preference and so not activate user and redirect to the thank you page with the email address encrpyted" in new BizTaxPrefsControllerSetup {
      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(true)
      when(mockPreferencesConnector.addTermsAndConditions(is(validUtr), is(Generic -> TermsAccepted(true)), is(Some(emailAddress)))(any())).thenReturn(Future.successful(false))

      val page = Future.successful(controller.submitPrefsFormAction(AccountDetails)(user, FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress),("email.confirm", emailAddress), "accept-tc" -> "true"), hostContext = TestFixtures.sampleHostContext))

      status(page) shouldBe 303
      header("Location", page).get should include(routes.DeprecatedYTABizTaxPrefsController.thankYou(Some(Encrypted(EmailAddress(emailAddress)))).toString())

      verify(mockPreferencesConnector).saveCohort(is(validUtr), is(assignedCohort))(any())
      verify(mockPreferencesConnector).addTermsAndConditions(is(validUtr), is(Generic -> TermsAccepted(true)), is(Some(emailAddress)))(any())
      verify(mockPreferencesConnector, times(0)).activateUser(is(validUtr), anyString)(any())
      verify(mockEmailConnector).isValid(is(emailAddress))(any())

      verifyNoMoreInteractions(mockPreferencesConnector, mockEmailConnector)
    }

    "when opting-out, save the preference and redirect to the thank you page" in new BizTaxPrefsControllerSetup {
      when(mockPreferencesConnector.addTermsAndConditions(is(validUtr), is(Generic -> TermsAccepted(false)), is(None))(any())).thenReturn(Future.successful(true))
      when(mockPreferencesConnector.activateUser(is(validUtr), anyString)(any())).thenReturn(Future.successful(true))
      val page = Future.successful(controller.submitPrefsFormAction(AccountDetails)(user, FakeRequest().withFormUrlEncodedBody("opt-in" -> "false"), hostContext = TestFixtures.sampleHostContext))

      status(page) shouldBe 303
      header("Location", page).get should include(ExternalUrls.businessTaxHome)

      verify(mockPreferencesConnector).saveCohort(is(validUtr), is(assignedCohort))(any())
      verify(mockPreferencesConnector).addTermsAndConditions(is(validUtr), is(Generic -> TermsAccepted(false)), is(None))(any())
      verify(mockPreferencesConnector, times(0)).activateUser(is(validUtr), anyString)(any())

      verifyNoMoreInteractions(mockPreferencesConnector, mockEmailConnector)
    }
  }

  "A post to set preferences with an emailVerifiedFlag" should {

    "if the verified flag is true, save the preference and redirect to the thank you page without verifying the email address again" in new BizTaxPrefsControllerSetup {
      val emailAddress = "someone@email.com"
      when(mockPreferencesConnector.addTermsAndConditions(is(validUtr), is(Generic -> TermsAccepted(true)), is(Some(emailAddress)))(any())).thenReturn(Future.successful(true))
      when(mockPreferencesConnector.activateUser(is(validUtr), anyString)(any())).thenReturn(Future.successful(true))

      val page = Future.successful(controller.submitPrefsFormAction(AccountDetails)(user, FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), ("email.confirm", emailAddress), ("emailVerified", "true"), "accept-tc" -> "true"), hostContext = TestFixtures.sampleHostContext))

      status(page) shouldBe 303
      header("Location", page).get should include(routes.DeprecatedYTABizTaxPrefsController.thankYou(Some(Encrypted(EmailAddress(emailAddress)))).toString())

      verify(mockPreferencesConnector).saveCohort(is(validUtr), is(assignedCohort))(any())
      verify(mockPreferencesConnector).addTermsAndConditions(is(validUtr), is(Generic -> TermsAccepted(true)), is(Some(emailAddress)))(any())
      verify(mockPreferencesConnector).activateUser(is(validUtr), anyString)(any())

      verifyNoMoreInteractions(mockPreferencesConnector, mockEmailConnector)
    }

    "if the verified flag is false and the email does not pass validation by the email micro service, display the verify page" in new BizTaxPrefsControllerSetup {

      val emailAddress = "someone@dodgy.domain"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(false)

      val page = Future.successful(controller.submitPrefsFormAction(AccountDetails)(user, FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), ("email.confirm", emailAddress), ("emailVerified", "false"), "accept-tc" -> "true"), hostContext = TestFixtures.sampleHostContext))

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") shouldNot be(null)
      document.select("#emailIsCorrectLink") shouldNot be(null)

      verifyZeroInteractions(mockPreferencesConnector)
    }

    "if the verified flag is any value other than true, treat it as false" in new BizTaxPrefsControllerSetup {

      val emailAddress = "someone@dodgy.domain"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(false)

      val page = Future.successful(controller.submitPrefsFormAction(AccountDetails)(user, FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), ("email.confirm", emailAddress), ("emailVerified", "hjgjhghjghjgj"), "accept-tc" -> "true"), hostContext = TestFixtures.sampleHostContext))

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") shouldNot be(null)
      document.select("#emailIsCorrectLink") shouldNot be(null)
    }
  }

  "An audit event" should {
    "be created as EventTypes.Succeeded when the user is successfully opted in and activated on submitting a print preference from IPage" in new BizTaxPrefsControllerSetup {

      override def assignedCohort = IPage
      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(true)
      when(mockPreferencesConnector.addTermsAndConditions(is(validUtr), is(Generic -> TermsAccepted(true)), is(Some(emailAddress)))(any())).thenReturn(Future.successful(true))
      when(mockPreferencesConnector.activateUser(is(validUtr), anyString)(any())).thenReturn(Future.successful(true))

      val page = Future.successful(controller.submitPrefsFormAction(Interstitial)(user, FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress),("email.confirm", emailAddress), "accept-tc" -> "true"), hostContext = TestFixtures.sampleHostContext))

      status(page) shouldBe 303

      val eventArg : ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      verify(mockAuditConnector).sendEvent(eventArg.capture())(any(), any())

      private val value: ExtendedDataEvent = eventArg.getValue
      value.auditSource  shouldBe "preferences-frontend"
      value.auditType shouldBe EventTypes.Succeeded
      value.tags should contain ("transactionName" -> "Set Print Preference")
      value.detail \ "cohort" shouldBe JsString("IPage")
      value.detail \ "journey" shouldBe JsString("Interstitial")
      value.detail \ "utr" shouldBe JsString(validUtr.value)
      value.detail \ "email" shouldBe JsString("someone@email.com")
      value.detail \ "digital" shouldBe JsString("true")
      value.detail \ "userConfirmedReadTandCs" shouldBe JsString("true")
      value.detail \ "userCreated" shouldBe JsString("true")
      value.detail \ "userActivated" shouldBe JsString("true")
    }

    "be created as EventTypes.Failed when the user is failed to be activated on submitting a print preference from IPage" in new BizTaxPrefsControllerSetup {

      override def assignedCohort = IPage
      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(true)
      when(mockPreferencesConnector.addTermsAndConditions(is(validUtr), is(Generic -> TermsAccepted(true)), is(Some(emailAddress)))(any())).thenReturn(Future.successful(true))
      when(mockPreferencesConnector.activateUser(is(validUtr), anyString)(any())).thenReturn(Future.successful(false))

      val page = Future.successful(controller.submitPrefsFormAction(Interstitial)(user, FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress),("email.confirm", emailAddress), "accept-tc" -> "true"), hostContext = TestFixtures.sampleHostContext))

      status(page) shouldBe 303

      val eventArg : ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      verify(mockAuditConnector).sendEvent(eventArg.capture())(any(), any())

      private val value: ExtendedDataEvent = eventArg.getValue
      value.auditSource  shouldBe "preferences-frontend"
      value.auditType shouldBe EventTypes.Failed
      value.tags should contain ("transactionName" -> "Set Print Preference")
      value.detail \ "cohort" shouldBe JsString("IPage")
      value.detail \ "journey" shouldBe JsString("Interstitial")
      value.detail \ "utr" shouldBe JsString(validUtr.value)
      value.detail \ "email" shouldBe JsString("someone@email.com")
      value.detail \ "digital" shouldBe JsString("true")
      value.detail \ "userConfirmedReadTandCs" shouldBe JsString("true")
      value.detail \ "userCreated" shouldBe JsString("true")
      value.detail \ "userActivated" shouldBe JsString("false")
    }

    "be created as EventTypes.Succeeded when choosing to not opt out" in new BizTaxPrefsControllerSetup {

      override def assignedCohort = IPage
      when(mockPreferencesConnector.addTermsAndConditions(
        is(validUtr),
        is(Generic -> TermsAccepted(false)),
        is(None))(any())).thenReturn(Future.successful(true))

      val page = Future.successful(controller.submitPrefsFormAction(AccountDetails)(user, FakeRequest().withFormUrlEncodedBody("opt-in" -> "false"), hostContext = TestFixtures.sampleHostContext))

      status(page) shouldBe 303

      val eventArg : ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      verify(mockAuditConnector).sendEvent(eventArg.capture())(any(), any())

      private val value: ExtendedDataEvent = eventArg.getValue
      value.auditSource  shouldBe "preferences-frontend"
      value.auditType shouldBe EventTypes.Succeeded
      value.tags should contain ("transactionName" -> "Set Print Preference")
      value.detail \ "cohort" shouldBe JsString("IPage")
      value.detail \ "journey" shouldBe JsString("AccountDetails")
      value.detail \ "utr" shouldBe JsString(validUtr.value)
      value.detail \ "email" shouldBe JsString("")
      value.detail \ "digital" shouldBe JsString("false")
      value.detail \ "userConfirmedReadTandCs" shouldBe JsString("false")
      value.detail \ "userCreated" shouldBe JsString("true")
      value.detail \ "userActivated" shouldBe JsString("false")
    }
  }
}