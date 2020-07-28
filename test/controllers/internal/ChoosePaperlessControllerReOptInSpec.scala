/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers.internal

import _root_.connectors._
import helpers.TestFixtures
import model.Encrypted
import org.joda.time.{ DateTime, DateTimeZone }
import org.jsoup.Jsoup
import helpers.Resources
import org.jsoup.nodes.Document
import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.{ AffinityGroup, AuthConnector, ConfidenceLevel }
import uk.gov.hmrc.auth.core.retrieve.{ LoginTimes, Name, ~ }
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{ EventTypes, MergedDataEvent }
import org.mockito.Matchers.{ any, eq => is }
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import model.{ HostContext, Language }

import scala.concurrent.Future
import play.api.libs.json._
import org.mockito.Matchers.{ eq => meq, _ }
import play.api.i18n.Lang
import play.api.i18n.{ DefaultMessagesApiProvider, Langs }

import org.mockito.Mockito
import com.kenshoo.play.metrics.Metrics

trait ChoosePaperlessControllerReOptInSetup {
  def assignedCohort: OptInCohort = CohortCurrent.reoptinpage
  val validUtr = SaUtr("1234567890")
  val request = FakeRequest()

  type AuthRetrievals =
    Option[Name] ~ LoginTimes ~ Option[String] ~ Option[String] ~ Option[AffinityGroup] ~ ConfidenceLevel

  val currentLogin = new DateTime(2015, 1, 1, 12, 0).withZone(DateTimeZone.UTC)
  val previousLogin = new DateTime(2012, 1, 1, 12, 0).withZone(DateTimeZone.UTC)

  val retrievalResult
    : Future[Option[Name] ~ LoginTimes ~ Option[String] ~ Option[String] ~ Option[AffinityGroup] ~ ConfidenceLevel] =
    Future.successful(
      new ~(
        new ~(
          new ~(
            new ~(
              new ~(Some(Name(Some("Alex"), Some("Brown"))), LoginTimes(currentLogin, Some(previousLogin))),
              Option.empty[String]
            ),
            Some("1234567890")
          ),
          Some(AffinityGroup.Individual)
        ),
        ConfidenceLevel.L200
      )
    )
  def paramValue(url: String, param: String): Option[String] =
    url.split(Array('=', '?', '&')).drop(1).sliding(2, 2).map(x => x(0) -> x(1)).toMap.get(param)
}

