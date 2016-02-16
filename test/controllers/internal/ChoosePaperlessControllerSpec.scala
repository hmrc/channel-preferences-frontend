package controllers.internal

import _root_.connectors._
import controllers.AuthorityUtils._
import helpers.{ConfigHelper, TestFixtures}
import model.Encrypted
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.JsString
import play.api.mvc.{Request, Results}
import play.api.test.{FakeRequest, WithApplication}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{EventTypes, ExtendedDataEvent}
import uk.gov.hmrc.play.frontend.auth._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future


abstract class ChoosePaperlessControllerSetup extends WithApplication(ConfigHelper.fakeApp) with MockitoSugar {

  val validUtr = SaUtr("1234567890")
  val user = AuthContext(authority = saAuthority("userId", validUtr.value), nameFromSession = Some("Ciccio"), governmentGatewayToken = None)

  val request = FakeRequest()

  def assignedCohort: OptInCohort = IPage

  val mockAuditConnector = mock[AuditConnector]
  val mockPreferencesConnector = mock[EntityResolverConnector]
  val mockAuthConnector = mock[AuthConnector]
  val mockEmailConnector = mock[EmailConnector]

  val controller = new ChoosePaperlessController {

    override def calculateCohort(user: AuthContext) = assignedCohort

    override def preferencesConnector: EntityResolverConnector = mockPreferencesConnector

    override def emailConnector: EmailConnector = mockEmailConnector

    override def auditConnector: AuditConnector = mockAuditConnector

    override protected implicit def authConnector: AuthConnector = mockAuthConnector

    override def authenticated = AuthenticatedBy(TestAuthenticationProvider, pageVisibility = GGConfidence)
  }

  object TestAuthenticationProvider extends AuthenticationProvider {

    override val id = "TST"

    def login = "/login"

    def redirectToLogin(implicit request: Request[_]) = Future.successful(Results.Redirect(login))

    def handleNotAuthenticated(implicit request: Request[_]) = {
      case _ => Future.successful(Left(user))
    }
  }

  when(mockPreferencesConnector.saveCohort(any(), any())(any())).thenReturn(Future.successful(()))
}

class ChoosePaperlessControllerSpec extends UnitSpec with MockitoSugar {

  import org.mockito.Matchers.{any, eq => is}
  import play.api.test.Helpers._

  def allGoPaperlessFormElementsArePresent(document: Document) {
    document.getElementById("email.main") shouldNot be(null)
    document.getElementById("email.main").attr("value") shouldBe ""

    document.getElementById("email.confirm") shouldNot be(null)
    document.getElementById("email.confirm").attr("value") shouldBe ""

    document.getElementById("opt-in-in") shouldNot be(null)
    document.getElementById("opt-in-in").attr("checked") shouldBe "checked"

    document.getElementById("opt-in-out") shouldNot be(null)
    document.getElementById("opt-in-out").attr("checked") shouldBe ""

    document.getElementById("terms-and-conditions").attr("href") should endWith("www.tax.service.gov.uk/information/terms#secure")
  }

  "The preferences action on non login version page" should {

    "show main banner" in new ChoosePaperlessControllerSetup {
      val page = controller.displayForm(Some(assignedCohort), None, TestFixtures.sampleHostContext)(request)
      status(page) shouldBe 200
      val document = Jsoup.parse(contentAsString(page))
      document.getElementsByTag("nav").attr("id") shouldBe "proposition-menu"
    }

    "have correct form action to save preferences" in new ChoosePaperlessControllerSetup {
      val page = controller.displayForm(Some(assignedCohort), None, TestFixtures.sampleHostContext)(request)
      status(page) shouldBe 200
      val document = Jsoup.parse(contentAsString(page))
      document.select("#form-submit-email-address").attr("action") should endWith(routes.ChoosePaperlessController.submitForm(TestFixtures.sampleHostContext).url)
    }

    "audit the cohort information for the account details page" in new ChoosePaperlessControllerSetup {
      val page = controller.displayForm(Some(assignedCohort), None, TestFixtures.sampleHostContext)(request)
      status(page) shouldBe 200

      val eventArg: ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      verify(mockAuditConnector).sendEvent(eventArg.capture())(any(), any())

      private val value: ExtendedDataEvent = eventArg.getValue
      value.auditSource shouldBe "preferences-frontend"
      value.auditType shouldBe EventTypes.Succeeded
      value.tags should contain("transactionName" -> "Show Print Preference Option")
      value.detail \ "cohort" shouldBe JsString(assignedCohort.toString)
      value.detail \ "journey" shouldBe JsString("AccountDetails")
      value.detail \ "utr" shouldBe JsString(validUtr.value)
    }

    "redirect to a re-calculated cohort when no cohort is supplied" in new ChoosePaperlessControllerSetup {
      when(mockPreferencesConnector.getPreferences(is(validUtr))(any())).thenReturn(None)

      val page = controller.displayForm(cohort = None, emailAddress = None, hostContext = TestFixtures.sampleHostContext)(request)

      status(page) shouldBe 303
      header("Location", page).get should be(routes.ChoosePaperlessController.displayForm(Some(assignedCohort), None, TestFixtures.sampleHostContext).url)
    }
  }

