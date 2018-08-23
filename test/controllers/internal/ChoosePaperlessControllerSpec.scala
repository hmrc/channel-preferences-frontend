package controllers.internal

import _root_.connectors._
import controllers.AuthorityUtils._
import helpers.{ConfigHelper, TestFixtures}
import model.{Encrypted, HostContext}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.libs.json.{JsDefined, JsString}
import play.api.mvc.Results._
import play.api.mvc.{Request, Results}
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{EventTypes, MergedDataEvent}
import uk.gov.hmrc.play.frontend.auth._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

abstract class ChoosePaperlessControllerSetup extends MockitoSugar {

  val validUtr = SaUtr("1234567890")
  val user = AuthContext(authority = saAuthority("userId", validUtr.value), nameFromSession = Some("Ciccio"), governmentGatewayToken = None)

  val request = FakeRequest()
  def assignedCohort: OptInCohort = IPage

  val mockAuditConnector = mock[AuditConnector]
  val mockEntityResolverConnector : EntityResolverConnector = {
    val entityResolverMock = mock[EntityResolverConnector]
    when(entityResolverMock.getPreferencesStatus(any())(any())).thenReturn(Future.successful(Right[Int,PreferenceStatus](PreferenceNotFound(None))))
    when(entityResolverMock.getPreferencesStatusByToken(any(), any(), any())(any())).thenReturn(Future.successful(Right[Int,PreferenceStatus](PreferenceNotFound(None))))
    entityResolverMock
  }
  val mockAuthConnector = mock[AuthConnector]
  val mockEmailConnector = mock[EmailConnector]

  val controller = new ChoosePaperlessController {

    override def calculateCohort(user: HostContext) = assignedCohort

    override def entityResolverConnector: EntityResolverConnector = mockEntityResolverConnector

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

}

class ChoosePaperlessControllerSpec extends UnitSpec with MockitoSugar with OneAppPerSuite {

  import org.mockito.Matchers.{any, eq => is}
  import play.api.test.Helpers._

  override implicit lazy val app : Application = ConfigHelper.fakeApp

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

    "show main banner for svc" in new ChoosePaperlessControllerSetup {
      val page = controller.displayFormBySvc("mtdfbit", "token", None, TestFixtures.sampleHostContext)(request)
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

    "have correct form action to save preferences for svc" in new ChoosePaperlessControllerSetup {
      val page = controller.displayFormBySvc("mtdfbit", "token", None, TestFixtures.sampleHostContext)(request)
      status(page) shouldBe 200
      val document = Jsoup.parse(contentAsString(page))
      document.select("#form-submit-email-address").attr("action") should endWith(routes.ChoosePaperlessController.submitFormBySvc("mtdfbit", "token", TestFixtures.sampleHostContext).url)
    }

    "audit the cohort information for the account details page" in new ChoosePaperlessControllerSetup {
      val page = controller.displayForm(Some(assignedCohort), None, TestFixtures.sampleHostContext)(request)
      status(page) shouldBe 200

      val eventArg: ArgumentCaptor[MergedDataEvent] = ArgumentCaptor.forClass(classOf[MergedDataEvent])
      verify(mockAuditConnector).sendMergedEvent(eventArg.capture())(any(), any())

      private val value: MergedDataEvent = eventArg.getValue
      value.auditSource shouldBe "preferences-frontend"
      value.auditType shouldBe EventTypes.Succeeded
      value.request.tags should contain("transactionName" -> "Show Print Preference Option")
      value.request.detail("cohort") shouldBe assignedCohort.toString
      value.request.detail("journey") shouldBe "AccountDetails"
      value.request.detail("utr") shouldBe validUtr.value
      value.request.detail("nino") shouldBe "N/A"
    }

    "audit the cohort information for the account details page for svc" in new ChoosePaperlessControllerSetup {
      val page = controller.displayFormBySvc("mtdfbit", "token", None, TestFixtures.sampleHostContext)(request)
      status(page) shouldBe 200

      val eventArg: ArgumentCaptor[MergedDataEvent] = ArgumentCaptor.forClass(classOf[MergedDataEvent])
      verify(mockAuditConnector).sendMergedEvent(eventArg.capture())(any(), any())

      private val value: MergedDataEvent = eventArg.getValue
      value.auditSource shouldBe "preferences-frontend"
      value.auditType shouldBe EventTypes.Succeeded
      value.request.tags should contain("transactionName" -> "Show Print Preference Option")
      value.request.detail("cohort")shouldBe assignedCohort.toString
      value.request.detail("journey") shouldBe "AccountDetails"
      value.request.detail("utr") shouldBe validUtr.value
      value.request.detail("nino") shouldBe "N/A"
    }

    "redirect to a re-calculated cohort when no cohort is supplied" in new ChoosePaperlessControllerSetup {
      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(None)

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
      document.getElementById("opt-in-in").attr("checked") should be(empty)
      document.getElementById("opt-in-out").attr("checked") should be(empty)
    }

    "render an email input field populated with the supplied email address, and the Opt-in option selected" in new ChoosePaperlessControllerSetup {
      val emailAddress = "bob@bob.com"

      val page = controller.displayForm(Some(assignedCohort), Some(Encrypted(EmailAddress(emailAddress))), TestFixtures.sampleHostContext)(request)

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))

      document.getElementById("email.main") shouldNot be(null)
      document.getElementById("email.main").attr("value") shouldBe emailAddress
      document.getElementById("email.main").hasAttr("readonly") shouldBe false
      document.getElementById("email.confirm") shouldNot be(null)
      document.getElementById("email.confirm").attr("value") shouldBe emailAddress
      document.getElementById("email.main").hasAttr("readonly") shouldBe false
      document.getElementById("opt-in-in").attr("checked") should be(empty)
      document.getElementById("opt-in-out").attr("checked") should be(empty)
    }

