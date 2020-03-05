/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers.internal

import connectors.PreferenceResponse._
import connectors._
import controllers.auth.AuthenticatedRequest
import helpers.TestFixtures
import model.Encrypted
import model.Language.Welsh
import org.joda.time.{DateTime, DateTimeZone}
import org.jsoup.Jsoup
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.HeaderNames
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.retrieve.{LoginTimes, Name, ~}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ManagePaperlessControllerSpec
    extends PlaySpec with MockitoSugar with GuiceOneAppPerSuite with BeforeAndAfterEach {

  import org.mockito.Matchers.{any, eq => is}

  val validUtr = SaUtr("1234567890")

  val request = new AuthenticatedRequest(FakeRequest(), None, None, None, None)

  val hc = new HeaderCarrier()

  val mockEntityResolverConnector = mock[EntityResolverConnector]
  val mockAuthConnector = mock[AuthConnector]
  val mockAuditConnector = mock[AuditConnector]
  val mockEmailConnector = mock[EmailConnector]
  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "govuk-tax.Test.preferences-frontend.host" -> ""
      )
      .overrides(
        bind[AuditConnector].toInstance(mockAuditConnector),
        bind[AuthConnector].toInstance(mockAuthConnector),
        bind[EntityResolverConnector].toInstance(mockEntityResolverConnector),
        bind[EmailConnector].toInstance(mockEmailConnector)
      )
      .build()
  val controller = app.injector.instanceOf[ManagePaperlessController]

  type AuthRetrievals = Option[Name] ~ LoginTimes ~ Option[String] ~ Option[String]

  val currentLogin = new DateTime(2015, 1, 1, 12, 0).withZone(DateTimeZone.UTC)
  val previousLogin = new DateTime(2012, 1, 1, 12, 0).withZone(DateTimeZone.UTC)

  val retrievalResult: Future[Option[Name] ~ LoginTimes ~ Option[String] ~ Option[String]] =
    Future.successful(
      new ~(
        new ~(
          new ~(Some(Name(Some("Alex"), Some("Brown"))), LoginTimes(currentLogin, Some(previousLogin))),
          //Some("AB123456D")),
          Option.empty[String]),
        Some("1234567890")
      ))

  when(mockAuthConnector.authorise[AuthRetrievals](any(), any())(any(), any()))
    .thenReturn(retrievalResult)

  override def beforeEach(): Unit = {
    reset(mockEntityResolverConnector)
    reset(mockAuditConnector)
    reset(mockEmailConnector)
  }

  "clicking on Change email address link in the account details page" should {
    "display update email address form when accessed from Account Details" in {
      val saPreferences =
        SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.Verified)))
          .toNewPreference()
      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(Future.successful(Some(saPreferences)))

      val result = controller._displayChangeEmailAddress(None)(request, TestFixtures.sampleHostContext, hc)

      status(result) mustBe 200
      val page = Jsoup.parse(contentAsString(result))

      page.getElementById("current-email-address").text mustBe "test@test.com."
      page.getElementById("email.main") mustNot be(null)
      page.getElementById("email.main").attr("value") mustBe ""
      page.getElementById("email.confirm") mustNot be(null)
      page.getElementById("email.confirm").attr("value") mustBe ""
    }

    "display update email address form with the email input field pre-populated when coming back from the warning page" in {
      val saPreferences =
        SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.Verified)))
          .toNewPreference()
      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(Future.successful(Some(saPreferences)))

      val existingEmailAddress = "existing@email.com"
      val result = controller._displayChangeEmailAddress(Some(Encrypted(EmailAddress(existingEmailAddress))))(
        request,
        TestFixtures.sampleHostContext,
        hc)

      status(result) mustBe 200
      val page = Jsoup.parse(contentAsString(result))

      page.getElementById("current-email-address").text mustBe "test@test.com."
      page.getElementById("email.main") mustNot be(null)
      page.getElementById("email.main").attr("value") mustBe existingEmailAddress
      page.getElementById("email.confirm") mustNot be(null)
      page.getElementById("email.confirm").attr("value") mustBe existingEmailAddress
    }

    "return bad request if the SA user has opted into paper" in {

      val saPreferences = SaPreference(false, None).toNewPreference()
      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(Future.successful(Some(saPreferences)))

      val result = controller._displayChangeEmailAddress(None)(request, TestFixtures.sampleHostContext, hc)

      status(result) mustBe 400
    }
  }

  "Clicking Resend validation email link on account details page" should {

    "call preferences as if opting-in and send the email as a part of the process" in {

      val saPreferences =
        SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.Pending))).toNewPreference()

      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(Future.successful(Some(saPreferences)))
      when(mockEntityResolverConnector.changeEmailAddress(is("test@test.com"))(any()))
        .thenReturn(Future.successful((HttpResponse(OK))))

      val page = controller._resendVerificationEmail(request, TestFixtures.sampleHostContext, hc)

      status(page) mustBe 200
      val document = Jsoup.parse(contentAsString(page))
      document.getElementById("verification-mail-message") must not be null
      document.getElementById("return-to-dashboard-button").attr("href") must be(
        TestFixtures.sampleHostContext.returnUrl)

      verify(mockEntityResolverConnector).changeEmailAddress(is("test@test.com"))(any())
    }
  }

  "Viewing the email address change thank you page" should {

    "display the confirmation page with the current email address obscured" in {
      val emailAddress = "someone@email.com"
      val saPreferences =
        SaPreference(true, Some(SaEmailPreference(emailAddress, SaEmailPreference.Status.Verified))).toNewPreference()

      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(Future.successful(Some(saPreferences)))

      val page = controller._displayChangeEmailAddressConfirmed(request, TestFixtures.sampleHostContext, hc)

      status(page) mustBe 200

      val doc = Jsoup.parse(contentAsString(page))
      doc.getElementById("updated-email-address") must have('text ("s*****e@email.com"))
      doc.toString must not include emailAddress
    }
  }

  "A post to update email address with no emailVerifiedFlag" should {

    "validate the email address, update the address for SA user and redirect to confirmation page" in {
      val emailAddress = "someone@email.com"
      val saPreferences =
        SaPreference(true, Some(SaEmailPreference("oldEmailAddress@test.com", SaEmailPreference.Status.Verified)))
          .toNewPreference()

      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(Future.successful(true))
      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(Future.successful(Some(saPreferences)))
      when(mockEntityResolverConnector.changeEmailAddress(is(emailAddress))(any()))
        .thenReturn(Future.successful(HttpResponse(NOT_FOUND)))

      val page =
        controller._submitChangeEmailAddress(
          AuthenticatedRequest(
            FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress), ("email.confirm", emailAddress)),
            None,
            None,
            None,
            None),
          TestFixtures.sampleHostContext,
          hc
        )

      status(page) mustBe 303
      header("Location", page).get must include(
        routes.ManagePaperlessController.displayChangeEmailAddressConfirmed(TestFixtures.sampleHostContext).toString())

      verify(mockEntityResolverConnector).changeEmailAddress(is(emailAddress))(any())
      verify(mockEmailConnector).isValid(is(emailAddress))(any())
      verify(mockEntityResolverConnector).getPreferences()(any())
      verifyNoMoreInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "show error if the 2 email address fields do not match" in {
      val saPreferences =
        SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.Verified)))
          .toNewPreference()

      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(Future.successful(Some(saPreferences)))

      val page =
        controller._submitChangeEmailAddress(
          AuthenticatedRequest(
            FakeRequest().withFormUrlEncodedBody("email.main" -> "a@a.com", "email.confirm" -> "b@b.com"),
            None,
            None,
            None,
            None),
          TestFixtures.sampleHostContext,
          hc
        )

      status(page) mustBe 400

      val document = Jsoup.parse(contentAsString(page))
      document
        .select("#form-submit-email-address .error-notification")
        .text mustBe "Check your email addresses - they don't match."
    }

    "show error if the email address is not syntactically valid" in {
      val emailAddress = "invalid-email"
      val saPreferences =
        SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.Verified)))
          .toNewPreference()

      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(Future.successful(Some(saPreferences)))
      val page =
        controller._submitChangeEmailAddress(
          AuthenticatedRequest(
            FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress)),
            None,
            None,
            None,
            None),
          TestFixtures.sampleHostContext,
          hc)

      status(page) mustBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select("#form-submit-email-address .error-notification").text mustBe "Enter a valid email address."
    }

    "show error if the email field is empty" in {
      val saPreferences =
        SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.Verified)))
          .toNewPreference()

      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(Future.successful(Some(saPreferences)))

      val page =
        controller._submitChangeEmailAddress(
          AuthenticatedRequest(FakeRequest().withFormUrlEncodedBody(("email.main", "")), None, None, None, None),
          TestFixtures.sampleHostContext,
          hc)

      status(page) mustBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select("#form-submit-email-address .error-notification").text mustBe "Enter a valid email address."
    }

    "show a warning page if the email has a valid structure but does not pass validation by the email micro service" in {

      val emailAddress = "someone@dodgy.domain"
      val saPreferences =
        SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.Verified)))
          .toNewPreference()

      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(Future.successful(false))
      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(Future.successful(Some(saPreferences)))

      val page =
        controller._submitChangeEmailAddress(
          AuthenticatedRequest(
            FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress), ("email.confirm", emailAddress)),
            None,
            None,
            None,
            None),
          TestFixtures.sampleHostContext,
          hc
        )

      status(page) mustBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") mustNot be(null)
      document.select("#emailIsCorrectLink") mustNot be(null)

      verify(mockEmailConnector).isValid(is(emailAddress))(any())
    }

  }

  "A post to set preferences with an emailVerifiedFlag" should {

    "if the verified flag is true, save the preference and redirect to the thank you page without verifying the email address again" in {
      val emailAddress = "someone@email.com"
      val saPreferences =
        SaPreference(true, Some(SaEmailPreference("oldEmailAddress@test.com", SaEmailPreference.Status.Verified)))
          .toNewPreference()

      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(Future.successful(Some(saPreferences)))
      when(mockEntityResolverConnector.changeEmailAddress(is(emailAddress))(any()))
        .thenReturn(Future.successful(HttpResponse(OK)))

      val page =
        controller._submitChangeEmailAddress(
          AuthenticatedRequest(
            FakeRequest().withFormUrlEncodedBody(
              ("email.main", emailAddress),
              ("email.confirm", emailAddress),
              ("emailVerified", "true")),
            None,
            None,
            None,
            None),
          TestFixtures.sampleHostContext,
          hc
        )

      status(page) mustBe 303
      header("Location", page).get must include(
        routes.ManagePaperlessController.displayChangeEmailAddressConfirmed(TestFixtures.sampleHostContext).toString())

      verify(mockEntityResolverConnector).changeEmailAddress(is(emailAddress))(any())
      verify(mockEntityResolverConnector).getPreferences()(any())
      verifyNoMoreInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "if the verified flag is false and the email does not pass validation by the email micro service, display the verify page" in {

      val emailAddress = "someone@dodgy.domain"
      val saPreferences =
        SaPreference(true, Some(SaEmailPreference("oldEmailAddress@test.com", SaEmailPreference.Status.Verified)))
          .toNewPreference()

      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(Future.successful(false))
      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(Future.successful(Some(saPreferences)))

      val page =
        controller._submitChangeEmailAddress(
          AuthenticatedRequest(
            FakeRequest().withFormUrlEncodedBody(
              ("email.main", emailAddress),
              ("email.confirm", emailAddress),
              ("emailVerified", "false")),
            None,
            None,
            None,
            None),
          TestFixtures.sampleHostContext,
          hc
        )

      status(page) mustBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") mustNot be(null)
      document.select("#emailIsCorrectLink") mustNot be(null)

      verify(mockEntityResolverConnector).getPreferences()(any())
      verifyNoMoreInteractions(mockEntityResolverConnector)
      verify(mockEmailConnector).isValid(is(emailAddress))(any())
    }

    "if the verified flag is any value other than true, treat it as false" in {

      val emailAddress = "someone@dodgy.domain"
      val saPreferences =
        SaPreference(true, Some(SaEmailPreference("oldEmailAddress@test.com", SaEmailPreference.Status.Verified)))
          .toNewPreference()

      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(Future.successful(false))
      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(Future.successful(Some(saPreferences)))

      val page =
        controller._submitChangeEmailAddress(
          AuthenticatedRequest(
            FakeRequest().withFormUrlEncodedBody(
              ("email.main", emailAddress),
              ("email.confirm", emailAddress),
              ("emailVerified", "hjgjhghjghjgj")),
            None,
            None,
            None,
            None
          ),
          TestFixtures.sampleHostContext,
          hc
        )

      status(page) mustBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") mustNot be(null)
      document.select("#emailIsCorrectLink") mustNot be(null)

      verify(mockEntityResolverConnector).getPreferences()(any())
      verifyNoMoreInteractions(mockEntityResolverConnector)
      verify(mockEmailConnector).isValid(is(emailAddress))(any())
    }
  }

  "clicking on opt-out of email reminders link in the account details page" should {

    "display the <are you sure> page" in {
      val saPreferences =
        SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.Verified)))
          .toNewPreference()

      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(Future.successful(Some(saPreferences)))

      val result = controller._displayStopPaperless(request, TestFixtures.sampleHostContext, hc)

      status(result) mustBe 200
      val page = Jsoup.parse(contentAsString(result))

      page.getElementById("confirm-opt-out").text mustBe "Stop emails from HMRC"
      page.getElementById("cancel-link").text mustBe "Cancel"
      page.text() must not include "test@test.com"
    }

    "return bad request if the user has not opted into digital" in {
      val saPreferences = SaPreference(false, None).toNewPreference()
      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(Future.successful(Some(saPreferences)))

      val result = controller._displayStopPaperless(request, TestFixtures.sampleHostContext, hc)

      status(result) mustBe 400
    }
  }

  "A post to confirm opt out of email reminders" should {

    "return a redirect to thank you page" in {
      val saPreferences =
        SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.Verified)))
          .toNewPreference()

      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(Future.successful(Some(saPreferences)))
      when(
        mockEntityResolverConnector
          .updateTermsAndConditions(any[TermsAndConditionsUpdate])(any(), any()))
        .thenReturn(Future.successful(PreferencesExists))

      val result = controller._submitStopPaperless(lang = Welsh)(request, TestFixtures.sampleHostContext, hc)

      status(result) mustBe 303
      header("Location", result).get must include(
        routes.ManagePaperlessController.displayStopPaperlessConfirmed(TestFixtures.sampleHostContext).url)
      val page = Jsoup.parse(contentAsString(result))

      verify(mockEntityResolverConnector)
        .updateTermsAndConditions(any[TermsAndConditionsUpdate])(any(), any())
    }
  }

  "A get to display the how to verify my email page" should {

    val saPreferences =
      SaPreference(true, Some(SaEmailPreference("test@test.com", SaEmailPreference.Status.Pending))).toNewPreference()

    "return a 200 with the correct english content" in {

      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(Future.successful(Some(saPreferences)))

      val result = controller._displayHowToVerifyEmail(saPreferences.email.get)(request, TestFixtures.sampleHostContext)

      result.header.status mustBe 200
      val outcome = contentAsString(Future(result))
      outcome must include("test@test.com")
      outcome must include("How to verify your email address")
    }

    "return a 200 with the correct welsh content" in {

      val headers = request.headers.add((HeaderNames.ACCEPT_LANGUAGE, "cy"))
      val welshRequest = AuthenticatedRequest(request.withHeaders(headers), None, None, None, None)

      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(Future.successful(Some(saPreferences)))

      val result = controller._displayHowToVerifyEmail(saPreferences.email.get)(welshRequest, TestFixtures.sampleHostContext)

      result.header.status mustBe 200
      val outcome = contentAsString(Future(result))
      outcome must include("test@test.com")
      outcome must include("Sut i ddilysu’ch cyfeiriad e-bost")
    }
  }
}
