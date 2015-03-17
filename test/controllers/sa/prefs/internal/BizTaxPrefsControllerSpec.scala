package controllers.sa.prefs.internal

import connectors.{EmailConnector, PreferencesConnector, SaEmailPreference, SaPreference}
import controllers.common.FrontEndRedirect
import controllers.sa.Encrypted
import controllers.sa.prefs.AuthorityUtils._
import controllers.sa.prefs.ExternalUrls
import controllers.sa.prefs.internal.EmailOptInJourney._
import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.JsString
import play.api.test.Helpers._
import play.api.test.{FakeApplication, FakeRequest, WithApplication}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{EventTypes, ExtendedDataEvent}
import uk.gov.hmrc.play.microservice.auth.AuthConnector
import uk.gov.hmrc.play.microservice.domain.User
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

abstract class BizTaxPrefsControllerSetup extends WithApplication(FakeApplication()) with MockitoSugar {
  def assignedCohort: OptInCohort = FPage

  val auditConnector = mock[AuditConnector]
  val preferencesConnector = mock[PreferencesConnector]
  val authConnector = mock[AuthConnector]
  val emailConnector = mock[EmailConnector]
  val controller = new BizTaxPrefsController(auditConnector, preferencesConnector, emailConnector)(authConnector) {
    override def calculateCohort(user: User) = assignedCohort
    override def calculate(hashCode: Int): OptInCohort = assignedCohort
  }

  val request = FakeRequest()

  when(preferencesConnector.saveCohort(any(), any())(any())).thenReturn(Future.successful(()))
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