    "render an email input field populated with the supplied readonly email address, and the Opt-in option selected if a preferences is not found for terms but an email do exist" in new ChoosePaperlessControllerSetup {
      val emailAddress = "bob@bob.com"

      override val mockEntityResolverConnector : EntityResolverConnector = {
        val entityResolverMock = mock[EntityResolverConnector]
        val emailPreference = EmailPreference(emailAddress, true, false, false, None)
        when(entityResolverMock.getPreferencesStatus(any())(any())).
          thenReturn(Future.successful(Right[Int,PreferenceStatus](PreferenceNotFound(Some(emailPreference))))
        )
        entityResolverMock
      }
      val page = controller.displayForm(Some(assignedCohort), Some(Encrypted(EmailAddress(emailAddress))), TestFixtures.sampleHostContext)(request)

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))

      document.getElementById("email.main") shouldNot be(null)
      document.getElementById("email.main").attr("value") shouldBe emailAddress
      document.getElementById("email.main").hasAttr("readonly") shouldBe true
      document.getElementById("email.confirm") shouldNot be(null)
      document.getElementById("email.confirm").attr("value") shouldBe emailAddress
      document.getElementById("email.confirm").hasAttr("hidden") shouldBe true
      document.getElementById("opt-in-in").attr("checked") should be(empty)
      document.getElementById("opt-in-out").attr("checked") should be(empty)
    }

    "render an email input field populated with the supplied readonly email address, and the Opt-in option selected if a opted out preferences with email is found" in new ChoosePaperlessControllerSetup {
      val emailAddress = "bob@bob.com"

      override val mockEntityResolverConnector : EntityResolverConnector = {
        val entityResolverMock = mock[EntityResolverConnector]
        val emailPreference = EmailPreference(emailAddress, true, false, false, None)
        when(entityResolverMock.getPreferencesStatus(any())(any())).
          thenReturn(Future.successful(Right[Int,PreferenceStatus](PreferenceFound(false,Some(emailPreference))))
          )
        entityResolverMock
      }
      val page = controller.displayForm(Some(assignedCohort), Some(Encrypted(EmailAddress(emailAddress))), TestFixtures.sampleHostContext)(request)

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))

      document.getElementById("email.main") shouldNot be(null)
      document.getElementById("email.main").attr("value") shouldBe emailAddress
      document.getElementById("email.main").hasAttr("readonly") shouldBe true
      document.getElementById("email.confirm") shouldNot be(null)
      document.getElementById("email.confirm").attr("value") shouldBe emailAddress
      document.getElementById("email.confirm").hasAttr("hidden") shouldBe true
      document.getElementById("opt-in-in").attr("checked") should be(empty)
      document.getElementById("opt-in-out").attr("checked") should be(empty)
    }
  }

  "A post to set preferences with no emailVerifiedFlag" should {

    "show an error if no opt-in preference has been chosen" in new ChoosePaperlessControllerSetup {
      val page = Future.successful(controller.submitForm(TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody()))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select(".error-notification").text shouldBe "Confirm if you want paperless notifications"
      verifyZeroInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "show an error when opting-in if the email is incorrectly formatted" in new ChoosePaperlessControllerSetup {
      val emailAddress = "invalid-email"

      val page = Future.successful(controller.submitForm(TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", "email.main" -> emailAddress)))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select("#form-submit-email-address .error-notification").text shouldBe "Enter a valid email address. You must accept the terms and conditions"
      verifyZeroInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "show an error when opting-in if the T&C's are not accepted" in new ChoosePaperlessControllerSetup {
      override def assignedCohort = IPage

      val emailAddress = "someone@email.com"
      val page = Future.successful(controller.submitForm(TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", "email.main" -> emailAddress, "email.confirm" -> emailAddress, "accept-tc" -> "false")))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select(".error-notification").text shouldBe "You must accept the terms and conditions"
      verifyZeroInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "show an error when opting-in if the T&C's accepted flag is not present" in new ChoosePaperlessControllerSetup {
      override def assignedCohort = IPage

      val emailAddress = "someone@email.com"
      val page = Future.successful(controller.submitForm(TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", "email.main" -> emailAddress, "email.confirm" -> emailAddress)))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select(".error-notification").text shouldBe "You must accept the terms and conditions"
      verifyZeroInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "show an error when opting-in if the email is not set" in new ChoosePaperlessControllerSetup {

      val page = Future.successful(controller.submitForm(TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", "email.main" -> "", "accept-tc" -> "true")))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select(".error-notification").text shouldBe "As you would like to opt in, please enter an email address."
      verifyZeroInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "show an error when opting-in if the two email fields are not equal" in new ChoosePaperlessControllerSetup {
      val emailAddress = "someone@email.com"

      val page = Future.successful(controller.submitForm(TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", "email.main" -> emailAddress, "email.confirm" -> "other", "accept-tc" -> "true")))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select("#form-submit-email-address .error-notification").text shouldBe "Check your email addresses - they don't match."
      verifyZeroInteractions(mockEntityResolverConnector, mockEmailConnector)
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
      when(mockEntityResolverConnector.updateTermsAndConditionsForSvc( is(GenericTerms -> TermsAccepted(true)), is(Some(emailAddress)), any(), any(), any())(any(), any())).thenReturn(Future.successful(PreferencesCreated))

      val page = Future.successful(controller.submitForm(TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), ("email.confirm", emailAddress), "accept-tc" -> "true")))

      status(page) shouldBe 303
      header("Location", page).get should include(routes.ChoosePaperlessController.displayNearlyDone(Some(Encrypted(EmailAddress(emailAddress))), TestFixtures.sampleHostContext).toString())

      verify(mockEmailConnector).isValid(is(emailAddress))(any())
      verify(mockEntityResolverConnector).updateTermsAndConditionsForSvc( is(GenericTerms -> TermsAccepted(true)), is(Some(emailAddress)), any(), any(), any())(any(), any())

      verifyNoMoreInteractions(mockEntityResolverConnector, mockEmailConnector)
    }
    "when opting-in, validate the email address, failed to save the preference and so not activate user and redirect to the thank you page with the email address encrpyted" in new ChoosePaperlessControllerSetup {
      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(true)
      when(mockEntityResolverConnector.updateTermsAndConditionsForSvc( is(GenericTerms -> TermsAccepted(true)), is(Some(emailAddress)), any(), any(), any())(any(), any())).thenReturn(Future.successful(PreferencesFailure))

      val page = Future.successful(controller.submitForm(TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), ("email.confirm", emailAddress), "accept-tc" -> "true")))

      status(page) shouldBe 303
      header("Location", page).get should include(routes.ChoosePaperlessController.displayNearlyDone(Some(Encrypted(EmailAddress(emailAddress))), TestFixtures.sampleHostContext).toString())

      verify(mockEntityResolverConnector).updateTermsAndConditionsForSvc( is(GenericTerms -> TermsAccepted(true)), is(Some(emailAddress)), any(), any(), any())(any(), any())
      verify(mockEmailConnector).isValid(is(emailAddress))(any())

      verifyNoMoreInteractions(mockEntityResolverConnector, mockEmailConnector)
    }


    "when opting-in, validate the email address, save the preference and redirect to the thank you page with the email address encrpyted when the user has no email address stored" in new ChoosePaperlessControllerSetup {
      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(true)
      when(mockEntityResolverConnector.updateTermsAndConditionsForSvc( is(GenericTerms -> TermsAccepted(true)), is(Some(emailAddress)), any(), any(), any())(any(), any())).thenReturn(Future.successful(PreferencesCreated))

      val page = Future.successful(controller.submitForm(TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), ("email.confirm", emailAddress), "accept-tc" -> "true", "emailAlreadyStored" -> "false")))

      status(page) shouldBe 303
      header("Location", page).get should include(routes.ChoosePaperlessController.displayNearlyDone(Some(Encrypted(EmailAddress(emailAddress))), TestFixtures.sampleHostContext).toString())

      verify(mockEmailConnector).isValid(is(emailAddress))(any())
      verify(mockEntityResolverConnector).updateTermsAndConditionsForSvc( is(GenericTerms -> TermsAccepted(true)), is(Some(emailAddress)), any(), any(), any())(any(), any())

      verifyNoMoreInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "when opting-in save the preference and redirect return url if the user has already an email (opting in for generic when the user has already opted in for TaxCredits)" in new ChoosePaperlessControllerSetup {
      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(true)
      when(mockEntityResolverConnector.updateTermsAndConditionsForSvc( is(GenericTerms -> TermsAccepted(true)), is(Some(emailAddress)), any(), any(), any())(any(), any())).thenReturn(Future.successful(PreferencesCreated))

      val page = Future.successful(controller.submitForm(TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), ("email.confirm", emailAddress), "accept-tc" -> "true", "emailAlreadyStored" -> "true")))

      status(page) shouldBe 303
      header("Location", page).get should include(TestFixtures.sampleHostContext.returnUrl)

      verify(mockEmailConnector).isValid(is(emailAddress))(any())
      verify(mockEntityResolverConnector).updateTermsAndConditionsForSvc( is(GenericTerms -> TermsAccepted(true)), is(Some(emailAddress)), any(), any(), any())(any(), any())

      verifyNoMoreInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "when opting-out, save the preference and redirect to the thank you page" in new ChoosePaperlessControllerSetup {
      when(mockEntityResolverConnector.updateTermsAndConditionsForSvc( is(GenericTerms -> TermsAccepted(false)), is(None), any(), any(), any())(any(), any())).thenReturn(Future.successful(PreferencesCreated))
      val page = Future.successful(controller.submitForm(TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody("opt-in" -> "false")))

      status(page) shouldBe 303
      header("Location", page).get should be(TestFixtures.sampleHostContext.returnUrl)

      verify(mockEntityResolverConnector).updateTermsAndConditionsForSvc( is(GenericTerms -> TermsAccepted(false)), is(None), any(), any(), any())(any(), any())

      verifyNoMoreInteractions(mockEntityResolverConnector, mockEmailConnector)
    }
  }

  "A post to set preferences with an emailVerifiedFlag" should {

    "if the verified flag is true, save the preference and redirect to the thank you page without verifying the email address again" in new ChoosePaperlessControllerSetup {
      val emailAddress = "someone@email.com"
      when(mockEntityResolverConnector.updateTermsAndConditionsForSvc(is(GenericTerms -> TermsAccepted(true)), is(Some(emailAddress)), any(), any(), any())(any(), any())).thenReturn(Future.successful(PreferencesCreated))

      val page = Future.successful(controller.submitForm(TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), ("email.confirm", emailAddress), ("emailVerified", "true"), "accept-tc" -> "true")))

      status(page) shouldBe 303
      header("Location", page).get should include(routes.ChoosePaperlessController.displayNearlyDone(Some(Encrypted(EmailAddress(emailAddress))), TestFixtures.sampleHostContext).toString())

      verify(mockEntityResolverConnector).updateTermsAndConditionsForSvc(is(GenericTerms -> TermsAccepted(true)), is(Some(emailAddress)), any(), any(), any())(any(), any())

      verifyNoMoreInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "if the verified flag is true, save the preference and redirect to the thank you page without verifying the email address again by svc" in new ChoosePaperlessControllerSetup {
      val emailAddress = "someone@email.com"
      when(mockEntityResolverConnector.updateTermsAndConditionsForSvc(is(GenericTerms -> TermsAccepted(true)), is(Some(emailAddress)), any(), any(), any())(any(), any())).thenReturn(Future.successful(PreferencesCreated))

      val page = Future.successful(controller.submitFormBySvc("mtdfbit", "token", TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), ("email.confirm", emailAddress), ("emailVerified", "true"), "accept-tc" -> "true")))

      status(page) shouldBe 303
      header("Location", page).get should include(routes.ChoosePaperlessController.displayNearlyDone(Some(Encrypted(EmailAddress(emailAddress))), TestFixtures.sampleHostContext).toString())

      verify(mockEntityResolverConnector).updateTermsAndConditionsForSvc(is(GenericTerms -> TermsAccepted(true)), is(Some(emailAddress)), any(), any(), any())(any(), any())

      verifyNoMoreInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "if the verified flag is false and the email does not pass validation by the email micro service, display the verify page" in new ChoosePaperlessControllerSetup {

      val emailAddress = "someone@dodgy.domain"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(false)

      val page = Future.successful(controller.submitForm(TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), ("email.confirm", emailAddress), ("emailVerified", "false"), "accept-tc" -> "true")))

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") shouldNot be(null)
      document.select("#emailIsCorrectLink") shouldNot be(null)

      verifyZeroInteractions(mockEntityResolverConnector)
    }

    "if the verified flag is any value other than true, treat it as false" in new ChoosePaperlessControllerSetup {

      val emailAddress = "someone@dodgy.domain"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(false)

      val page = Future.successful(controller.submitForm(TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), ("email.confirm", emailAddress), ("emailVerified", "hjgjhghjghjgj"), "accept-tc" -> "true")))

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") shouldNot be(null)
      document.select("#emailIsCorrectLink") shouldNot be(null)

      verifyZeroInteractions(mockEntityResolverConnector)

    }

    "if the verified flag is any value other than true, treat it as false for svc" in new ChoosePaperlessControllerSetup {

      val emailAddress = "someone@dodgy.domain"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(false)

      val page = Future.successful(controller.submitFormBySvc("mtdfbit", "token", TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), ("email.confirm", emailAddress), ("emailVerified", "hjgjhghjghjgj"), "accept-tc" -> "true")))

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") shouldNot be(null)
      document.select("#emailIsCorrectLink") shouldNot be(null)

      verifyZeroInteractions(mockEntityResolverConnector)
    }
  }

  "An audit event" should {

    "be created as EventTypes.Succeeded when a new user is activated on submitting a print preference from IPage" in new ChoosePaperlessControllerSetup {

      override def assignedCohort = IPage

      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(true)
      when(mockEntityResolverConnector.updateTermsAndConditionsForSvc( is(GenericTerms -> TermsAccepted(true)), is(Some(emailAddress)), any(), any(), any())(any(), any())).thenReturn(Future.successful(PreferencesCreated))

      val page = Future.successful(controller.submitForm(TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), ("email.confirm", emailAddress), "accept-tc" -> "true")))

      status(page) shouldBe 303

      val eventArg: ArgumentCaptor[MergedDataEvent] = ArgumentCaptor.forClass(classOf[MergedDataEvent])
      verify(mockAuditConnector).sendMergedEvent(eventArg.capture())(any(), any())

      private val value: MergedDataEvent = eventArg.getValue
      value.auditSource shouldBe "preferences-frontend"
      value.auditType shouldBe EventTypes.Succeeded
      value.request.tags should contain("transactionName" -> "Set Print Preference")
      value.request.detail("cohort") shouldBe "IPage"
      value.request.detail("journey") shouldBe "AccountDetails"
      value.request.detail("utr") shouldBe validUtr.value
      value.request.detail("nino") shouldBe "N/A"
      value.request.detail("email") shouldBe "someone@email.com"
      value.request.detail("digital") shouldBe "true"
      value.request.detail("userConfirmedReadTandCs") shouldBe "true"
      value.request.detail("newUserPreferencesCreated") shouldBe "true"
    }

    "be created as EventTypes.Succeeded when an existing user is activated on submitting a print preference from IPage" in new ChoosePaperlessControllerSetup {

      override def assignedCohort = IPage

      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(true)
      when(mockEntityResolverConnector.updateTermsAndConditionsForSvc( is(GenericTerms -> TermsAccepted(true)), is(Some(emailAddress)), any(), any(), any())(any(), any())).thenReturn(Future.successful(PreferencesExists))

      val page = Future.successful(controller.submitForm(TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), ("email.confirm", emailAddress), "accept-tc" -> "true")))

      status(page) shouldBe 303

      val eventArg: ArgumentCaptor[MergedDataEvent] = ArgumentCaptor.forClass(classOf[MergedDataEvent])
      verify(mockAuditConnector).sendMergedEvent(eventArg.capture())(any(), any())

      private val value: MergedDataEvent = eventArg.getValue
      value.auditSource shouldBe "preferences-frontend"
      value.auditType shouldBe EventTypes.Succeeded
      value.request.tags should contain("transactionName" -> "Set Print Preference")
      value.request.detail("cohort") shouldBe "IPage"
      value.request.detail("journey") shouldBe "AccountDetails"
      value.request.detail("utr") shouldBe validUtr.value
      value.request.detail("nino") shouldBe "N/A"
      value.request.detail("email") shouldBe "someone@email.com"
      value.request.detail("digital") shouldBe "true"
      value.request.detail("userConfirmedReadTandCs") shouldBe "true"
      value.request.detail("newUserPreferencesCreated") shouldBe "false"
    }

    "be created as EventTypes.Succeeded when choosing to not opt in" in new ChoosePaperlessControllerSetup {

      override def assignedCohort = IPage

      when(mockEntityResolverConnector.updateTermsAndConditionsForSvc(

        is(GenericTerms -> TermsAccepted(false)),
        is(None), any(), any(), any())(any(), any())).thenReturn(Future.successful(PreferencesCreated))

      val page = Future.successful(controller.submitForm(TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody("opt-in" -> "false")))

      status(page) shouldBe 303

      val eventArg: ArgumentCaptor[MergedDataEvent] = ArgumentCaptor.forClass(classOf[MergedDataEvent])
      verify(mockAuditConnector).sendMergedEvent(eventArg.capture())(any(), any())

      private val value: MergedDataEvent = eventArg.getValue
      value.auditSource shouldBe "preferences-frontend"
      value.auditType shouldBe EventTypes.Succeeded
      value.request.tags should contain("transactionName" -> "Set Print Preference")
      value.request.detail("cohort") shouldBe "IPage"
      value.request.detail("journey") shouldBe "AccountDetails"
      value.request.detail("utr") shouldBe validUtr.value
      value.request.detail("nino") shouldBe "N/A"
      value.request.detail("email") shouldBe ""
      value.request.detail("digital") shouldBe "false"
      value.request.detail("userConfirmedReadTandCs") shouldBe "false"
      value.request.detail("newUserPreferencesCreated") shouldBe "true"
    }
  }
}