class ChoosePaperlessControllerReOptInSpec
    extends PlaySpec with MockitoSugar with BeforeAndAfterEach with GuiceOneAppPerSuite
    with ChoosePaperlessControllerReOptInSetup {

  val mockAuditConnector = mock[AuditConnector]
  val mockEntityResolverConnector = mock[EntityResolverConnector]
  val mockEmailConnector = mock[EmailConnector]
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  when(mockAuthConnector.authorise[AuthRetrievals](any(), any())(any(), any()))
    .thenReturn(retrievalResult)

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "sso.encryption.key"          -> "P5xsJ9Nt+quxGZzB4DeLfw==",
        "sso.encryption.previousKeys" -> Seq.empty
      )
      .overrides(
        bind[AuditConnector].toInstance(mockAuditConnector),
        bind[AuthConnector].toInstance(mockAuthConnector),
        bind[EntityResolverConnector].toInstance(mockEntityResolverConnector),
        bind[EmailConnector].toInstance(mockEmailConnector),
        bind[Metrics].toInstance(Mockito.mock(classOf[Metrics]))
      )
      .build()
  val messageApi = fakeApplication.injector.instanceOf[DefaultMessagesApiProvider].get
  override def beforeEach(): Unit = {
    reset(mockAuditConnector)
    reset(mockAuthConnector)
    reset(mockEntityResolverConnector)
    reset(mockEmailConnector)
    when(mockAuthConnector.authorise[AuthRetrievals](any(), any())(any(), any()))
      .thenReturn(retrievalResult)

    when(mockEntityResolverConnector.getPreferencesStatus(any())(any()))
      .thenReturn(Future.successful(Right[Int, PreferenceStatus](PreferenceNotFound(None))))
    when(mockEntityResolverConnector.getPreferencesStatusByToken(any(), any(), any())(any()))
      .thenReturn(Future.successful(Right[Int, PreferenceStatus](PreferenceNotFound(None))))
  }
  val controller = app.injector.instanceOf[ChoosePaperlessController]

  "displayForm for reoptin request" should {

    "show main banner" in new ChoosePaperlessControllerReOptInSetup {
      val page =
        controller.displayForm(Some(assignedCohort), None, TestFixtures.reOptInHostContext("foo@bar.com"))(request)
      status(page) mustBe 200
      val document = Jsoup.parse(contentAsString(page))
      document.getElementsByTag("nav").attr("id") mustBe "proposition-menu"
    }

    "show reoptin title" in new ChoosePaperlessControllerReOptInSetup {
      val reOptInTitle = messageApi.translate("reoptin_page10.fg_page.title", Nil)(Lang("en", "")).get
      val page =
        controller.displayForm(Some(assignedCohort), None, TestFixtures.reOptInHostContext("foo@bar.com"))(request)
      status(page) mustBe 200
      val document = Jsoup.parse(contentAsString(page))
      document.getElementsByTag("title").get(0).text mustBe reOptInTitle
    }

    "have correct form action to save preferences" in new ChoosePaperlessControllerReOptInSetup {
      val page =
        controller.displayForm(Some(assignedCohort), None, TestFixtures.reOptInHostContext("foo@bar.com"))(request)
      status(page) mustBe 200
      val document = Jsoup.parse(contentAsString(page))
      document.select("#form-submit-email-address").attr("action") must endWith(
        routes.ChoosePaperlessController.submitForm(TestFixtures.reOptInHostContext("foo@bar.com")).url
      )
    }

    "render email address  if email is present in hostContext" in new ChoosePaperlessControllerReOptInSetup {
      val emailAddress = "foo@bar.com"
      val page =
        controller.displayForm(Some(assignedCohort), None, TestFixtures.reOptInHostContext(emailAddress))(request)
      status(page) mustBe 200
      val document = Jsoup.parse(contentAsString(page))
      document.getElementById("email.main") mustNot be(null)
      document.getElementById("email.main").attr("value") mustBe emailAddress
      document.getElementById("email.main").hasAttr("readonly") mustBe false
      document.getElementById("email.main").hasAttr("readonly") mustBe false
      document.getElementById("opt-in-in").attr("checked") must be(empty)
      document.getElementById("opt-in-out").attr("checked") must be(empty)
    }

    "render email address with no value if not present in hostContext" in new ChoosePaperlessControllerReOptInSetup {
      val page = controller.displayForm(Some(assignedCohort), None, TestFixtures.reOptInHostContext())(request)
      status(page) mustBe 200
      val document = Jsoup.parse(contentAsString(page))
      document.getElementById("email.main") mustNot be(null)
      document.getElementById("email.main").attr("value") mustBe ""
      document.getElementById("email.main").hasAttr("readonly") mustBe false
      document.getElementById("email.main").hasAttr("readonly") mustBe false
      document.getElementById("opt-in-in").attr("checked") must be(empty)
      document.getElementById("opt-in-out").attr("checked") must be(empty)
    }

    "audit the cohort information for the account details page" in new ChoosePaperlessControllerReOptInSetup {
      reset(mockAuditConnector)
      val page =
        controller.displayForm(Some(assignedCohort), None, TestFixtures.reOptInHostContext("foo@bar.com"))(request)
      status(page) mustBe 200

      val eventArg: ArgumentCaptor[MergedDataEvent] = ArgumentCaptor.forClass(classOf[MergedDataEvent])
      verify(mockAuditConnector).sendMergedEvent(eventArg.capture())(any(), any())

      private val value: MergedDataEvent = eventArg.getValue
      value.auditSource mustBe "preferences-frontend"
      value.auditType mustBe EventTypes.Succeeded
      value.request.tags must contain("transactionName" -> "Show Print Preference Option")
      value.request.detail("cohort") mustBe assignedCohort.toString
      value.request.detail("journey") mustBe "AccountDetails"
      value.request.detail("utr") mustBe validUtr.value
      value.request.detail("nino") mustBe "N/A"
    }

  }
  "A post to set preferences with no emailVerifiedFlag" should {

    "show an error if no opt-in preference has been chosen" in new ChoosePaperlessControllerReOptInSetup {
      reset(mockEntityResolverConnector)
      reset(mockEmailConnector)
      val page =
        controller.submitForm(TestFixtures.reOptInHostContext("foo@bar.com"))(FakeRequest().withFormUrlEncodedBody())

      status(page) mustBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select(".error-notification").text mustBe "Confirm if you want paperless notifications"
      verifyZeroInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "show an error when opting-in if the email is incorrectly formatted" in new ChoosePaperlessControllerReOptInSetup {
      val emailAddress = "invalid-email"

      val page = controller.submitForm(TestFixtures.reOptInHostContext())(
        FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", "email.main" -> emailAddress))

      status(page) mustBe 400

      val document = Jsoup.parse(contentAsString(page))
      document
        .select("#form-submit-email-address .error-notification")
        .text mustBe "Enter a valid email address. You must accept the terms and conditions"
      verifyZeroInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "show an error when opting-in if the T&C's are not accepted" in new ChoosePaperlessControllerReOptInSetup {
      override def assignedCohort = CohortCurrent.ipage

      val emailAddress = "someone@email.com"
      val page = controller.submitForm(TestFixtures.reOptInHostContext())(
        FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", "email.main" -> emailAddress, "accept-tc" -> "false"))

      status(page) mustBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select(".error-notification").text mustBe "You must accept the terms and conditions"
      verifyZeroInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "show an error when opting-in if the T&C's accepted flag is not present" in new ChoosePaperlessControllerReOptInSetup {
      override def assignedCohort = CohortCurrent.ipage

      val emailAddress = "someone@email.com"
      val page = controller.submitForm(TestFixtures.reOptInHostContext("foo@bar.com"))(
        FakeRequest()
          .withFormUrlEncodedBody("opt-in" -> "true", "email.main" -> emailAddress, "email.confirm" -> emailAddress))

      status(page) mustBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select(".error-notification").text mustBe "You must accept the terms and conditions"
      verifyZeroInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "show an error when opting-in if the email is not set" in new ChoosePaperlessControllerReOptInSetup {

      val page = controller.submitForm(TestFixtures.reOptInHostContext())(
        FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", "email.main" -> "", "accept-tc" -> "true"))

      status(page) mustBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select(".error-notification").text mustBe "As you would like to opt in, please enter an email address."
      verifyZeroInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "show a warning page when opting-in if the email has a valid structure but does not pass validation by the email micro service" in new ChoosePaperlessControllerReOptInSetup {

      val emailAddress = "someone@dodgy.domain"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(Future.successful(false))

      val page = controller.submitForm(TestFixtures.reOptInHostContext("foo@bar.com"))(
        FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), "accept-tc" -> "true"))

      status(page) mustBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") mustNot be(null)
      document.select("#emailIsCorrectLink") mustNot be(null)
    }

    "when opting-in, validate the email address, save the preference and redirect to the thank you page with the email address encrpyted" in new ChoosePaperlessControllerReOptInSetup {
      reset(mockEntityResolverConnector)
      reset(mockEmailConnector)
      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(Future.successful(true))
      when(
        mockEntityResolverConnector
          .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any()))
        .thenReturn(Future.successful(PreferencesCreated))

      val page = controller.submitForm(TestFixtures.reOptInHostContext("foo@bar.com"))(
        FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), "accept-tc" -> "true"))

      status(page) mustBe 303
      header("Location", page).get must include(routes.ChoosePaperlessController
        .displayNearlyDone(Some(Encrypted(EmailAddress(emailAddress))), TestFixtures.reOptInHostContext("foo@bar.com"))
        .toString())

      verify(mockEmailConnector).isValid(is(emailAddress))(any())
      verify(mockEntityResolverConnector)
        .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())

      verifyNoMoreInteractions(mockEntityResolverConnector, mockEmailConnector)
    }
    "when opting-in, validate the email address, failed to save the preference and so not activate user and redirect to the thank you page with the email address encrpyted" in new ChoosePaperlessControllerReOptInSetup {
      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(Future.successful(true))
      when(
        mockEntityResolverConnector
          .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any()))
        .thenReturn(Future.successful(PreferencesCreated))

      val page = controller.submitForm(TestFixtures.reOptInHostContext())(
        FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), "accept-tc" -> "true"))

      status(page) mustBe 303
      header("Location", page).get must include(
        routes.ChoosePaperlessController
          .displayNearlyDone(Some(Encrypted(EmailAddress(emailAddress))), TestFixtures.reOptInHostContext())
          .toString())

      verify(mockEntityResolverConnector)
        .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())

      verify(mockEmailConnector).isValid(is(emailAddress))(any())

      verifyNoMoreInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "when opting-in, validate the email address, save the preference and redirect to the thank you page with the email address encrpyted when the user has no email address stored" in new ChoosePaperlessControllerReOptInSetup {
      reset(mockEntityResolverConnector)
      reset(mockEmailConnector)
      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(Future.successful(true))
      when(
        mockEntityResolverConnector
          .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any()))
        .thenReturn(Future.successful(PreferencesCreated))

      val page = controller.submitForm(TestFixtures.reOptInHostContext("foo@bar.com"))(
        FakeRequest().withFormUrlEncodedBody(
          "opt-in" -> "true",
          ("email.main", emailAddress),
          "accept-tc"          -> "true",
          "emailAlreadyStored" -> "false"))

      status(page) mustBe 303
      header("Location", page).get must include(routes.ChoosePaperlessController
        .displayNearlyDone(Some(Encrypted(EmailAddress(emailAddress))), TestFixtures.reOptInHostContext("foo@bar.com"))
        .toString())

      verify(mockEmailConnector).isValid(is(emailAddress))(any())
      verify(mockEntityResolverConnector)
        .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())

      verifyNoMoreInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "when opting-in save the preference and redirect return url if the user has already an email (opting in for generic when the user has already opted in for TaxCredits)" in new ChoosePaperlessControllerReOptInSetup {
      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(Future.successful(true))
      when(
        mockEntityResolverConnector
          .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any()))
        .thenReturn(Future.successful(PreferencesCreated))

      val page = controller.submitForm(TestFixtures.reOptInHostContext("foo@bar.com"))(
        FakeRequest().withFormUrlEncodedBody(
          "opt-in" -> "true",
          ("email.main", emailAddress),
          ("email.confirm", emailAddress),
          "accept-tc"          -> "true",
          "emailAlreadyStored" -> "true"))

      status(page) mustBe 303
      header("Location", page).get must include(TestFixtures.reOptInHostContext("foo@bar.com").returnUrl)

      verify(mockEmailConnector).isValid(is(emailAddress))(any())
      verify(mockEntityResolverConnector)
        .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())

      verifyNoMoreInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "when opting-out, save the preference and redirect to the thank you page" in new ChoosePaperlessControllerReOptInSetup {
      when(
        mockEntityResolverConnector
          .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any()))
        .thenReturn(Future.successful(PreferencesCreated))

      val page =
        controller.submitForm(TestFixtures.reOptInHostContext("foo@bar.com"))(
          FakeRequest().withFormUrlEncodedBody("opt-in" -> "false"))

      status(page) mustBe 303
      header("Location", page).get must be(TestFixtures.reOptInHostContext("foo@bar.com").returnUrl)

      verify(mockEntityResolverConnector)
        .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())

      verifyNoMoreInteractions(mockEntityResolverConnector, mockEmailConnector)
    }
  }

  "A post to set preferences with an emailVerifiedFlag" should {

    "if the verified flag is true, save the preference and redirect to the thank you page without verifying the email address again" in new ChoosePaperlessControllerReOptInSetup {
      val emailAddress = "someone@email.com"
      when(
        mockEntityResolverConnector
          .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any()))
        .thenReturn(Future.successful(PreferencesCreated))

      val page = controller.submitForm(TestFixtures.reOptInHostContext("foo@bar.com"))(
        FakeRequest().withFormUrlEncodedBody(
          "opt-in" -> "true",
          ("email.main", emailAddress),
          ("email.confirm", emailAddress),
          ("emailVerified", "true"),
          "accept-tc" -> "true"))

      status(page) mustBe 303
      header("Location", page).get must include(routes.ChoosePaperlessController
        .displayNearlyDone(Some(Encrypted(EmailAddress(emailAddress))), TestFixtures.reOptInHostContext("foo@bar.com"))
        .toString())

      verify(mockEntityResolverConnector)
        .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())

      verifyNoMoreInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "if the verified flag is true, save the preference and redirect to the thank you page without verifying the email address again by svc" in new ChoosePaperlessControllerReOptInSetup {
      val emailAddress = "someone@email.com"
      when(
        mockEntityResolverConnector
          .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any()))
        .thenReturn(Future.successful(PreferencesCreated))

      val page = controller.submitFormBySvc("mtdfbit", "token", TestFixtures.reOptInHostContext("foo@bar.com"))(
        FakeRequest().withFormUrlEncodedBody(
          "opt-in" -> "true",
          ("email.main", emailAddress),
          ("email.confirm", emailAddress),
          ("emailVerified", "true"),
          "accept-tc" -> "true"))

      status(page) mustBe 303
      header("Location", page).get must include(routes.ChoosePaperlessController
        .displayNearlyDone(Some(Encrypted(EmailAddress(emailAddress))), TestFixtures.reOptInHostContext("foo@bar.com"))
        .toString())

      verify(mockEntityResolverConnector)
        .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())

      verifyNoMoreInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "if the verified flag is false and the email does not pass validation by the email micro service, display the verify page" in new ChoosePaperlessControllerReOptInSetup {

      val emailAddress = "someone@dodgy.domain"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(Future.successful(false))

      val page = controller.submitForm(TestFixtures.reOptInHostContext("foo@bar.com"))(
        FakeRequest().withFormUrlEncodedBody(
          "opt-in" -> "true",
          ("email.main", emailAddress),
          ("email.confirm", emailAddress),
          ("emailVerified", "false"),
          "accept-tc" -> "true"))

      status(page) mustBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") mustNot be(null)
      document.select("#emailIsCorrectLink") mustNot be(null)

      verifyZeroInteractions(mockEntityResolverConnector)
    }

    "if the verified flag is any value other than true, treat it as false" in new ChoosePaperlessControllerReOptInSetup {

      val emailAddress = "someone@dodgy.domain"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(Future.successful(false))

      val page = controller.submitForm(TestFixtures.reOptInHostContext("foo@bar.com"))(
        FakeRequest().withFormUrlEncodedBody(
          "opt-in" -> "true",
          ("email.main", emailAddress),
          ("email.confirm", emailAddress),
          ("emailVerified", "hjgjhghjghjgj"),
          "accept-tc" -> "true"))

      status(page) mustBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") mustNot be(null)
      document.select("#emailIsCorrectLink") mustNot be(null)

      verifyZeroInteractions(mockEntityResolverConnector)

    }

    "if the verified flag is any value other than true, treat it as false for svc" in new ChoosePaperlessControllerReOptInSetup {

      val emailAddress = "someone@dodgy.domain"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(Future.successful(false))

      val page = controller.submitFormBySvc("mtdfbit", "token", TestFixtures.reOptInHostContext("foo@bar.com"))(
        FakeRequest().withFormUrlEncodedBody(
          "opt-in" -> "true",
          ("email.main", emailAddress),
          ("email.confirm", emailAddress),
          ("emailVerified", "hjgjhghjghjgj"),
          "accept-tc" -> "true"))

      status(page) mustBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") mustNot be(null)
      document.select("#emailIsCorrectLink") mustNot be(null)

      verifyZeroInteractions(mockEntityResolverConnector)
    }
  }

  "An audit event" should {

    "be created as EventTypes.Succeeded when a new user is activated on submitting a print preference from CohortCurrent.reoptinpage" in new ChoosePaperlessControllerReOptInSetup {

      override def assignedCohort = CohortCurrent.ipage

      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(Future.successful(true))
      when(
        mockEntityResolverConnector
          .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any()))
        .thenReturn(Future.successful(PreferencesCreated))

      val page = controller.submitForm(TestFixtures.reOptInHostContext("foo@bar.com"))(
        FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), "accept-tc" -> "true"))

      status(page) mustBe 303

      val eventArg: ArgumentCaptor[MergedDataEvent] = ArgumentCaptor.forClass(classOf[MergedDataEvent])
      verify(mockAuditConnector).sendMergedEvent(eventArg.capture())(any(), any())

      private val value: MergedDataEvent = eventArg.getValue
      value.auditSource mustBe "preferences-frontend"
      value.auditType mustBe EventTypes.Succeeded
      value.request.tags must contain("transactionName" -> "Set Print Preference")
      value.request.detail("cohort") mustBe "ReOptInPage10"
      value.request.detail("journey") mustBe "AccountDetails"
      value.request.detail("utr") mustBe validUtr.value
      value.request.detail("nino") mustBe "N/A"
      value.request.detail("email") mustBe "someone@email.com"
      value.request.detail("digital") mustBe "true"
      value.request.detail("userConfirmedReadTandCs") mustBe "true"
      value.request.detail("newUserPreferencesCreated") mustBe "true"
    }

    "be created as EventTypes.Succeeded when an existing user is activated on submitting a print preference from CohortCurrent.reoptinpage" in new ChoosePaperlessControllerReOptInSetup {

      override def assignedCohort = CohortCurrent.ipage

      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(Future.successful(true))
      when(
        mockEntityResolverConnector
          .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any()))
        .thenReturn(Future.successful(PreferencesExists))

      val page = controller.submitForm(TestFixtures.reOptInHostContext("foo@bar.com"))(
        FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), "accept-tc" -> "true"))

      status(page) mustBe 303

      val eventArg: ArgumentCaptor[MergedDataEvent] = ArgumentCaptor.forClass(classOf[MergedDataEvent])
      verify(mockAuditConnector).sendMergedEvent(eventArg.capture())(any(), any())

      private val value: MergedDataEvent = eventArg.getValue
      value.auditSource mustBe "preferences-frontend"
      value.auditType mustBe EventTypes.Succeeded
      value.request.tags must contain("transactionName" -> "Set Print Preference")
      value.request.detail("cohort") mustBe "ReOptInPage10"
      value.request.detail("journey") mustBe "AccountDetails"
      value.request.detail("utr") mustBe validUtr.value
      value.request.detail("nino") mustBe "N/A"
      value.request.detail("email") mustBe "someone@email.com"
      value.request.detail("digital") mustBe "true"
      value.request.detail("userConfirmedReadTandCs") mustBe "true"
      value.request.detail("newUserPreferencesCreated") mustBe "false"
    }

    "be created as EventTypes.Succeeded when choosing to not opt in" in new ChoosePaperlessControllerReOptInSetup {

      override def assignedCohort = CohortCurrent.ipage

      when(
        mockEntityResolverConnector
          .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any()))
        .thenReturn(Future.successful(PreferencesCreated))

      val page =
        controller.submitForm(TestFixtures.reOptInHostContext("foo@bar.com"))(
          FakeRequest().withFormUrlEncodedBody("opt-in" -> "false"))

      status(page) mustBe 303

      val eventArg: ArgumentCaptor[MergedDataEvent] = ArgumentCaptor.forClass(classOf[MergedDataEvent])
      verify(mockAuditConnector).sendMergedEvent(eventArg.capture())(any(), any())

      private val value: MergedDataEvent = eventArg.getValue
      value.auditSource mustBe "preferences-frontend"
      value.auditType mustBe EventTypes.Succeeded
      value.request.tags must contain("transactionName" -> "Set Print Preference")
      value.request.detail("cohort") mustBe "ReOptInPage10"
      value.request.detail("journey") mustBe "AccountDetails"
      value.request.detail("utr") mustBe validUtr.value
      value.request.detail("nino") mustBe "N/A"
      value.request.detail("email") mustBe ""
      value.request.detail("digital") mustBe "false"
      value.request.detail("userConfirmedReadTandCs") mustBe "false"
      value.request.detail("newUserPreferencesCreated") mustBe "true"
    }
  }
}