    "redirect to interstitial page for the matching cohort if they have no preference set" in new BizTaxPrefsControllerSetup {
      when(preferencesConnector.getPreferences(is(validUtr))(any())).thenReturn(None)

      val page = controller.redirectToBTAOrInterstitialPageAction(user, request)

      status(page) shouldBe 303
      header("Location", page).get should include(routes.BizTaxPrefsController.displayInterstitialPrefsFormForCohort(Some(assignedCohort)).url)
    }

  }

  "The preferences interstitial page" should {

    "redirect to BTA when preferences already exist" in new BizTaxPrefsControllerSetup {
      val preferencesAlreadyCreated = SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.verified)))
      when(preferencesConnector.getPreferences(is(validUtr))(any())).thenReturn(Some(preferencesAlreadyCreated))

      val page = controller.displayInterstitialPrefsFormAction(user, request, Some(assignedCohort))

      status(page) shouldBe 303
      header("Location", page).get should include(ExternalUrls.businessTaxHome)
    }

    "redirect to a re-calculated cohort when no cohort is supplied" in new BizTaxPrefsControllerSetup {
      when(preferencesConnector.getPreferences(is(validUtr))(any())).thenReturn(None)

      val page = controller.displayInterstitialPrefsFormAction(user, request, possibleCohort = None)

      status(page) shouldBe 303
      header("Location", page).get should be (routes.BizTaxPrefsController.displayInterstitialPrefsFormForCohort(Some(assignedCohort)).url)
    }

    "render the form in the correct initial state when no preferences exist" in new BizTaxPrefsControllerSetup {
      when(preferencesConnector.getPreferences(is(validUtr))(any())).thenReturn(None)

      val page = controller.displayInterstitialPrefsFormAction(user, request, Some(assignedCohort))

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))

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

    "audit the cohort information for FPage" in new BizTaxPrefsControllerSetup {
      override def assignedCohort = FPage
      when(preferencesConnector.getPreferences(is(validUtr))(any())).thenReturn(None)

      val page = controller.displayInterstitialPrefsFormAction(user, request, Some(assignedCohort))
      status(page) shouldBe 200

      val eventArg : ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      verify(auditConnector).sendEvent(eventArg.capture())(any(), any())

      private val value: ExtendedDataEvent = eventArg.getValue
      value.auditSource  shouldBe "preferences-frontend"
      value.auditType shouldBe EventTypes.Succeeded
      value.tags should contain ("transactionName" -> "Show Print Preference Option")
      value.detail \ "cohort" shouldBe JsString("FPage")
      value.detail \ "journey" shouldBe JsString("Interstitial")
      value.detail \ "utr" shouldBe JsString(validUtr.value)
    }
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

      val page = controller.displayPrefsFormAction(None, Some(assignedCohort))(user, request)
      status(page) shouldBe 200
      val document = Jsoup.parse(contentAsString(page))
      document.getElementsByTag("nav").attr("id") shouldBe "proposition-menu"
    }

    "have correct form action to save preferences" in new BizTaxPrefsControllerSetup {
      val page = controller.displayPrefsFormAction(None, Some(assignedCohort))(user, request)
      status(page) shouldBe 200
      val document = Jsoup.parse(contentAsString(page))
      document.select("#form-submit-email-address").attr("action") should endWith("opt-in-email-reminders")
    }

    "audit the cohort information for the account details page" in new BizTaxPrefsControllerSetup {
      val page = controller.displayPrefsFormAction(None, Some(assignedCohort))(user, request)
      status(page) shouldBe 200

      val eventArg : ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      verify(auditConnector).sendEvent(eventArg.capture())(any(), any())

      private val value: ExtendedDataEvent = eventArg.getValue
      value.auditSource  shouldBe "preferences-frontend"
      value.auditType shouldBe EventTypes.Succeeded
      value.tags should contain ("transactionName" -> "Show Print Preference Option")
      value.detail \ "cohort" shouldBe JsString(assignedCohort.toString)
      value.detail \ "journey" shouldBe JsString("AccountDetails")
      value.detail \ "utr" shouldBe JsString(validUtr.value)
    }

    "redirect to a re-calculated cohort when no cohort is supplied" in new BizTaxPrefsControllerSetup {
      when(preferencesConnector.getPreferences(is(validUtr))(any())).thenReturn(None)

      val page = controller.displayPrefsFormAction(emailAddress = None, possibleCohort = None)(user, request)

      status(page) shouldBe 303
      header("Location", page).get should be (routes.BizTaxPrefsController.displayPrefsFormForCohort(Some(assignedCohort), None).url)
    }
  }

  "The preferences form" should {

    "render an email input field with no value if no email address is supplied, and no option selected" in new BizTaxPrefsControllerSetup {
      val page = controller.displayPrefsFormAction(None, Some(assignedCohort))(user, request)

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))

      document.getElementById("email.main").attr("value") shouldBe ""
      document.getElementById("email.confirm").attr("value") shouldBe ""
      document.getElementById("opt-in-in").attr("checked") should be ("checked")
      document.getElementById("opt-in-out").attr("checked") should be (empty)
    }

    "render an email input field populated with the supplied email address, and the Opt-in option selected" in new BizTaxPrefsControllerSetup {
      val emailAddress = "bob@bob.com"

      val page = controller.displayPrefsFormAction(Some(Encrypted(EmailAddress(emailAddress))), Some(assignedCohort))(user, request)

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
      val page = Future.successful(controller.submitPrefsFormAction(AccountDetails)(user, FakeRequest().withFormUrlEncodedBody()))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select(".error-notification").text shouldBe "Confirm if you want Self Assessment email reminders"
      verifyZeroInteractions(preferencesConnector, emailConnector)
    }

    "show an error when opting-in if the email is incorrectly formatted" in new BizTaxPrefsControllerSetup {
      val emailAddress = "invalid-email"

      val page = Future.successful(controller.submitPrefsFormAction(AccountDetails)(user, FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", "email.main" -> emailAddress)))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select("#form-submit-email-address .error-notification").text shouldBe "Enter a valid email address. You must accept the terms and conditions"
      verifyZeroInteractions(preferencesConnector, emailConnector)
    }

    "show an error when opting-in if the T&C's are not accepted" in new BizTaxPrefsControllerSetup {
      override def assignedCohort = FPage

      val emailAddress = "someone@email.com"
      val page = Future.successful(controller.submitPrefsFormAction(AccountDetails)(user,
        FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", "email.main" -> emailAddress, "email.confirm" -> emailAddress, "accept-tc" -> "false")))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select(".error-notification").text shouldBe "You must accept the terms and conditions"
      verifyZeroInteractions(preferencesConnector, emailConnector)
    }

    "show an error when opting-in if the T&C's accepted flag is not present" in new BizTaxPrefsControllerSetup {
      override def assignedCohort = FPage

      val emailAddress = "someone@email.com"
      val page = Future.successful(controller.submitPrefsFormAction(AccountDetails)(user,
        FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", "email.main" -> emailAddress, "email.confirm" -> emailAddress)))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select(".error-notification").text shouldBe "You must accept the terms and conditions"
      verifyZeroInteractions(preferencesConnector, emailConnector)
    }

    "show an error when opting-in if the email is not set" in new BizTaxPrefsControllerSetup {

      val page = Future.successful(controller.submitPrefsFormAction(AccountDetails)(user, FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", "email.main" -> "", "accept-tc" -> "true")))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select(".error-notification").text shouldBe "As you would like to opt in, please enter an email address."
      verifyZeroInteractions(preferencesConnector, emailConnector)
    }

    "show an error when opting-in if the two email fields are not equal" in new BizTaxPrefsControllerSetup {
      val emailAddress = "someone@email.com"

      val page = Future.successful(controller.submitPrefsFormAction(AccountDetails)(user, FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", "email.main" -> emailAddress, "email.confirm" -> "other", "accept-tc" -> "true")))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select("#form-submit-email-address .error-notification").text shouldBe "Check your email addresses - they don’t match."
      verifyZeroInteractions(preferencesConnector, emailConnector)
    }

    "show a warning page when opting-in if the email has a valid structure but does not pass validation by the email micro service" in new BizTaxPrefsControllerSetup {

      val emailAddress = "someone@dodgy.domain"
      when(emailConnector.isValid(is(emailAddress))(any())).thenReturn(false)

      val page = Future.successful(controller.submitPrefsFormAction(AccountDetails)(user, FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress),("email.confirm", emailAddress), "accept-tc" -> "true")))

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") shouldNot be(null)
      document.select("#emailIsCorrectLink") shouldNot be(null)
    }

    "when opting-in, validate the email address, save the preference and redirect to the thank you page with the email address encrpyted" in new BizTaxPrefsControllerSetup {
      val emailAddress = "someone@email.com"
      when(emailConnector.isValid(is(emailAddress))(any())).thenReturn(true)
      when(preferencesConnector.savePreferences(is(validUtr), is(true), is(Some(emailAddress)))(any())).thenReturn(Future.successful(None))

      val page = Future.successful(controller.submitPrefsFormAction(AccountDetails)(user, FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress),("email.confirm", emailAddress), "accept-tc" -> "true")))

      status(page) shouldBe 303
      header("Location", page).get should include(routes.BizTaxPrefsController.thankYou(Some(Encrypted(EmailAddress(emailAddress)))).toString())

      verify(preferencesConnector).saveCohort(is(validUtr), is(assignedCohort))(any())
      verify(preferencesConnector).savePreferences(is(validUtr), is(true), is(Some(emailAddress)))(any())
      verify(emailConnector).isValid(is(emailAddress))(any())

      verifyNoMoreInteractions(preferencesConnector, emailConnector)
    }

    "when opting-out, save the preference and redirect to the thank you page" in new BizTaxPrefsControllerSetup {
      when(preferencesConnector.savePreferences(is(validUtr), is(false), is(None))(any())).thenReturn(Future.successful(None))

      val page = Future.successful(controller.submitPrefsFormAction(AccountDetails)(user, FakeRequest().withFormUrlEncodedBody("opt-in" -> "false")))

      status(page) shouldBe 303
      header("Location", page).get should include(FrontEndRedirect.businessTaxHome)

      verify(preferencesConnector).saveCohort(is(validUtr), is(assignedCohort))(any())
      verify(preferencesConnector).savePreferences(is(validUtr), is(false), is(None))(any())

      verifyNoMoreInteractions(preferencesConnector, emailConnector)
    }
  }

  "A post to set preferences with an emailVerifiedFlag" should {

    "if the verified flag is true, save the preference and redirect to the thank you page without verifying the email address again" in new BizTaxPrefsControllerSetup {
      val emailAddress = "someone@email.com"
      when(preferencesConnector.savePreferences(is(validUtr), is(true), is(Some(emailAddress)))(any())).thenReturn(Future.successful(None))

      val page = Future.successful(controller.submitPrefsFormAction(AccountDetails)(user, FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), ("email.confirm", emailAddress), ("emailVerified", "true"), "accept-tc" -> "true")))

      status(page) shouldBe 303
      header("Location", page).get should include(routes.BizTaxPrefsController.thankYou(Some(Encrypted(EmailAddress(emailAddress)))).toString())

      verify(preferencesConnector).saveCohort(is(validUtr), is(assignedCohort))(any())
      verify(preferencesConnector).savePreferences(is(validUtr), is(true), is(Some(emailAddress)))(any())

      verifyNoMoreInteractions(preferencesConnector, emailConnector)
    }

    "if the verified flag is false and the email does not pass validation by the email micro service, display the verify page" in new BizTaxPrefsControllerSetup {

      val emailAddress = "someone@dodgy.domain"
      when(emailConnector.isValid(is(emailAddress))(any())).thenReturn(false)

      val page = Future.successful(controller.submitPrefsFormAction(AccountDetails)(user, FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), ("email.confirm", emailAddress), ("emailVerified", "false"), "accept-tc" -> "true")))

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") shouldNot be(null)
      document.select("#emailIsCorrectLink") shouldNot be(null)

      verifyZeroInteractions(preferencesConnector)
    }

    "if the verified flag is any value other than true, treat it as false" in new BizTaxPrefsControllerSetup {

      val emailAddress = "someone@dodgy.domain"
      when(emailConnector.isValid(is(emailAddress))(any())).thenReturn(false)

      val page = Future.successful(controller.submitPrefsFormAction(AccountDetails)(user, FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), ("email.confirm", emailAddress), ("emailVerified", "hjgjhghjghjgj"), "accept-tc" -> "true")))

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") shouldNot be(null)
      document.select("#emailIsCorrectLink") shouldNot be(null)
    }
  }

  "An audit event" should {
    "be created when submitting a print preference from FPage" in new BizTaxPrefsControllerSetup {

      override def assignedCohort = FPage
      val emailAddress = "someone@email.com"
      when(emailConnector.isValid(is(emailAddress))(any())).thenReturn(true)
      when(preferencesConnector.savePreferences(is(validUtr), is(true), is(Some(emailAddress)))(any())).thenReturn(Future.successful(None))

      val page = Future.successful(controller.submitPrefsFormAction(Interstitial)(user, FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress),("email.confirm", emailAddress), "accept-tc" -> "true")))

      status(page) shouldBe 303

      val eventArg : ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      verify(auditConnector).sendEvent(eventArg.capture())(any(), any())

      private val value: ExtendedDataEvent = eventArg.getValue
      value.auditSource  shouldBe "preferences-frontend"
      value.auditType shouldBe EventTypes.Succeeded
      value.tags should contain ("transactionName" -> "Set Print Preference")
      value.detail \ "cohort" shouldBe JsString("FPage")
      value.detail \ "journey" shouldBe JsString("Interstitial")
      value.detail \ "utr" shouldBe JsString(validUtr.value)
      value.detail \ "email" shouldBe JsString("someone@email.com")
      value.detail \ "digital" shouldBe JsString("true")
      value.detail \ "userConfirmedReadTandCs" shouldBe JsString("true")
    }

    "be created when choosing to not accept email reminders from FPage" in new BizTaxPrefsControllerSetup {

      override def assignedCohort = FPage
      when(preferencesConnector.savePreferences(
        is(validUtr),
        is(false),
        is(None))(any())).thenReturn(Future.successful(None))

      val page = Future.successful(controller.submitPrefsFormAction(AccountDetails)(user, FakeRequest().withFormUrlEncodedBody("opt-in" -> "false")))

      status(page) shouldBe 303

      val eventArg : ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      verify(auditConnector).sendEvent(eventArg.capture())(any(), any())

      private val value: ExtendedDataEvent = eventArg.getValue
      value.auditSource  shouldBe "preferences-frontend"
      value.auditType shouldBe EventTypes.Succeeded
      value.tags should contain ("transactionName" -> "Set Print Preference")
      value.detail \ "cohort" shouldBe JsString("FPage")
      value.detail \ "journey" shouldBe JsString("AccountDetails")
      value.detail \ "utr" shouldBe JsString(validUtr.value)
      value.detail \ "email" shouldBe JsString("")
      value.detail \ "digital" shouldBe JsString("false")
      value.detail \ "userConfirmedReadTandCs" shouldBe JsString("false")
    }
  }
}