class ChoosePaperlessControllerSpecTC extends UnitSpec with MockitoSugar with OneAppPerSuite {

  import org.mockito.Matchers.{any, eq => is}
  import play.api.test.Helpers._

  override implicit lazy val app : Application = ConfigHelper.fakeApp

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
      override def assignedCohort: OptInCohort = TCPage

      val page = controller.displayForm(Some(assignedCohort), None, TestFixtures.taxCreditsHostContext(""))(request)
      status(page) shouldBe 200
      val document = Jsoup.parse(contentAsString(page))
      document.getElementsByTag("nav").attr("id") shouldBe "proposition-menu"
    }

    "have correct form action to save preferences" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = TCPage

      val page = controller.displayForm(Some(assignedCohort), None, TestFixtures.taxCreditsHostContext(""))(request)
      status(page) shouldBe 200
      val document = Jsoup.parse(contentAsString(page))
      document.select("#form-submit-email-address-tc").attr("action") should endWith(routes.ChoosePaperlessController.submitForm(TestFixtures.taxCreditsHostContext("")).url)
    }

    "audit the cohort information for the account details page" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = TCPage

      val page = controller.displayForm(Some(assignedCohort), None, TestFixtures.taxCreditsHostContext(""))(request)
      status(page) shouldBe 200

      val eventArg: ArgumentCaptor[MergedDataEvent] = ArgumentCaptor.forClass(classOf[MergedDataEvent])
      verify(mockAuditConnector).sendMergedEvent(eventArg.capture())(any(), any())

      private val value: MergedDataEvent = eventArg.getValue
      value.auditSource shouldBe "preferences-frontend"
      value.auditType shouldBe EventTypes.Succeeded
      value.request.tags should contain("transactionName" -> "Show Print Preference Option")
      value.request.detail("cohort") shouldBe assignedCohort.toString
      value.request.detail("journey") shouldBe "AccountDetails"
      value.request.detail("utr") shouldBe validUtr.value
      value.request.detail("nino") shouldBe "N/A"
    }

    "redirect to a re-calculated cohort when no cohort is supplied" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = TCPage

      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(None)

      val page = controller.displayForm(cohort = None, emailAddress = None, hostContext = TestFixtures.taxCreditsHostContext(""))(request)

      status(page) shouldBe 303
      header("Location", page).get should be(routes.ChoosePaperlessController.displayForm(Some(assignedCohort), None, TestFixtures.taxCreditsHostContext("")).url)
    }
  }

  "The preferences form" should {

    "render an email input field with no value if no email address is supplied, and no option selected" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = TCPage

      val page = controller.displayForm(Some(assignedCohort), None, TestFixtures.taxCreditsHostContext(""))(request)

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))

      document.getElementById("email.main").attr("value") shouldBe ""
      document.getElementById("email.confirm").attr("value") shouldBe ""
      document.getElementById("opt-in-in").attr("checked") should be(empty)
      document.getElementById("opt-in-out").attr("checked") should be(empty)
    }

    "render an email input field populated with the supplied email address, and the Opt-in option selected" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = TCPage

      val emailAddress = "bob@bob.com"

      val page = controller.displayForm(Some(assignedCohort), Some(Encrypted(EmailAddress(emailAddress))), TestFixtures.taxCreditsHostContext(""))(request)

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))

      document.getElementById("email.main") shouldNot be(null)
      document.getElementById("email.main").attr("value") shouldBe emailAddress
      document.getElementById("email.main").attr("type") shouldBe "hidden"
      document.getElementById("email.confirm") shouldNot be(null)
      document.getElementById("email.confirm").attr("value") shouldBe emailAddress
      document.getElementById("email.main").attr("type") shouldBe "hidden"
      document.getElementById("opt-in-in").attr("checked") should be(empty)
      document.getElementById("opt-in-out").attr("checked") should be(empty)
    }

    "render an email input field populated with the supplied hidden email address, and no Opt-in option selected if a preferences is not found for terms but an email do exist" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = TCPage

      val emailAddress = "bob@bob.com"

      override val mockEntityResolverConnector : EntityResolverConnector = {
        val entityResolverMock = mock[EntityResolverConnector]
        val emailPreference = EmailPreference(emailAddress, true, false, false, None)
        when(entityResolverMock.getPreferencesStatus(any())(any())).
          thenReturn(Future.successful(Right[Int,PreferenceStatus](PreferenceNotFound(Some(emailPreference))))
          )
        entityResolverMock
      }
      val page = controller.displayForm(
        Some(assignedCohort),
        Some(Encrypted(EmailAddress(emailAddress))),
        TestFixtures.taxCreditsHostContext(emailAddress)
      )(request)

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))

      document.getElementById("email.main") shouldNot be(null)
      document.getElementById("email.main").attr("value") shouldBe emailAddress
      document.getElementById("email.main").attr("type") shouldBe "hidden"
      document.getElementById("email.confirm") shouldNot be(null)
      document.getElementById("email.confirm").attr("value") shouldBe emailAddress
      document.getElementById("email.confirm").attr("type") shouldBe "hidden"
      document.getElementById("opt-in-in").attr("checked") should be(empty)
      document.getElementById("opt-in-out").attr("checked") should be(empty)
    }

    "render an email input field populated with the supplied hidden email address, and no Opt-in option selected if a opted out preferences with email is found" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = TCPage

      val emailAddress = "bob@bob.com"

      override val mockEntityResolverConnector : EntityResolverConnector = {
        val entityResolverMock = mock[EntityResolverConnector]
        val emailPreference = EmailPreference(emailAddress, true, false, false, None)
        when(entityResolverMock.getPreferencesStatus(any())(any())).
          thenReturn(Future.successful(Right[Int,PreferenceStatus](PreferenceFound(false,Some(emailPreference))))
          )
        entityResolverMock
      }
      val page = controller.displayForm(
        Some(assignedCohort),
        Some(Encrypted(EmailAddress(emailAddress))),
        TestFixtures.taxCreditsHostContext(emailAddress)
      )(request)

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))

      document.getElementById("email.main") shouldNot be(null)
      document.getElementById("email.main").attr("value") shouldBe emailAddress
      document.getElementById("email.main").attr("type") shouldBe "hidden"
      document.getElementById("email.confirm") shouldNot be(null)
      document.getElementById("email.confirm").attr("value") shouldBe emailAddress
      document.getElementById("email.confirm").attr("type") shouldBe "hidden"
      document.getElementById("opt-in-in").attr("checked") should be(empty)
      document.getElementById("opt-in-out").attr("checked") should be(empty)
    }
  }

  "A post to set preferences with no emailVerifiedFlag" should {

    "show an error if no opt-in preference has been chosen" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = TCPage

      val page = Future.successful(controller.submitForm(TestFixtures.taxCreditsHostContext(""))(FakeRequest().withFormUrlEncodedBody()))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select(".error-notification").text shouldBe "Select yes if you are happy for us to store your email address"
      verifyZeroInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "show no error when opting-in if the email is incorrectly formatted (it has been prepopulated)" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = TCPage
      val emailAddress = "invalid-email"

      val page = Future.successful(controller.submitForm(TestFixtures.taxCreditsHostContext(emailAddress))(FakeRequest().withFormUrlEncodedBody("termsAndConditions.opt-in" -> "true", "email.main" -> emailAddress, "email.confirm" -> emailAddress, "termsAndConditions.accept-tc" -> "true", "emailAlreadyStored" -> "true")))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))

      verifyZeroInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "show an error when opting-in if the T&C's are not accepted" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = TCPage
      val emailAddress = "someone@email.com"
      val page = Future.successful(controller.submitForm(TestFixtures.taxCreditsHostContext(""))(FakeRequest().withFormUrlEncodedBody("termsAndConditions.opt-in" -> "true", "email.main" -> emailAddress, "email.confirm" -> emailAddress, "termsAndConditions.accept-tc" -> "false")))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select(".error-notification").text shouldBe "You must agree to the terms and conditions if you are happy for us to store your email address"
      verifyZeroInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "not show an error when opting-out if the T&C's are not selected" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = TCPage

      when(mockEntityResolverConnector.updateTermsAndConditionsForSvc(
        is(TaxCreditsTerms -> TermsAccepted(false)),
        is(None), any(), any(), any())(any(), any())).thenReturn(Future.successful(PreferencesCreated))

      val emailAddress = "someone@email.com"
      val page = Future.successful(controller.submitForm(TestFixtures.taxCreditsHostContext(""))(FakeRequest().withFormUrlEncodedBody("termsAndConditions.opt-in" -> "false", "email.main" -> emailAddress, "email.confirm" -> emailAddress)))

      status(page) shouldBe 303
    }

    "not show an error when opting-in if the T&C's are not selected" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = TCPage
      val emailAddress = "someone@email.com"
      val page = Future.successful(controller.submitForm(TestFixtures.taxCreditsHostContext(""))(FakeRequest().withFormUrlEncodedBody("termsAndConditions.opt-in" -> "true", "email.main" -> emailAddress, "email.confirm" -> emailAddress)))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select(".error-notification").text shouldBe "You must agree to the terms and conditions if you are happy for us to store your email address"
      verifyZeroInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "show an error when opting-in if the T&C's accepted flag is not present" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = TCPage
      val emailAddress = "someone@email.com"
      val page = Future.successful(controller.submitForm(TestFixtures.taxCreditsHostContext(emailAddress))(FakeRequest().withFormUrlEncodedBody("termsAndConditions.opt-in" -> "true", "email.main" -> emailAddress, "email.confirm" -> emailAddress)))

      status(page) shouldBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select(".error-notification").text shouldBe "You must agree to the terms and conditions if you are happy for us to store your email address"
      verifyZeroInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "show a warning page when opting-in if the email has a valid structure but does not pass validation by the email micro service" in new ChoosePaperlessControllerSetup {

      override def assignedCohort: OptInCohort = TCPage
      val emailAddress = "someone@dodgy.domain"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(false)

      val page = Future.successful(controller.submitForm(TestFixtures.taxCreditsHostContext(""))(FakeRequest().withFormUrlEncodedBody("termsAndConditions.opt-in" -> "true", ("email.main", emailAddress), ("email.confirm", emailAddress), "termsAndConditions.accept-tc" -> "true")))

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") shouldNot be(null)
      document.select("#emailIsCorrectLink") shouldNot be(null)
    }

    "when opting-in, validate the email address, save the preference and redirect to the thank you page with the email address encrpyted" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = TCPage
      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(true)
      when(mockEntityResolverConnector.updateTermsAndConditionsForSvc( is(TaxCreditsTerms -> TermsAccepted(true)), is(Some(emailAddress)), any(), any(), any())(any(), any())).thenReturn(Future.successful(PreferencesCreated))

      val page = Future.successful(controller.submitForm(TestFixtures.taxCreditsHostContext(""))(FakeRequest().withFormUrlEncodedBody("termsAndConditions.opt-in" -> "true", ("email.main", emailAddress), ("email.confirm", emailAddress), "termsAndConditions.accept-tc" -> "true")))

      status(page) shouldBe 303
      header("Location", page).get should include(routes.ChoosePaperlessController.displayNearlyDone(Some(Encrypted(EmailAddress(emailAddress))), TestFixtures.taxCreditsHostContext("")).toString())

      verify(mockEmailConnector).isValid(is(emailAddress))(any())
      verify(mockEntityResolverConnector).updateTermsAndConditionsForSvc(is(TaxCreditsTerms -> TermsAccepted(true)), is(Some(emailAddress)), any(), any(), any())(any(), any())

      verifyNoMoreInteractions(mockEntityResolverConnector, mockEmailConnector)
    }
    "when opting-in, validate the email address, failed to save the preference and so not activate user and redirect to the thank you page with the email address encrpyted" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = TCPage
      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(true)
      when(mockEntityResolverConnector.updateTermsAndConditionsForSvc( is(TaxCreditsTerms -> TermsAccepted(true)), is(Some(emailAddress)), any(), any(), any())(any(), any())).thenReturn(Future.successful(PreferencesFailure))

      val page = Future.successful(controller.submitForm(TestFixtures.taxCreditsHostContext(""))(FakeRequest().withFormUrlEncodedBody("termsAndConditions.opt-in" -> "true", ("email.main", emailAddress), ("email.confirm", emailAddress), "termsAndConditions.accept-tc" -> "true")))

      status(page) shouldBe 303
      header("Location", page).get should include(routes.ChoosePaperlessController.displayNearlyDone(Some(Encrypted(EmailAddress(emailAddress))), TestFixtures.taxCreditsHostContext("")).toString())

      verify(mockEntityResolverConnector).updateTermsAndConditionsForSvc( is(TaxCreditsTerms -> TermsAccepted(true)), is(Some(emailAddress)), any(), any(), any())(any(), any())
      verify(mockEmailConnector).isValid(is(emailAddress))(any())

      verifyNoMoreInteractions(mockEntityResolverConnector, mockEmailConnector)
    }


    "when opting-in, validate the email address, save the preference and redirect to the thank you page with the email address encrpyted when the user has no email address stored" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = TCPage
      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(true)
      when(mockEntityResolverConnector.updateTermsAndConditionsForSvc( is(TaxCreditsTerms -> TermsAccepted(true)), is(Some(emailAddress)), any(), any(), any())(any(), any())).thenReturn(Future.successful(PreferencesCreated))

      val page = Future.successful(controller.submitForm(TestFixtures.taxCreditsHostContext(""))(FakeRequest().withFormUrlEncodedBody("termsAndConditions.opt-in" -> "true", ("email.main", emailAddress), ("email.confirm", emailAddress), "termsAndConditions.accept-tc" -> "true", "emailAlreadyStored" -> "false")))

      status(page) shouldBe 303
      header("Location", page).get should include(routes.ChoosePaperlessController.displayNearlyDone(Some(Encrypted(EmailAddress(emailAddress))), TestFixtures.taxCreditsHostContext("")).toString())

      verify(mockEmailConnector).isValid(is(emailAddress))(any())
      verify(mockEntityResolverConnector).updateTermsAndConditionsForSvc( is(TaxCreditsTerms -> TermsAccepted(true)), is(Some(emailAddress)), any(), any(), any())(any(), any())

      verifyNoMoreInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "when opting-in save the preference and redirect return url if the user has already an email (opting in for generic when the user has already opted in for TaxCredits)" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = TCPage
      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(true)
      when(mockEntityResolverConnector.updateTermsAndConditionsForSvc( is(TaxCreditsTerms -> TermsAccepted(true)), is(Some(emailAddress)), any(), any(), any())(any(), any())).thenReturn(Future.successful(PreferencesCreated))

      val page = Future.successful(controller.submitForm(TestFixtures.taxCreditsHostContext(""))(FakeRequest().withFormUrlEncodedBody("termsAndConditions.opt-in" -> "true", ("email.main", emailAddress), ("email.confirm", emailAddress), "termsAndConditions.accept-tc" -> "true", "emailAlreadyStored" -> "true")))

      status(page) shouldBe 303
      header("Location", page).get should include(TestFixtures.taxCreditsHostContext("").returnUrl)

      verify(mockEmailConnector).isValid(is(emailAddress))(any())
      verify(mockEntityResolverConnector).updateTermsAndConditionsForSvc( is(TaxCreditsTerms -> TermsAccepted(true)), is(Some(emailAddress)), any(), any(), any())(any(), any())

      verifyNoMoreInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "when opting-out, save the preference and redirect to the thank you page" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = TCPage
      when(mockEntityResolverConnector.updateTermsAndConditionsForSvc( is(TaxCreditsTerms -> TermsAccepted(false)), is(None), any(), any(), any())(any(), any())).thenReturn(Future.successful(PreferencesCreated))
      val page = Future.successful(controller.submitForm(TestFixtures.taxCreditsHostContext(""))(FakeRequest().withFormUrlEncodedBody("termsAndConditions.opt-in" -> "false")))

      status(page) shouldBe 303
      header("Location", page).get should be(TestFixtures.taxCreditsHostContext("").returnUrl)

      verify(mockEntityResolverConnector).updateTermsAndConditionsForSvc( is(TaxCreditsTerms -> TermsAccepted(false)), is(None), any(), any(), any())(any(), any())

      verifyNoMoreInteractions(mockEntityResolverConnector, mockEmailConnector)
    }
  }

  "A post to set preferences with an emailVerifiedFlag" should {

    "if the verified flag is true, save the preference and redirect to the thank you page without verifying the email address again" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = TCPage
      val emailAddress = "someone@email.com"
      when(mockEntityResolverConnector.updateTermsAndConditionsForSvc( is(TaxCreditsTerms -> TermsAccepted(true)), is(Some(emailAddress)), any(), any(), any())(any(), any())).thenReturn(Future.successful(PreferencesCreated))

      val page = Future.successful(controller.submitForm(TestFixtures.taxCreditsHostContext(""))(FakeRequest().withFormUrlEncodedBody("termsAndConditions.opt-in" -> "true", ("email.main", emailAddress), ("email.confirm", emailAddress), ("emailVerified", "true"), "termsAndConditions.accept-tc" -> "true")))

      status(page) shouldBe 303
      header("Location", page).get should include(routes.ChoosePaperlessController.displayNearlyDone(Some(Encrypted(EmailAddress(emailAddress))), TestFixtures.taxCreditsHostContext("")).toString())

      verify(mockEntityResolverConnector).updateTermsAndConditionsForSvc( is(TaxCreditsTerms -> TermsAccepted(true)), is(Some(emailAddress)), any(), any(), any())(any(), any())

      verifyNoMoreInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "if the verified flag is false and the email does not pass validation by the email micro service, display the verify page" in new ChoosePaperlessControllerSetup {

      override def assignedCohort: OptInCohort = TCPage
      val emailAddress = "someone@dodgy.domain"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(false)

      val page = Future.successful(controller.submitForm(TestFixtures.taxCreditsHostContext(""))(FakeRequest().withFormUrlEncodedBody("termsAndConditions.opt-in" -> "true", ("email.main", emailAddress), ("email.confirm", emailAddress), ("emailVerified", "false"), "termsAndConditions.accept-tc" -> "true")))

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") shouldNot be(null)
      document.select("#emailIsCorrectLink") shouldNot be(null)

      verifyZeroInteractions(mockEntityResolverConnector)
    }

    "if the verified flag is any value other than true, treat it as false" in new ChoosePaperlessControllerSetup {

      override def assignedCohort: OptInCohort = TCPage
      val emailAddress = "someone@dodgy.domain"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(false)

      val page = Future.successful(controller.submitForm(TestFixtures.taxCreditsHostContext(""))(FakeRequest().withFormUrlEncodedBody("termsAndConditions.opt-in" -> "true", ("email.main", emailAddress), ("email.confirm", emailAddress), ("emailVerified", "hjgjhghjghjgj"), "termsAndConditions.accept-tc" -> "true")))

      status(page) shouldBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") shouldNot be(null)
      document.select("#emailIsCorrectLink") shouldNot be(null)

      verifyZeroInteractions(mockEntityResolverConnector)
    }
  }

  "An audit event" should {

    "be created as EventTypes.Succeeded when a new user is activated on submitting a print preference from TCPage" in new ChoosePaperlessControllerSetup {

      override def assignedCohort: OptInCohort = TCPage
      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(true)
      when(mockEntityResolverConnector.updateTermsAndConditionsForSvc( is(TaxCreditsTerms -> TermsAccepted(true)), is(Some(emailAddress)), any(), any(), any())(any(), any())).thenReturn(Future.successful(PreferencesCreated))

      val page = Future.successful(controller.submitForm(TestFixtures.taxCreditsHostContext(""))(FakeRequest().withFormUrlEncodedBody("termsAndConditions.opt-in" -> "true", ("email.main", emailAddress), ("email.confirm", emailAddress), "termsAndConditions.accept-tc" -> "true")))

      status(page) shouldBe 303

      val eventArg: ArgumentCaptor[MergedDataEvent] = ArgumentCaptor.forClass(classOf[MergedDataEvent])
      verify(mockAuditConnector).sendMergedEvent(eventArg.capture())(any(), any())

      private val value: MergedDataEvent = eventArg.getValue
      value.auditSource shouldBe "preferences-frontend"
      value.auditType shouldBe EventTypes.Succeeded
      value.request.tags should contain("transactionName" -> "Set Print Preference")
      value.request.detail("cohort") shouldBe "TCPage"
      value.request.detail("journey") shouldBe "AccountDetails"
      value.request.detail("utr") shouldBe validUtr.value
      value.request.detail("nino") shouldBe "N/A"
      value.request.detail("email") shouldBe "someone@email.com"
      value.request.detail("digital") shouldBe "true"
      value.request.detail("userConfirmedReadTandCs") shouldBe "true"
      value.request.detail("newUserPreferencesCreated") shouldBe "true"
    }

    "be created as EventTypes.Succeeded when an existing user is activated on submitting a print preference from TCPage" in new ChoosePaperlessControllerSetup {

      override def assignedCohort: OptInCohort = TCPage
      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(true)
      when(mockEntityResolverConnector.updateTermsAndConditionsForSvc( is(TaxCreditsTerms -> TermsAccepted(true)), is(Some(emailAddress)), any(), any(), any())(any(), any())).thenReturn(Future.successful(PreferencesExists))

      val page = Future.successful(controller.submitForm(TestFixtures.taxCreditsHostContext(""))(FakeRequest().withFormUrlEncodedBody("termsAndConditions.opt-in" -> "true", ("email.main", emailAddress), ("email.confirm", emailAddress), "termsAndConditions.accept-tc" -> "true")))

      status(page) shouldBe 303

      val eventArg: ArgumentCaptor[MergedDataEvent] = ArgumentCaptor.forClass(classOf[MergedDataEvent])
      verify(mockAuditConnector).sendMergedEvent(eventArg.capture())(any(), any())

      private val value: MergedDataEvent = eventArg.getValue
      value.auditSource shouldBe "preferences-frontend"
      value.auditType shouldBe EventTypes.Succeeded
      value.request.tags should contain("transactionName" -> "Set Print Preference")
      value.request.detail("cohort") shouldBe "TCPage"
      value.request.detail("journey") shouldBe "AccountDetails"
      value.request.detail("utr") shouldBe validUtr.value
      value.request.detail("nino") shouldBe "N/A"
      value.request.detail("email") shouldBe "someone@email.com"
      value.request.detail("digital") shouldBe "true"
      value.request.detail("userConfirmedReadTandCs") shouldBe "true"
      value.request.detail("newUserPreferencesCreated") shouldBe "false"
    }

    "be created as EventTypes.Succeeded when choosing to not opt in" in new ChoosePaperlessControllerSetup {

      override def assignedCohort: OptInCohort = TCPage

      when(mockEntityResolverConnector.updateTermsAndConditionsForSvc(
        is(TaxCreditsTerms -> TermsAccepted(false)),
        is(None), any(), any(), any())(any(), any())).thenReturn(Future.successful(PreferencesCreated))

      val page = Future.successful(controller.submitForm(TestFixtures.taxCreditsHostContext(""))(FakeRequest().withFormUrlEncodedBody("termsAndConditions.opt-in" -> "false")))

      status(page) shouldBe 303

      val eventArg: ArgumentCaptor[MergedDataEvent] = ArgumentCaptor.forClass(classOf[MergedDataEvent])
      verify(mockAuditConnector).sendMergedEvent(eventArg.capture())(any(), any())

      private val value: MergedDataEvent = eventArg.getValue
      value.auditSource shouldBe "preferences-frontend"
      value.auditType shouldBe EventTypes.Succeeded
      value.request.tags should contain("transactionName" -> "Set Print Preference")
      value.request.detail("cohort") shouldBe "TCPage"
      value.request.detail("journey") shouldBe "AccountDetails"
      value.request.detail("utr") shouldBe validUtr.value
      value.request.detail("nino") shouldBe "N/A"
      value.request.detail("email") shouldBe ""
      value.request.detail("digital") shouldBe "false"
      value.request.detail("userConfirmedReadTandCs") shouldBe "false"
      value.request.detail("newUserPreferencesCreated") shouldBe "true"
    }
  }
}
