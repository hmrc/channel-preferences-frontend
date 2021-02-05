/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers.internal

import _root_.connectors._
import com.kenshoo.play.metrics.Metrics
import helpers.TestFixtures
import org.joda.time.{ DateTime, DateTimeZone }
import org.jsoup.Jsoup
import org.mockito.Matchers.{ any, eq => is }
import org.mockito.{ ArgumentCaptor, Mockito }
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.{ DefaultMessagesApiProvider, Lang }
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.retrieve.{ LoginTimes, Name, ~ }
import uk.gov.hmrc.auth.core.{ AffinityGroup, AuthConnector, ConfidenceLevel }
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{ EventTypes, MergedDataEvent }

import scala.concurrent.Future

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
      .configure("metrics.enabled" -> false)
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
      document.getElementsByTag("nav").attr("class") mustBe "hmrc-sign-out-nav"
    }

    "show reoptin title" in new ChoosePaperlessControllerReOptInSetup {
      val reOptInTitle = messageApi.translate("reoptin_page52.fg_page.title", Nil)(Lang("en", "")).get
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

    "render email address if email is present in hostContext" in new ChoosePaperlessControllerReOptInSetup {
      val emailAddress = "foo@bar.com"
      val page =
        controller.displayForm(Some(assignedCohort), None, TestFixtures.reOptInHostContext(emailAddress))(request)
      status(page) mustBe 200
      val document = Jsoup.parse(contentAsString(page))
      document.getElementById("email.main") mustNot be(null)
      document.getElementById("email.main").attr("value") mustBe emailAddress
      document.getElementById("email.main").hasAttr("readonly") mustBe true
      document.getElementById("opt-in").attr("checked") must be(empty)
      document.getElementById("opt-in-2").attr("checked") must be(empty)
    }

    "render email address with no value if not present in hostContext" in new ChoosePaperlessControllerReOptInSetup {
      val page = controller.displayForm(Some(assignedCohort), None, TestFixtures.reOptInHostContext())(request)
      status(page) mustBe 200
      val document = Jsoup.parse(contentAsString(page))
      document.getElementById("email.main") mustNot be(null)
      document.getElementById("email.main").attr("value") mustBe ""
      document.getElementById("email.main").hasAttr("readonly") mustBe true
      document.getElementById("opt-in").attr("checked") must be(empty)
      document.getElementById("opt-in-2").attr("checked") must be(empty)
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
        FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", "email.main" -> emailAddress)
      )

      status(page) mustBe 400

      val document = Jsoup.parse(contentAsString(page))
      document
        .getElementById("email.main-error")
        .childNodes()
        .get(2)
        .toString
        .trim mustBe "Enter an email address in the correct format, like name@example.com"
      verifyZeroInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "show an error when opting-in if the T&C's are not accepted" in new ChoosePaperlessControllerReOptInSetup {
      override def assignedCohort = CohortCurrent.ipage

      val emailAddress = "someone@email.com"
      val page = controller.submitForm(TestFixtures.reOptInHostContext())(
        FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", "email.main" -> emailAddress, "accept-tc" -> "false")
      )

      status(page) mustBe 400

      val document = Jsoup.parse(contentAsString(page))

      document
        .getElementById("terms-and-conditions")
        .childNodes()
        .get(0)
        .toString
        .trim mustBe "terms and conditions"

      document
        .getElementById("accept-tc-error")
        .childNodes()
        .get(2)
        .toString
        .trim mustBe "You must agree to the terms and conditions to use this service"
      verifyZeroInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "show an error when opting-in if the T&C's accepted flag is not present" in new ChoosePaperlessControllerReOptInSetup {
      override def assignedCohort = CohortCurrent.ipage

      val emailAddress = "someone@email.com"
      val page = controller.submitForm(TestFixtures.reOptInHostContext("foo@bar.com"))(
        FakeRequest()
          .withFormUrlEncodedBody("opt-in" -> "true", "email.main" -> emailAddress, "email.confirm" -> emailAddress)
      )

      status(page) mustBe 400

      val document = Jsoup.parse(contentAsString(page))
      document
        .getElementById("accept-tc-error")
        .childNodes()
        .get(2)
        .toString
        .trim mustBe "You must agree to the terms and conditions to use this service"
      verifyZeroInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "show an error when opting-in if the email is not set" in new ChoosePaperlessControllerReOptInSetup {

      val page = controller.submitForm(TestFixtures.reOptInHostContext())(
        FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", "email.main" -> "", "accept-tc" -> "true")
      )

      status(page) mustBe 400

      val document = Jsoup.parse(contentAsString(page))
      document
        .getElementById("email.main-error")
        .childNodes()
        .get(2)
        .toString
        .trim mustBe "Enter an email address in the correct format, like name@example.com"
      verifyZeroInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "when re-opting-in, do not validate the email address, save the preference and redirect to the survey" in new ChoosePaperlessControllerReOptInSetup {
      reset(mockEntityResolverConnector)
      reset(mockEmailConnector)
      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(Future.successful(true))
      when(
        mockEntityResolverConnector
          .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())
      )
        .thenReturn(Future.successful(PreferencesCreated))
      val testHc = TestFixtures.reOptInHostContext("foo@bar.com")
      val page = controller.submitForm(testHc)(
        FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), "accept-tc" -> "true")
      )

      val checkYourSettingsUrl =
        "/paperless/check-settings?returnUrl=kvXgJfoJJ%2FbmaHgdHhhRpg%3D%3D&returnLinkText=huhgy5odc6KaXfFIMZXkeZjs11wvNGxKPz2CtY8L8GM%3D&email=yCVwXTaKNqm1whFZ7gcFkQ%3D%3D&cohort=u%2Fn1h8qcsJrhpRofXkhmXg%3D%3D"
      header("Location", page).get must be(checkYourSettingsUrl)
      status(page) mustBe 303
      verify(mockEntityResolverConnector)
        .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())
      verifyNoMoreInteractions(mockEntityResolverConnector, mockEmailConnector)
    }
  }

  "An audit event" should {

    "be created as EventTypes.Succeeded when a new user is activated on submitting a print preference from CohortCurrent.reoptinpage" in new ChoosePaperlessControllerReOptInSetup {

      override def assignedCohort = CohortCurrent.ipage

      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(Future.successful(true))
      when(
        mockEntityResolverConnector
          .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())
      )
        .thenReturn(Future.successful(PreferencesCreated))

      val page = controller.submitForm(TestFixtures.reOptInHostContext("foo@bar.com"))(
        FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), "accept-tc" -> "true")
      )

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
          .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())
      )
        .thenReturn(Future.successful(PreferencesExists))

      val page = controller.submitForm(TestFixtures.reOptInHostContext("foo@bar.com"))(
        FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), "accept-tc" -> "true")
      )

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
          .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())
      )
        .thenReturn(Future.successful(PreferencesCreated))

      val page =
        controller.submitForm(TestFixtures.reOptInHostContext("foo@bar.com"))(
          FakeRequest().withFormUrlEncodedBody("opt-in" -> "false")
        )

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