  "The preferences form" should {

    "render an email input field with no value if no email address is supplied, and no option selected" in new ChoosePaperlessControllerSetup {
      val page = controller.displayForm(Some(assignedCohort), None, TestFixtures.sampleHostContext)(request)

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))

      document.getElementById("email.main").attr("value") shouldBe ""
      document.getElementById("email.confirm").attr("value") shouldBe ""
      document.getElementById("opt-in-in").attr("checked") should be("checked")
      document.getElementById("opt-in-out").attr("checked") should be(empty)
    }

    "render an email input field populated with the supplied email address, and the Opt-in option selected" in new ChoosePaperlessControllerSetup {
      val emailAddress = "bob@bob.com"

      val page = controller.displayForm(Some(assignedCohort), Some(Encrypted(EmailAddress(emailAddress))), TestFixtures.sampleHostContext)(request)

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))

      document.getElementById("email.main") shouldNot be(null)
      document.getElementById("email.main").attr("value") shouldBe emailAddress
      document.getElementById("email.confirm") shouldNot be(null)
      document.getElementById("email.confirm").attr("value") shouldBe emailAddress
      document.getElementById("opt-in-in").attr("checked") should be("checked")
      document.getElementById("opt-in-out").attr("checked") should be(empty)
    }
  }

  "A post to set preferences with no emailVerifiedFlag" should {

    "show an error if no opt-in preference has been chosen" in new ChoosePaperlessControllerSetup {
      val page = Future.successful(controller.submitForm(TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody()))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select(".error-notification").text shouldBe "Confirm if you want paperless notifications"
      verifyZeroInteractions(mockPreferencesConnector, mockEmailConnector)
    }

    "show an error when opting-in if the email is incorrectly formatted" in new ChoosePaperlessControllerSetup {
      val emailAddress = "invalid-email"

      val page = Future.successful(controller.submitForm(TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", "email.main" -> emailAddress)))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select("#form-submit-email-address .error-notification").text shouldBe "Enter a valid email address. You must accept the terms and conditions"
      verifyZeroInteractions(mockPreferencesConnector, mockEmailConnector)
    }

    "show an error when opting-in if the T&C's are not accepted" in new ChoosePaperlessControllerSetup {
      override def assignedCohort = IPage

      val emailAddress = "someone@email.com"
      val page = Future.successful(controller.submitForm(TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", "email.main" -> emailAddress, "email.confirm" -> emailAddress, "accept-tc" -> "false")))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select(".error-notification").text shouldBe "You must accept the terms and conditions"
      verifyZeroInteractions(mockPreferencesConnector, mockEmailConnector)
    }

    "show an error when opting-in if the T&C's accepted flag is not present" in new ChoosePaperlessControllerSetup {
      override def assignedCohort = IPage

      val emailAddress = "someone@email.com"
      val page = Future.successful(controller.submitForm(TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", "email.main" -> emailAddress, "email.confirm" -> emailAddress)))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select(".error-notification").text shouldBe "You must accept the terms and conditions"
      verifyZeroInteractions(mockPreferencesConnector, mockEmailConnector)
    }

    "show an error when opting-in if the email is not set" in new ChoosePaperlessControllerSetup {

      val page = Future.successful(controller.submitForm(TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", "email.main" -> "", "accept-tc" -> "true")))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select(".error-notification").text shouldBe "As you would like to opt in, please enter an email address."
      verifyZeroInteractions(mockPreferencesConnector, mockEmailConnector)
    }

    "show an error when opting-in if the two email fields are not equal" in new ChoosePaperlessControllerSetup {
      val emailAddress = "someone@email.com"

      val page = Future.successful(controller.submitForm(TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", "email.main" -> emailAddress, "email.confirm" -> "other", "accept-tc" -> "true")))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select("#form-submit-email-address .error-notification").text shouldBe "Check your email addresses - they don't match."
      verifyZeroInteractions(mockPreferencesConnector, mockEmailConnector)
    }

    "show a warning page when opting-in if the email has a valid structure but does not pass validation by the email micro service" in new ChoosePaperlessControllerSetup {

      val emailAddress = "someone@dodgy.domain"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(false)

      val page = Future.successful(controller.submitForm(TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), ("email.confirm", emailAddress), "accept-tc" -> "true")))

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") shouldNot be(null)
      document.select("#emailIsCorrectLink") shouldNot be(null)
    }

    "when opting-in, validate the email address, save the preference and redirect to the thank you page with the email address encrpyted" in new ChoosePaperlessControllerSetup {
      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(true)
      when(mockPreferencesConnector.updateTermsAndConditions(is(validUtr), is(Generic -> TermsAccepted(true)), is(Some(emailAddress)))(any())).thenReturn(Future.successful(PreferencesCreated))

      val page = Future.successful(controller.submitForm(TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), ("email.confirm", emailAddress), "accept-tc" -> "true")))

      status(page) shouldBe 303
      header("Location", page).get should include(routes.ChoosePaperlessController.displayNearlyDone(Some(Encrypted(EmailAddress(emailAddress))), TestFixtures.sampleHostContext).toString())

      verify(mockPreferencesConnector).saveCohort(is(validUtr), is(assignedCohort))(any())
      verify(mockEmailConnector).isValid(is(emailAddress))(any())
      verify(mockPreferencesConnector).updateTermsAndConditions(is(validUtr), is(Generic -> TermsAccepted(true)), is(Some(emailAddress)))(any())

      verifyNoMoreInteractions(mockPreferencesConnector, mockEmailConnector)
    }

    "when opting-in, validate the email address, failed to save the preference and so not activate user and redirect to the thank you page with the email address encrpyted" in new ChoosePaperlessControllerSetup {
      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(true)
      when(mockPreferencesConnector.updateTermsAndConditions(is(validUtr), is(Generic -> TermsAccepted(true)), is(Some(emailAddress)))(any())).thenReturn(Future.successful(PreferencesFailure))

      val page = Future.successful(controller.submitForm(TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), ("email.confirm", emailAddress), "accept-tc" -> "true")))

      status(page) shouldBe 303
      header("Location", page).get should include(routes.ChoosePaperlessController.displayNearlyDone(Some(Encrypted(EmailAddress(emailAddress))), TestFixtures.sampleHostContext).toString())

      verify(mockPreferencesConnector).saveCohort(is(validUtr), is(assignedCohort))(any())
      verify(mockPreferencesConnector).updateTermsAndConditions(is(validUtr), is(Generic -> TermsAccepted(true)), is(Some(emailAddress)))(any())
      verify(mockEmailConnector).isValid(is(emailAddress))(any())

      verifyNoMoreInteractions(mockPreferencesConnector, mockEmailConnector)
    }

    "when opting-out, save the preference and redirect to the thank you page" in new ChoosePaperlessControllerSetup {
      when(mockPreferencesConnector.updateTermsAndConditions(is(validUtr), is(Generic -> TermsAccepted(false)), is(None))(any())).thenReturn(Future.successful(PreferencesCreated))
      val page = Future.successful(controller.submitForm(TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody("opt-in" -> "false")))

      status(page) shouldBe 303
      header("Location", page).get should be(TestFixtures.sampleHostContext.returnUrl)

      verify(mockPreferencesConnector).saveCohort(is(validUtr), is(assignedCohort))(any())
      verify(mockPreferencesConnector).updateTermsAndConditions(is(validUtr), is(Generic -> TermsAccepted(false)), is(None))(any())

      verifyNoMoreInteractions(mockPreferencesConnector, mockEmailConnector)
    }
  }

  "A post to set preferences with an emailVerifiedFlag" should {

    "if the verified flag is true, save the preference and redirect to the thank you page without verifying the email address again" in new ChoosePaperlessControllerSetup {
      val emailAddress = "someone@email.com"
      when(mockPreferencesConnector.updateTermsAndConditions(is(validUtr), is(Generic -> TermsAccepted(true)), is(Some(emailAddress)))(any())).thenReturn(Future.successful(PreferencesCreated))

      val page = Future.successful(controller.submitForm(TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), ("email.confirm", emailAddress), ("emailVerified", "true"), "accept-tc" -> "true")))

      status(page) shouldBe 303
      header("Location", page).get should include(routes.ChoosePaperlessController.displayNearlyDone(Some(Encrypted(EmailAddress(emailAddress))), TestFixtures.sampleHostContext).toString())

      verify(mockPreferencesConnector).saveCohort(is(validUtr), is(assignedCohort))(any())
      verify(mockPreferencesConnector).updateTermsAndConditions(is(validUtr), is(Generic -> TermsAccepted(true)), is(Some(emailAddress)))(any())

      verifyNoMoreInteractions(mockPreferencesConnector, mockEmailConnector)
    }

    "if the verified flag is false and the email does not pass validation by the email micro service, display the verify page" in new ChoosePaperlessControllerSetup {

      val emailAddress = "someone@dodgy.domain"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(false)

      val page = Future.successful(controller.submitForm(TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), ("email.confirm", emailAddress), ("emailVerified", "false"), "accept-tc" -> "true")))

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") shouldNot be(null)
      document.select("#emailIsCorrectLink") shouldNot be(null)

      verifyZeroInteractions(mockPreferencesConnector)
    }

    "if the verified flag is any value other than true, treat it as false" in new ChoosePaperlessControllerSetup {

      val emailAddress = "someone@dodgy.domain"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(false)

      val page = Future.successful(controller.submitForm(TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), ("email.confirm", emailAddress), ("emailVerified", "hjgjhghjghjgj"), "accept-tc" -> "true")))

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") shouldNot be(null)
      document.select("#emailIsCorrectLink") shouldNot be(null)

      verifyZeroInteractions(mockPreferencesConnector)
    }
  }

  "An audit event" should {

    "be created as EventTypes.Succeeded when a new user is activated on submitting a print preference from IPage" in new ChoosePaperlessControllerSetup {

      override def assignedCohort = IPage

      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(true)
      when(mockPreferencesConnector.updateTermsAndConditions(is(validUtr), is(Generic -> TermsAccepted(true)), is(Some(emailAddress)))(any())).thenReturn(Future.successful(PreferencesCreated))

      val page = Future.successful(controller.submitForm(TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), ("email.confirm", emailAddress), "accept-tc" -> "true")))

      status(page) shouldBe 303

      val eventArg: ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      verify(mockAuditConnector).sendEvent(eventArg.capture())(any(), any())

      private val value: ExtendedDataEvent = eventArg.getValue
      value.auditSource shouldBe "preferences-frontend"
      value.auditType shouldBe EventTypes.Succeeded
      value.tags should contain("transactionName" -> "Set Print Preference")
      value.detail \ "cohort" shouldBe JsString("IPage")
      value.detail \ "journey" shouldBe JsString("AccountDetails")
      value.detail \ "utr" shouldBe JsString(validUtr.value)
      value.detail \ "email" shouldBe JsString("someone@email.com")
      value.detail \ "digital" shouldBe JsString("true")
      value.detail \ "userConfirmedReadTandCs" shouldBe JsString("true")
      value.detail \ "newUserPreferencesCreated" shouldBe JsString("true")
    }

    "be created as EventTypes.Succeeded when an existing user is activated on submitting a print preference from IPage" in new ChoosePaperlessControllerSetup {

      override def assignedCohort = IPage

      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(true)
      when(mockPreferencesConnector.updateTermsAndConditions(is(validUtr), is(Generic -> TermsAccepted(true)), is(Some(emailAddress)))(any())).thenReturn(Future.successful(PreferencesExists))

      val page = Future.successful(controller.submitForm(TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), ("email.confirm", emailAddress), "accept-tc" -> "true")))

      status(page) shouldBe 303

      val eventArg: ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      verify(mockAuditConnector).sendEvent(eventArg.capture())(any(), any())

      private val value: ExtendedDataEvent = eventArg.getValue
      value.auditSource shouldBe "preferences-frontend"
      value.auditType shouldBe EventTypes.Succeeded
      value.tags should contain("transactionName" -> "Set Print Preference")
      value.detail \ "cohort" shouldBe JsString("IPage")
      value.detail \ "journey" shouldBe JsString("AccountDetails")
      value.detail \ "utr" shouldBe JsString(validUtr.value)
      value.detail \ "email" shouldBe JsString("someone@email.com")
      value.detail \ "digital" shouldBe JsString("true")
      value.detail \ "userConfirmedReadTandCs" shouldBe JsString("true")
      value.detail \ "newUserPreferencesCreated" shouldBe JsString("false")
    }

    "be created as EventTypes.Succeeded when choosing to not opt in" in new ChoosePaperlessControllerSetup {

      override def assignedCohort = IPage

      when(mockPreferencesConnector.updateTermsAndConditions(
        is(validUtr),
        is(Generic -> TermsAccepted(false)),
        is(None))(any())).thenReturn(Future.successful(PreferencesCreated))

      val page = Future.successful(controller.submitForm(TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody("opt-in" -> "false")))

      status(page) shouldBe 303

      val eventArg: ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      verify(mockAuditConnector).sendEvent(eventArg.capture())(any(), any())

      private val value: ExtendedDataEvent = eventArg.getValue
      value.auditSource shouldBe "preferences-frontend"
      value.auditType shouldBe EventTypes.Succeeded
      value.tags should contain("transactionName" -> "Set Print Preference")
      value.detail \ "cohort" shouldBe JsString("IPage")
      value.detail \ "journey" shouldBe JsString("AccountDetails")
      value.detail \ "utr" shouldBe JsString(validUtr.value)
      value.detail \ "email" shouldBe JsString("")
      value.detail \ "digital" shouldBe JsString("false")
      value.detail \ "userConfirmedReadTandCs" shouldBe JsString("false")
      value.detail \ "newUserPreferencesCreated" shouldBe JsString("true")
    }
  }
}