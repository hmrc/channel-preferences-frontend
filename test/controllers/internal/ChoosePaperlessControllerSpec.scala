/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.internal

import _root_.connectors._
import helpers.{ Resources, TestFixtures }
import model.{ Encrypted, HostContext, Language }
import org.joda.time.{ DateTime, DateTimeZone }
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{ any, eq => is }
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.retrieve.{ LoginTimes, Name, ~ }
import uk.gov.hmrc.auth.core.{ AffinityGroup, AuthConnector, ConfidenceLevel }
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{ EventTypes, MergedDataEvent }

import scala.concurrent.Future
import org.mockito.Matchers.{ eq => meq }
import views.html.defaultpages.todo

trait ChoosePaperlessControllerSetup {
  def assignedCohort: OptInCohort = CohortCurrent.ipage
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
}

class ChoosePaperlessControllerSpec
    extends PlaySpec with MockitoSugar with BeforeAndAfterEach with GuiceOneAppPerSuite
    with ChoosePaperlessControllerSetup {

  val mockAuditConnector = mock[AuditConnector]
  val mockEntityResolverConnector = mock[EntityResolverConnector]
  val mockEmailConnector = mock[EmailConnector]
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  when(mockAuthConnector.authorise[AuthRetrievals](any(), any())(any(), any()))
    .thenReturn(retrievalResult)

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[AuditConnector].toInstance(mockAuditConnector),
        bind[AuthConnector].toInstance(mockAuthConnector),
        bind[EntityResolverConnector].toInstance(mockEntityResolverConnector),
        bind[EmailConnector].toInstance(mockEmailConnector)
      )
      .build()

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

  def allGoPaperlessFormElementsArePresent(document: Document) {
    document.getElementById("email.main") mustNot be(null)
    document.getElementById("email.main").attr("value") mustBe ""

    document.getElementById("email.confirm") mustNot be(null)
    document.getElementById("email.confirm").attr("value") mustBe ""

    document.getElementById("opt-in") mustNot be(null)
    document.getElementById("opt-in").attr("checked") mustBe "checked"

    document.getElementById("opt-in-out") mustNot be(null)
    document.getElementById("opt-in-out").attr("checked") mustBe ""

    document.getElementById("terms-and-conditions").attr("href") must endWith(
      "www.tax.service.gov.uk/information/terms#secure"
    )
  }

  "The preferences action on non login version page" should {

    "show main banner" in new ChoosePaperlessControllerSetup {
      val page = controller.displayForm(Some(assignedCohort), None, TestFixtures.sampleHostContext)(request)
      status(page) mustBe 200
      val document = Jsoup.parse(contentAsString(page))
      document.getElementsByTag("nav").attr("class") mustBe "hmrc-sign-out-nav"
    }

    "show main banner for svc" in new ChoosePaperlessControllerSetup {
      val page = controller.displayFormBySvc("mtdfbit", "token", None, TestFixtures.sampleHostContext)(request)
      status(page) mustBe 200
      val document = Jsoup.parse(contentAsString(page))
      document.getElementsByTag("nav").attr("class") mustBe "hmrc-sign-out-nav"
    }

    "have correct form action to save preferences" in new ChoosePaperlessControllerSetup {
      val page = controller.displayForm(Some(assignedCohort), None, TestFixtures.sampleHostContext)(request)
      status(page) mustBe 200
      val document = Jsoup.parse(contentAsString(page))
      val url =
        routes.ChoosePaperlessController
          .submitForm(TestFixtures.sampleHostContext)
          .url
      document.getElementById("form-submit-email-address").attr("action") must endWith(url)
    }

    "have correct form action to save preferences for svc" in new ChoosePaperlessControllerSetup {
      val page = controller.displayFormBySvc("mtdfbit", "token", None, TestFixtures.sampleHostContext)(request)
      status(page) mustBe 200
      val document = Jsoup.parse(contentAsString(page))
      val url =
        routes.ChoosePaperlessController
          .submitFormBySvc("mtdfbit", "token", TestFixtures.sampleHostContext)
          .url
      document.getElementById("form-submit-email-address").attr("action") must endWith(url)
    }

    "audit the cohort information for the account details page" in new ChoosePaperlessControllerSetup {
      reset(mockAuditConnector)
      val page = controller.displayForm(Some(assignedCohort), None, TestFixtures.sampleHostContext)(request)
      status(page) mustBe 200

      val eventArg: ArgumentCaptor[MergedDataEvent] = ArgumentCaptor.forClass(classOf[MergedDataEvent])
      verify(mockAuditConnector).sendMergedEvent(eventArg.capture())(any(), any())

      private val value: MergedDataEvent = eventArg.getValue
      value.auditSource mustBe "channel-preferences-frontend"
      value.auditType mustBe EventTypes.Succeeded
      value.request.tags must contain("transactionName" -> "Show Print Preference Option")
      value.request.detail("cohort") mustBe assignedCohort.toString
      value.request.detail("journey") mustBe "AccountDetails"
      value.request.detail("utr") mustBe validUtr.value
      value.request.detail("nino") mustBe "N/A"
    }

    "audit the cohort information for the account details page for svc" in new ChoosePaperlessControllerSetup {
      reset(mockAuditConnector)
      val page = controller.displayFormBySvc("mtdfbit", "token", None, TestFixtures.sampleHostContext)(request)
      status(page) mustBe 200

      val eventArg: ArgumentCaptor[MergedDataEvent] = ArgumentCaptor.forClass(classOf[MergedDataEvent])
      verify(mockAuditConnector).sendMergedEvent(eventArg.capture())(any(), any())

      private val value: MergedDataEvent = eventArg.getValue
      value.auditSource mustBe "channel-preferences-frontend"
      value.auditType mustBe EventTypes.Succeeded
      value.request.tags must contain("transactionName" -> "Show Print Preference Option")
      value.request.detail("cohort") mustBe assignedCohort.toString
      value.request.detail("journey") mustBe "AccountDetails"
      value.request.detail("utr") mustBe validUtr.value
      value.request.detail("nino") mustBe "N/A"
    }

    "redirect to a re-calculated cohort when no cohort is supplied" in new ChoosePaperlessControllerSetup {
      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(Future.successful(None))

      val page = controller
        .displayForm(cohort = None, emailAddress = None, hostContext = TestFixtures.sampleHostContext)(request)

      status(page) mustBe 303
      header("Location", page).get must be(
        routes.ChoosePaperlessController.displayForm(Some(assignedCohort), None, TestFixtures.sampleHostContext).url
      )
    }
  }

  "The preferences form" should {

    "render an email input field with no value if no email address is supplied, and no option selected" in new ChoosePaperlessControllerSetup {
      val page = controller.displayForm(Some(assignedCohort), None, TestFixtures.sampleHostContext)(request)

      status(page) mustBe 200

      val document = Jsoup.parse(contentAsString(page))

      document.getElementById("email.main").attr("value") mustBe ""
      document.getElementById("opt-in").attr("checked") must be(empty)
      document.getElementById("opt-in-2").attr("checked") must be(empty)
    }

    "render an email input field populated with the supplied email address, and the Opt-in option selected" in new ChoosePaperlessControllerSetup {
      val emailAddress = "bob@bob.com"

      val page = controller.displayForm(
        Some(assignedCohort),
        Some(Encrypted(EmailAddress(emailAddress))),
        TestFixtures.sampleHostContext
      )(request)

      status(page) mustBe 200

      val document = Jsoup.parse(contentAsString(page))

      document.getElementById("email.main") mustNot be(null)
      document.getElementById("email.main").attr("value") mustBe emailAddress
      document.getElementById("email.main").hasAttr("readonly") mustBe false
      document.getElementById("email.main").hasAttr("readonly") mustBe false
      document.getElementById("opt-in").attr("checked") must be(empty)
      document.getElementById("opt-in-2").attr("checked") must be(empty)
    }
  }

  "A post to set preferences with no emailVerifiedFlag" should {

    "show an error if no opt-in preference has been chosen" in new ChoosePaperlessControllerSetup {
      reset(mockEntityResolverConnector)
      reset(mockEmailConnector)
      val page = controller.submitForm(TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody())

      status(page) mustBe 400

      val document = Jsoup.parse(contentAsString(page))
      document.select(".error-notification").text mustBe "Confirm if you want paperless notifications"
      verifyZeroInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    // TODO: fix
    "show an error when opting-in if the email is incorrectly formatted" in new ChoosePaperlessControllerSetup {
      val emailAddress = "invalid-email"

      val page = controller.submitForm(TestFixtures.sampleHostContext)(
        FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", "email.main" -> emailAddress)
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
        .getElementById("email.main-error")
        .childNodes()
        .get(2)
        .toString
        .trim mustBe "Enter an email address in the correct format, like name@example.com"
      verifyZeroInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    // TODO: fix test
    "show an error when opting-in if the T&C's are not accepted" in new ChoosePaperlessControllerSetup {
      override def assignedCohort = CohortCurrent.ipage

      val emailAddress = "someone@email.com"
      val page = controller.submitForm(TestFixtures.sampleHostContext)(
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

    // TODO: tix test
    "show an error when opting-in if the T&C's accepted flag is not present" in new ChoosePaperlessControllerSetup {
      override def assignedCohort = CohortCurrent.ipage

      val emailAddress = "someone@email.com"
      val page = controller.submitForm(TestFixtures.sampleHostContext)(
        FakeRequest()
          .withFormUrlEncodedBody("opt-in" -> "true", "email.main" -> emailAddress, "email.confirm" -> emailAddress)
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

    "show an error when opting-in if the email is not set" in new ChoosePaperlessControllerSetup {

      val page = controller.submitForm(TestFixtures.sampleHostContext)(
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

    "show a warning page when opting-in if the email has a valid structure but does not pass validation by the email micro service" in new ChoosePaperlessControllerSetup {

      val emailAddress = "someone@dodgy.domain"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(Future.successful(false))

      val page = controller.submitForm(TestFixtures.sampleHostContext)(
        FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), "accept-tc" -> "true")
      )

      status(page) mustBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") mustNot be(null)
      document.select("#emailIsCorrectLink") mustNot be(null)
    }

    "when opting-in, validate the email address, save the preference and redirect to the thank you page with the email address encrpyted" in new ChoosePaperlessControllerSetup {
      reset(mockEntityResolverConnector)
      reset(mockEmailConnector)
      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(Future.successful(true))
      when(
        mockEntityResolverConnector
          .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())
      ).thenReturn(Future.successful(PreferencesCreated))

      val page = controller.submitForm(TestFixtures.sampleHostContext)(
        FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), "accept-tc" -> "true")
      )

      status(page) mustBe 303
      header("Location", page).get must include(
        routes.ChoosePaperlessController
          .displayNearlyDone(Some(Encrypted(EmailAddress(emailAddress))), TestFixtures.sampleHostContext)
          .toString
      )

      verify(mockEmailConnector).isValid(is(emailAddress))(any())
      verify(mockEntityResolverConnector)
        .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())

      verifyNoMoreInteractions(mockEntityResolverConnector, mockEmailConnector)
    }
    "when opting-in, validate the email address, failed to save the preference and so not activate user and redirect to the thank you page with the email address encrpyted" in new ChoosePaperlessControllerSetup {
      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(Future.successful(true))
      when(
        mockEntityResolverConnector
          .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())
      ).thenReturn(Future.successful(PreferencesCreated))

      val page = controller.submitForm(TestFixtures.sampleHostContext)(
        FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), "accept-tc" -> "true")
      )

      status(page) mustBe 303
      header("Location", page).get must include(
        routes.ChoosePaperlessController
          .displayNearlyDone(Some(Encrypted(EmailAddress(emailAddress))), TestFixtures.sampleHostContext)
          .toString
      )

      verify(mockEntityResolverConnector)
        .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())

      verify(mockEmailConnector).isValid(is(emailAddress))(any())

      verifyNoMoreInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "when opting-in, validate the email address, save the preference and redirect to the thank you page with the email address encrpyted when the user has no email address stored" in new ChoosePaperlessControllerSetup {
      reset(mockEntityResolverConnector)
      reset(mockEmailConnector)
      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(Future.successful(true))
      when(
        mockEntityResolverConnector
          .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())
      ).thenReturn(Future.successful(PreferencesCreated))

      val page = controller.submitForm(TestFixtures.sampleHostContext)(
        FakeRequest().withFormUrlEncodedBody(
          "opt-in" -> "true",
          ("email.main", emailAddress),
          "accept-tc"          -> "true",
          "emailAlreadyStored" -> "false"
        )
      )

      status(page) mustBe 303
      header("Location", page).get must include(
        routes.ChoosePaperlessController
          .displayNearlyDone(Some(Encrypted(EmailAddress(emailAddress))), TestFixtures.sampleHostContext)
          .toString
      )

      verify(mockEmailConnector).isValid(is(emailAddress))(any())
      verify(mockEntityResolverConnector)
        .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())

      verifyNoMoreInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "when opting-in save the preference and redirect return url if the user has already an email (opting in for generic when the user has already opted in for TaxCredits)" in new ChoosePaperlessControllerSetup {
      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(Future.successful(true))
      when(
        mockEntityResolverConnector
          .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())
      ).thenReturn(Future.successful(PreferencesCreated))

      val page = controller.submitForm(TestFixtures.sampleHostContext)(
        FakeRequest().withFormUrlEncodedBody(
          "opt-in" -> "true",
          ("email.main", emailAddress),
          ("email.confirm", emailAddress),
          "accept-tc"          -> "true",
          "emailAlreadyStored" -> "true"
        )
      )

      status(page) mustBe 303
      header("Location", page).get must include(TestFixtures.sampleHostContext.returnUrl)

      verify(mockEmailConnector).isValid(is(emailAddress))(any())
      verify(mockEntityResolverConnector)
        .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())

      verifyNoMoreInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "when opting-out, save the preference and redirect to the thank you page" in new ChoosePaperlessControllerSetup {
      when(
        mockEntityResolverConnector
          .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())
      ).thenReturn(Future.successful(PreferencesCreated))

      val page =
        controller.submitForm(TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody("opt-in" -> "false"))

      status(page) mustBe 303
      header("Location", page).get must be(TestFixtures.sampleHostContext.returnUrl)

      verify(mockEntityResolverConnector)
        .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())

      verifyNoMoreInteractions(mockEntityResolverConnector, mockEmailConnector)
    }
  }

  "A post to set preferences with an emailVerifiedFlag" should {

    "if the verified flag is true, save the preference and redirect to the thank you page without verifying the email address again" in new ChoosePaperlessControllerSetup {
      val emailAddress = "someone@email.com"
      when(
        mockEntityResolverConnector
          .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())
      ).thenReturn(Future.successful(PreferencesCreated))

      val page = controller.submitForm(TestFixtures.sampleHostContext)(
        FakeRequest().withFormUrlEncodedBody(
          "opt-in" -> "true",
          ("email.main", emailAddress),
          ("email.confirm", emailAddress),
          ("emailVerified", "true"),
          "accept-tc" -> "true"
        )
      )

      status(page) mustBe 303
      header("Location", page).get must include(
        routes.ChoosePaperlessController
          .displayNearlyDone(Some(Encrypted(EmailAddress(emailAddress))), TestFixtures.sampleHostContext)
          .toString
      )

      verify(mockEntityResolverConnector)
        .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())

      verifyNoMoreInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "if the verified flag is true, save the preference and redirect to the thank you page without verifying the email address again by svc" in new ChoosePaperlessControllerSetup {
      val emailAddress = "someone@email.com"
      when(
        mockEntityResolverConnector
          .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())
      ).thenReturn(Future.successful(PreferencesCreated))

      val page = controller.submitFormBySvc("mtdfbit", "token", TestFixtures.sampleHostContext)(
        FakeRequest().withFormUrlEncodedBody(
          "opt-in" -> "true",
          ("email.main", emailAddress),
          ("email.confirm", emailAddress),
          ("emailVerified", "true"),
          "accept-tc" -> "true"
        )
      )

      status(page) mustBe 303
      header("Location", page).get must include(
        routes.ChoosePaperlessController
          .displayNearlyDone(Some(Encrypted(EmailAddress(emailAddress))), TestFixtures.sampleHostContext)
          .toString
      )

      verify(mockEntityResolverConnector)
        .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())

      verifyNoMoreInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "if the verified flag is false and the email does not pass validation by the email micro service, display the verify page" in new ChoosePaperlessControllerSetup {

      val emailAddress = "someone@dodgy.domain"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(Future.successful(false))

      val page = controller.submitForm(TestFixtures.sampleHostContext)(
        FakeRequest().withFormUrlEncodedBody(
          "opt-in" -> "true",
          ("email.main", emailAddress),
          ("email.confirm", emailAddress),
          ("emailVerified", "false"),
          "accept-tc" -> "true"
        )
      )

      status(page) mustBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") mustNot be(null)
      document.select("#emailIsCorrectLink") mustNot be(null)

      verifyZeroInteractions(mockEntityResolverConnector)
    }

    "if the verified flag is any value other than true, treat it as false" in new ChoosePaperlessControllerSetup {

      val emailAddress = "someone@dodgy.domain"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(Future.successful(false))

      val page = controller.submitForm(TestFixtures.sampleHostContext)(
        FakeRequest().withFormUrlEncodedBody(
          "opt-in" -> "true",
          ("email.main", emailAddress),
          ("email.confirm", emailAddress),
          ("emailVerified", "hjgjhghjghjgj"),
          "accept-tc" -> "true"
        )
      )

      status(page) mustBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") mustNot be(null)
      document.select("#emailIsCorrectLink") mustNot be(null)

      verifyZeroInteractions(mockEntityResolverConnector)

    }

    "if the verified flag is any value other than true, treat it as false for svc" in new ChoosePaperlessControllerSetup {

      val emailAddress = "someone@dodgy.domain"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(Future.successful(false))

      val page = controller.submitFormBySvc("mtdfbit", "token", TestFixtures.sampleHostContext)(
        FakeRequest().withFormUrlEncodedBody(
          "opt-in" -> "true",
          ("email.main", emailAddress),
          ("email.confirm", emailAddress),
          ("emailVerified", "hjgjhghjghjgj"),
          "accept-tc" -> "true"
        )
      )

      status(page) mustBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") mustNot be(null)
      document.select("#emailIsCorrectLink") mustNot be(null)

      verifyZeroInteractions(mockEntityResolverConnector)
    }
  }

  "An audit event" should {

    "be created as EventTypes.Succeeded when a new user is activated on submitting a print preference from CohortCurrent.ipage" in new ChoosePaperlessControllerSetup {

      override def assignedCohort = CohortCurrent.ipage

      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(Future.successful(true))
      when(
        mockEntityResolverConnector
          .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())
      ).thenReturn(Future.successful(PreferencesCreated))

      val page = controller.submitForm(TestFixtures.sampleHostContext)(
        FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), "accept-tc" -> "true")
      )

      status(page) mustBe 303

      val eventArg: ArgumentCaptor[MergedDataEvent] = ArgumentCaptor.forClass(classOf[MergedDataEvent])
      verify(mockAuditConnector).sendMergedEvent(eventArg.capture())(any(), any())

      private val value: MergedDataEvent = eventArg.getValue
      value.auditSource mustBe "channel-preferences-frontend"
      value.auditType mustBe EventTypes.Succeeded
      value.request.tags must contain("transactionName" -> "Set Print Preference")
      value.request.detail("cohort") mustBe "IPage8"
      value.request.detail("journey") mustBe "AccountDetails"
      value.request.detail("utr") mustBe validUtr.value
      value.request.detail("nino") mustBe "N/A"
      value.request.detail("email") mustBe "someone@email.com"
      value.request.detail("digital") mustBe "true"
      value.request.detail("userConfirmedReadTandCs") mustBe "true"
      value.request.detail("newUserPreferencesCreated") mustBe "true"
    }

    "be created as EventTypes.Succeeded when an existing user is activated on submitting a print preference from CohortCurrent.ipage" in new ChoosePaperlessControllerSetup {

      override def assignedCohort = CohortCurrent.ipage

      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(Future.successful(true))
      when(
        mockEntityResolverConnector
          .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())
      ).thenReturn(Future.successful(PreferencesExists))

      val page = controller.submitForm(TestFixtures.sampleHostContext)(
        FakeRequest().withFormUrlEncodedBody("opt-in" -> "true", ("email.main", emailAddress), "accept-tc" -> "true")
      )

      status(page) mustBe 303

      val eventArg: ArgumentCaptor[MergedDataEvent] = ArgumentCaptor.forClass(classOf[MergedDataEvent])
      verify(mockAuditConnector).sendMergedEvent(eventArg.capture())(any(), any())

      private val value: MergedDataEvent = eventArg.getValue
      value.auditSource mustBe "channel-preferences-frontend"
      value.auditType mustBe EventTypes.Succeeded
      value.request.tags must contain("transactionName" -> "Set Print Preference")
      value.request.detail("cohort") mustBe "IPage8"
      value.request.detail("journey") mustBe "AccountDetails"
      value.request.detail("utr") mustBe validUtr.value
      value.request.detail("nino") mustBe "N/A"
      value.request.detail("email") mustBe "someone@email.com"
      value.request.detail("digital") mustBe "true"
      value.request.detail("userConfirmedReadTandCs") mustBe "true"
      value.request.detail("newUserPreferencesCreated") mustBe "false"
    }

    "be created as EventTypes.Succeeded when choosing to not opt in" in new ChoosePaperlessControllerSetup {

      override def assignedCohort = CohortCurrent.ipage

      when(
        mockEntityResolverConnector
          .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())
      ).thenReturn(Future.successful(PreferencesCreated))

      val page =
        controller.submitForm(TestFixtures.sampleHostContext)(FakeRequest().withFormUrlEncodedBody("opt-in" -> "false"))

      status(page) mustBe 303

      val eventArg: ArgumentCaptor[MergedDataEvent] = ArgumentCaptor.forClass(classOf[MergedDataEvent])
      verify(mockAuditConnector).sendMergedEvent(eventArg.capture())(any(), any())

      private val value: MergedDataEvent = eventArg.getValue
      value.auditSource mustBe "channel-preferences-frontend"
      value.auditType mustBe EventTypes.Succeeded
      value.request.tags must contain("transactionName" -> "Set Print Preference")
      value.request.detail("cohort") mustBe "IPage8"
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

class ChoosePaperlessControllerSpecTC
    extends PlaySpec with MockitoSugar with GuiceOneAppPerSuite with BeforeAndAfterEach
    with ChoosePaperlessControllerSetup {

  override def assignedCohort: OptInCohort = CohortCurrent.ipage

  val mockAuditConnector = mock[AuditConnector]
  val mockEntityResolverConnector = mock[EntityResolverConnector]
  val mockAuthConnector = mock[AuthConnector]
  val mockEmailConnector = mock[EmailConnector]

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[AuthConnector].toInstance(mockAuthConnector),
        bind[AuditConnector].toInstance(mockAuditConnector),
        bind[EntityResolverConnector].toInstance(mockEntityResolverConnector),
        bind[EmailConnector].toInstance(mockEmailConnector)
      )
      .build()

  override def beforeEach(): Unit = {
    reset(mockAuditConnector)
    reset(mockAuthConnector)
    reset(mockEntityResolverConnector)
    reset(mockEmailConnector)

    when(mockEntityResolverConnector.getPreferencesStatus(any())(any()))
      .thenReturn(Future.successful(Right[Int, PreferenceStatus](PreferenceNotFound(None))))
    when(mockEntityResolverConnector.getPreferencesStatusByToken(any(), any(), any())(any()))
      .thenReturn(Future.successful(Right[Int, PreferenceStatus](PreferenceNotFound(None))))
    when(mockAuthConnector.authorise[AuthRetrievals](any(), any())(any(), any()))
      .thenReturn(retrievalResult)
  }

  val controller = app.injector.instanceOf[ChoosePaperlessController]

  def allGoPaperlessFormElementsArePresent(document: Document) {
    document.getElementById("email.main") mustNot be(null)
    document.getElementById("email.main").attr("value") mustBe ""

    document.getElementById("email.confirm") mustNot be(null)
    document.getElementById("email.confirm").attr("value") mustBe ""

    document.getElementById("opt-in") mustNot be(null)
    document.getElementById("opt-in").attr("checked") mustBe "checked"

    document.getElementById("opt-in-2") mustNot be(null)
    document.getElementById("opt-in-2").attr("checked") mustBe ""

    document.getElementById("terms-and-conditions").attr("href") must endWith(
      "www.tax.service.gov.uk/information/terms#secure"
    )
  }

  "The preferences action on non login version page" should {

    "show main banner" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = CohortCurrent.tcpage

      val page = controller.displayForm(Some(assignedCohort), None, TestFixtures.taxCreditsHostContext(""))(request)
      status(page) mustBe 200
      val document = Jsoup.parse(contentAsString(page))
      document.getElementsByTag("nav").attr("class") mustBe "hmrc-sign-out-nav"
    }

    "have correct form action to save preferences" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = CohortCurrent.tcpage

      val page = controller.displayForm(Some(assignedCohort), None, TestFixtures.taxCreditsHostContext(""))(request)
      status(page) mustBe 200
      val document = Jsoup.parse(contentAsString(page))
      document.select("#form-submit-email-address-tc").attr("action") must endWith(
        routes.ChoosePaperlessController.submitForm(TestFixtures.taxCreditsHostContext("")).url
      )
    }

    "audit the cohort information for the account details page" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = CohortCurrent.tcpage

      val page = controller.displayForm(Some(assignedCohort), None, TestFixtures.taxCreditsHostContext(""))(request)
      status(page) mustBe 200

      val eventArg: ArgumentCaptor[MergedDataEvent] = ArgumentCaptor.forClass(classOf[MergedDataEvent])
      verify(mockAuditConnector).sendMergedEvent(eventArg.capture())(any(), any())

      private val value: MergedDataEvent = eventArg.getValue
      value.auditSource mustBe "channel-preferences-frontend"
      value.auditType mustBe EventTypes.Succeeded
      value.request.tags must contain("transactionName" -> "Show Print Preference Option")
      value.request.detail("cohort") mustBe assignedCohort.toString
      value.request.detail("journey") mustBe "AccountDetails"
      value.request.detail("utr") mustBe validUtr.value
      value.request.detail("nino") mustBe "N/A"
    }

    "redirect to a re-calculated cohort when no cohort is supplied" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = CohortCurrent.tcpage

      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(Future.successful(None))

      val page = controller
        .displayForm(cohort = None, emailAddress = None, hostContext = TestFixtures.taxCreditsHostContext(""))(request)

      status(page) mustBe 303
      header("Location", page).get must be(
        routes.ChoosePaperlessController
          .displayForm(Some(assignedCohort), None, TestFixtures.taxCreditsHostContext(""))
          .url
      )
    }
  }

  "The preferences form" should {

    "render an email input field with no value if no email address is supplied, and no option selected" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = CohortCurrent.tcpage

      val page = controller.displayForm(Some(assignedCohort), None, TestFixtures.taxCreditsHostContext(""))(request)

      status(page) mustBe 200

      val document = Jsoup.parse(contentAsString(page))

      document.getElementById("email.main").attr("value") mustBe ""
      document.getElementById("email.confirm").attr("value") mustBe ""
      document.getElementById("opt-in").attr("checked") must be(empty)
      document.getElementById("opt-in-2").attr("checked") must be(empty)
    }

    "render an email input field populated with the supplied email address, and the Opt-in option selected" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = CohortCurrent.tcpage

      val emailAddress = "bob@bob.com"

      val page = controller.displayForm(
        Some(assignedCohort),
        Some(Encrypted(EmailAddress(emailAddress))),
        TestFixtures.taxCreditsHostContext("")
      )(request)

      status(page) mustBe 200

      val document = Jsoup.parse(contentAsString(page))

      document.getElementById("email.main") mustNot be(null)
      document.getElementById("email.main").attr("value") mustBe emailAddress
      document.getElementById("email.main").attr("type") mustBe "hidden"
      document.getElementById("email.confirm") mustNot be(null)
      document.getElementById("email.confirm").attr("value") mustBe emailAddress
      document.getElementById("email.main").attr("type") mustBe "hidden"
      document.getElementById("opt-in").attr("checked") must be(empty)
      document.getElementById("opt-in-2").attr("checked") must be(empty)
    }

    "render an email input field populated with the supplied hidden email address, and no Opt-in option selected if a preferences is not found for terms but an email do exist" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = CohortCurrent.tcpage

      val emailAddress = "bob@bob.com"

      val mockEntityResolverConnector = mock[EntityResolverConnector]
      val emailPreference = EmailPreference(emailAddress, true, false, false, None)
      when(mockEntityResolverConnector.getPreferencesStatus(any())(any()))
        .thenReturn(Future.successful(Right[Int, PreferenceStatus](PreferenceNotFound(Some(emailPreference)))))
      val page = controller.displayForm(
        Some(assignedCohort),
        Some(Encrypted(EmailAddress(emailAddress))),
        TestFixtures.taxCreditsHostContext(emailAddress)
      )(request)

      status(page) mustBe 200

      val document = Jsoup.parse(contentAsString(page))

      document.getElementById("email.main") mustNot be(null)
      document.getElementById("email.main").attr("value") mustBe emailAddress
      document.getElementById("email.main").attr("type") mustBe "hidden"
      document.getElementById("email.confirm") mustNot be(null)
      document.getElementById("email.confirm").attr("value") mustBe emailAddress
      document.getElementById("email.confirm").attr("type") mustBe "hidden"
      document.getElementById("opt-in").attr("checked") must be(empty)
      document.getElementById("opt-in-2").attr("checked") must be(empty)
    }

    "render an email input field populated with the supplied hidden email address, and no Opt-in option selected if a opted out preferences with email is found" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = CohortCurrent.tcpage

      val emailAddress = "bob@bob.com"

      val mockEntityResolverConnector = mock[EntityResolverConnector]
      val emailPreference = EmailPreference(emailAddress, true, false, false, None)
      when(mockEntityResolverConnector.getPreferencesStatus(any())(any()))
        .thenReturn(
          Future
            .successful(Right[Int, PreferenceStatus](PreferenceFound(false, Some(emailPreference), paperless = None)))
        )
      val page = controller.displayForm(
        Some(assignedCohort),
        Some(Encrypted(EmailAddress(emailAddress))),
        TestFixtures.taxCreditsHostContext(emailAddress)
      )(request)

      status(page) mustBe 200

      val document = Jsoup.parse(contentAsString(page))

      document.getElementById("email.main") mustNot be(null)
      document.getElementById("email.main").attr("value") mustBe emailAddress
      document.getElementById("email.main").attr("type") mustBe "hidden"
      document.getElementById("email.confirm") mustNot be(null)
      document.getElementById("email.confirm").attr("value") mustBe emailAddress
      document.getElementById("email.confirm").attr("type") mustBe "hidden"
      document.getElementById("opt-in").attr("checked") must be(empty)
      document.getElementById("opt-in-2").attr("checked") must be(empty)
    }
  }

  "A post to set preferences with no emailVerifiedFlag" should {

    "show an error if no opt-in preference has been chosen" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = CohortCurrent.tcpage

      val page = controller.submitForm(TestFixtures.taxCreditsHostContext(""))(FakeRequest().withFormUrlEncodedBody())

      status(page) mustBe 400

      val document = Jsoup.parse(contentAsString(page))
      document
        .select(".error-notification")
        .text mustBe "Select yes if you are happy for us to store your email address"
      verifyZeroInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "show no error when opting-in if the email is incorrectly formatted (it has been prepopulated)" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = CohortCurrent.tcpage

      val emailAddress = "invalid-email"

      val page = controller.submitForm(TestFixtures.taxCreditsHostContext(emailAddress))(
        FakeRequest().withFormUrlEncodedBody(
          "termsAndConditions.opt-in"    -> "true",
          "email.main"                   -> emailAddress,
          "email.confirm"                -> emailAddress,
          "termsAndConditions.accept-tc" -> "true",
          "emailAlreadyStored"           -> "true"
        )
      )

      status(page) mustBe 400

      val document = Jsoup.parse(contentAsString(page))

      verifyZeroInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "show an error when opting-in if the T&C's are not accepted" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = CohortCurrent.tcpage

      val emailAddress = "someone@email.com"
      val page = controller.submitForm(TestFixtures.taxCreditsHostContext(""))(
        FakeRequest().withFormUrlEncodedBody(
          "termsAndConditions.opt-in"    -> "true",
          "email.main"                   -> emailAddress,
          "email.confirm"                -> emailAddress,
          "termsAndConditions.accept-tc" -> "false"
        )
      )

      status(page) mustBe 400

      val document = Jsoup.parse(contentAsString(page))
      document
        .getElementById("accept-tc-error")
        .childNode(2)
        .toString
        .trim mustBe "You must agree to the terms and conditions if you are happy for us to store your email address"
      verifyZeroInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "not show an error when opting-out if the T&C's are not selected" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = CohortCurrent.tcpage

      when(
        mockEntityResolverConnector
          .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())
      ).thenReturn(Future.successful(PreferencesCreated))

      val emailAddress = "someone@email.com"
      val page = controller.submitForm(TestFixtures.taxCreditsHostContext(""))(
        FakeRequest().withFormUrlEncodedBody(
          "termsAndConditions.opt-in" -> "false",
          "email.main"                -> emailAddress,
          "email.confirm"             -> emailAddress
        )
      )

      status(page) mustBe 303
    }

    "not show an error when opting-in if the T&C's are not selected" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = CohortCurrent.tcpage

      val emailAddress = "someone@email.com"
      val page = controller.submitForm(TestFixtures.taxCreditsHostContext(""))(
        FakeRequest().withFormUrlEncodedBody(
          "termsAndConditions.opt-in" -> "true",
          "email.main"                -> emailAddress,
          "email.confirm"             -> emailAddress
        )
      )

      status(page) mustBe 400

      val document = Jsoup.parse(contentAsString(page))
      document
        .getElementById("accept-tc-error")
        .childNode(2)
        .toString
        .trim mustBe "You must agree to the terms and conditions if you are happy for us to store your email address"
      verifyZeroInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "show an error when opting-in if the T&C's accepted flag is not present" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = CohortCurrent.tcpage

      val emailAddress = "someone@email.com"
      val page = controller.submitForm(TestFixtures.taxCreditsHostContext(emailAddress))(
        FakeRequest().withFormUrlEncodedBody(
          "termsAndConditions.opt-in" -> "true",
          "email.main"                -> emailAddress,
          "email.confirm"             -> emailAddress
        )
      )

      status(page) mustBe 400

      val document = Jsoup.parse(contentAsString(page))
      document
        .getElementById("accept-tc-error")
        .childNode(2)
        .toString
        .trim mustBe "You must agree to the terms and conditions if you are happy for us to store your email address"
      verifyZeroInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "show a warning page when opting-in if the email has a valid structure but does not pass validation by the email micro service" in new ChoosePaperlessControllerSetup {

      override def assignedCohort: OptInCohort = CohortCurrent.tcpage

      val emailAddress = "someone@dodgy.domain"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(Future.successful(false))

      val page = controller.submitForm(TestFixtures.taxCreditsHostContext(""))(
        FakeRequest().withFormUrlEncodedBody(
          "termsAndConditions.opt-in" -> "true",
          ("email.main", emailAddress),
          ("email.confirm", emailAddress),
          "termsAndConditions.accept-tc" -> "true"
        )
      )

      status(page) mustBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") mustNot be(null)
      document.select("#emailIsCorrectLink") mustNot be(null)
    }

    "when opting-in, validate the email address, save the preference and redirect to the thank you page with the email address encrpyted" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = CohortCurrent.tcpage

      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(Future.successful(true))
      when(
        mockEntityResolverConnector
          .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())
      ).thenReturn(Future.successful(PreferencesCreated))

      val page = controller.submitForm(TestFixtures.taxCreditsHostContext(""))(
        FakeRequest().withFormUrlEncodedBody(
          "termsAndConditions.opt-in" -> "true",
          ("email.main", emailAddress),
          ("email.confirm", emailAddress),
          "termsAndConditions.accept-tc" -> "true"
        )
      )

      status(page) mustBe 303
      header("Location", page).get must include(
        routes.ChoosePaperlessController
          .displayNearlyDone(Some(Encrypted(EmailAddress(emailAddress))), TestFixtures.taxCreditsHostContext(""))
          .toString
      )

      verify(mockEmailConnector).isValid(is(emailAddress))(any())
      verify(mockEntityResolverConnector)
        .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())

      verifyNoMoreInteractions(mockEntityResolverConnector, mockEmailConnector)
    }
    "when opting-in, validate the email address, failed to save the preference and so not activate user and redirect to the thank you page with the email address encrpyted" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = CohortCurrent.tcpage

      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(Future.successful(true))
      when(
        mockEntityResolverConnector
          .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())
      ).thenReturn(Future.successful(PreferencesCreated))

      val page = controller.submitForm(TestFixtures.taxCreditsHostContext(""))(
        FakeRequest().withFormUrlEncodedBody(
          "termsAndConditions.opt-in" -> "true",
          ("email.main", emailAddress),
          ("email.confirm", emailAddress),
          "termsAndConditions.accept-tc" -> "true"
        )
      )

      status(page) mustBe 303
      header("Location", page).get must include(
        routes.ChoosePaperlessController
          .displayNearlyDone(Some(Encrypted(EmailAddress(emailAddress))), TestFixtures.taxCreditsHostContext(""))
          .toString
      )

      verify(mockEntityResolverConnector)
        .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())
      verify(mockEmailConnector).isValid(is(emailAddress))(any())

      verifyNoMoreInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "when opting-in, validate the email address, save the preference and redirect to the thank you page with the email address encrpyted when the user has no email address stored" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = CohortCurrent.tcpage

      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(Future.successful(true))
      when(
        mockEntityResolverConnector
          .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())
      ).thenReturn(Future.successful(PreferencesCreated))

      val page = controller.submitForm(TestFixtures.taxCreditsHostContext(""))(
        FakeRequest().withFormUrlEncodedBody(
          "termsAndConditions.opt-in" -> "true",
          ("email.main", emailAddress),
          ("email.confirm", emailAddress),
          "termsAndConditions.accept-tc" -> "true",
          "emailAlreadyStored"           -> "false"
        )
      )

      status(page) mustBe 303
      header("Location", page).get must include(
        routes.ChoosePaperlessController
          .displayNearlyDone(Some(Encrypted(EmailAddress(emailAddress))), TestFixtures.taxCreditsHostContext(""))
          .toString
      )

      verify(mockEmailConnector).isValid(is(emailAddress))(any())
      verify(mockEntityResolverConnector)
        .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())

      verifyNoMoreInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "when opting-in save the preference and redirect return url if the user has already an email (opting in for generic when the user has already opted in for TaxCredits)" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = CohortCurrent.tcpage

      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(Future.successful(true))
      when(
        mockEntityResolverConnector
          .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())
      ).thenReturn(Future.successful(PreferencesCreated))

      val page = controller.submitForm(TestFixtures.taxCreditsHostContext(""))(
        FakeRequest().withFormUrlEncodedBody(
          "termsAndConditions.opt-in" -> "true",
          ("email.main", emailAddress),
          ("email.confirm", emailAddress),
          "termsAndConditions.accept-tc" -> "true",
          "emailAlreadyStored"           -> "true"
        )
      )

      status(page) mustBe 303
      header("Location", page).get must include(TestFixtures.taxCreditsHostContext("").returnUrl)

      verify(mockEmailConnector).isValid(is(emailAddress))(any())
      verify(mockEntityResolverConnector)
        .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())

      verifyNoMoreInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "when opting-out, save the preference and redirect to the thank you page" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = CohortCurrent.tcpage

      when(
        mockEntityResolverConnector
          .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())
      ).thenReturn(Future.successful(PreferencesCreated))

      val page = controller.submitForm(TestFixtures.taxCreditsHostContext(""))(
        FakeRequest().withFormUrlEncodedBody("termsAndConditions.opt-in" -> "false")
      )

      status(page) mustBe 303
      header("Location", page).get must be(TestFixtures.taxCreditsHostContext("").returnUrl)

      verify(mockEntityResolverConnector)
        .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())

      verifyNoMoreInteractions(mockEntityResolverConnector, mockEmailConnector)
    }
  }

  "A post to set preferences with an emailVerifiedFlag" should {

    "if the verified flag is true, save the preference and redirect to the thank you page without verifying the email address again" in new ChoosePaperlessControllerSetup {
      override def assignedCohort: OptInCohort = CohortCurrent.tcpage

      val emailAddress = "someone@email.com"
      when(
        mockEntityResolverConnector
          .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())
      ).thenReturn(Future.successful(PreferencesCreated))

      val page = controller.submitForm(TestFixtures.taxCreditsHostContext(""))(
        FakeRequest().withFormUrlEncodedBody(
          "termsAndConditions.opt-in" -> "true",
          ("email.main", emailAddress),
          ("email.confirm", emailAddress),
          ("emailVerified", "true"),
          "termsAndConditions.accept-tc" -> "true"
        )
      )

      status(page) mustBe 303
      header("Location", page).get must include(
        routes.ChoosePaperlessController
          .displayNearlyDone(Some(Encrypted(EmailAddress(emailAddress))), TestFixtures.taxCreditsHostContext(""))
          .toString
      )

      verify(mockEntityResolverConnector)
        .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())

      verifyNoMoreInteractions(mockEntityResolverConnector, mockEmailConnector)
    }

    "if the verified flag is false and the email does not pass validation by the email micro service, display the verify page" in new ChoosePaperlessControllerSetup {

      override def assignedCohort: OptInCohort = CohortCurrent.tcpage

      val emailAddress = "someone@dodgy.domain"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(Future.successful(false))

      val page = controller.submitForm(TestFixtures.taxCreditsHostContext(""))(
        FakeRequest().withFormUrlEncodedBody(
          "termsAndConditions.opt-in" -> "true",
          ("email.main", emailAddress),
          ("email.confirm", emailAddress),
          ("emailVerified", "false"),
          "termsAndConditions.accept-tc" -> "true"
        )
      )

      status(page) mustBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") mustNot be(null)
      document.select("#emailIsCorrectLink") mustNot be(null)

      verifyZeroInteractions(mockEntityResolverConnector)
    }

    "if the verified flag is any value other than true, treat it as false" in new ChoosePaperlessControllerSetup {

      override def assignedCohort: OptInCohort = CohortCurrent.tcpage

      val emailAddress = "someone@dodgy.domain"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(Future.successful(false))

      val page = controller.submitForm(TestFixtures.taxCreditsHostContext(""))(
        FakeRequest().withFormUrlEncodedBody(
          "termsAndConditions.opt-in" -> "true",
          ("email.main", emailAddress),
          ("email.confirm", emailAddress),
          ("emailVerified", "hjgjhghjghjgj"),
          "termsAndConditions.accept-tc" -> "true"
        )
      )

      status(page) mustBe 200

      val document = Jsoup.parse(contentAsString(page))
      document.select("#emailIsNotCorrectLink") mustNot be(null)
      document.select("#emailIsCorrectLink") mustNot be(null)

      verifyZeroInteractions(mockEntityResolverConnector)
    }
  }

  "An audit event" should {

    "be created as EventTypes.Succeeded when a new user is activated on submitting a print preference from TCPage" in new ChoosePaperlessControllerSetup {

      override def assignedCohort: OptInCohort = CohortCurrent.tcpage

      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(Future.successful(true))
      when(
        mockEntityResolverConnector
          .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())
      ).thenReturn(Future.successful(PreferencesCreated))

      val page = controller.submitForm(TestFixtures.taxCreditsHostContext(""))(
        FakeRequest().withFormUrlEncodedBody(
          "termsAndConditions.opt-in" -> "true",
          ("email.main", emailAddress),
          ("email.confirm", emailAddress),
          "termsAndConditions.accept-tc" -> "true"
        )
      )

      status(page) mustBe 303

      val eventArg: ArgumentCaptor[MergedDataEvent] = ArgumentCaptor.forClass(classOf[MergedDataEvent])
      verify(mockAuditConnector).sendMergedEvent(eventArg.capture())(any(), any())

      private val value: MergedDataEvent = eventArg.getValue
      value.auditSource mustBe "channel-preferences-frontend"
      value.auditType mustBe EventTypes.Succeeded
      value.request.tags must contain("transactionName" -> "Set Print Preference")
      value.request.detail("cohort") mustBe "TCPage9"
      value.request.detail("journey") mustBe "AccountDetails"
      value.request.detail("utr") mustBe validUtr.value
      value.request.detail("nino") mustBe "N/A"
      value.request.detail("email") mustBe "someone@email.com"
      value.request.detail("digital") mustBe "true"
      value.request.detail("userConfirmedReadTandCs") mustBe "true"
      value.request.detail("newUserPreferencesCreated") mustBe "true"
    }

    "be created as EventTypes.Succeeded when an existing user is activated on submitting a print preference from TCPage" in new ChoosePaperlessControllerSetup {

      override def assignedCohort: OptInCohort = CohortCurrent.tcpage

      val emailAddress = "someone@email.com"
      when(mockEmailConnector.isValid(is(emailAddress))(any())).thenReturn(Future.successful(true))
      when(
        mockEntityResolverConnector
          .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())
      ).thenReturn(Future.successful(PreferencesExists))

      val page = controller.submitForm(TestFixtures.taxCreditsHostContext(""))(
        FakeRequest().withFormUrlEncodedBody(
          "termsAndConditions.opt-in" -> "true",
          ("email.main", emailAddress),
          ("email.confirm", emailAddress),
          "termsAndConditions.accept-tc" -> "true"
        )
      )

      status(page) mustBe 303

      val eventArg: ArgumentCaptor[MergedDataEvent] = ArgumentCaptor.forClass(classOf[MergedDataEvent])
      verify(mockAuditConnector).sendMergedEvent(eventArg.capture())(any(), any())

      private val value: MergedDataEvent = eventArg.getValue
      value.auditSource mustBe "channel-preferences-frontend"
      value.auditType mustBe EventTypes.Succeeded
      value.request.tags must contain("transactionName" -> "Set Print Preference")
      value.request.detail("cohort") mustBe "TCPage9"
      value.request.detail("journey") mustBe "AccountDetails"
      value.request.detail("utr") mustBe validUtr.value
      value.request.detail("nino") mustBe "N/A"
      value.request.detail("email") mustBe "someone@email.com"
      value.request.detail("digital") mustBe "true"
      value.request.detail("userConfirmedReadTandCs") mustBe "true"
      value.request.detail("newUserPreferencesCreated") mustBe "false"
    }

    "be created as EventTypes.Succeeded when choosing to not opt in" in new ChoosePaperlessControllerSetup {

      override def assignedCohort: OptInCohort = CohortCurrent.tcpage

      when(
        mockEntityResolverConnector
          .updateTermsAndConditionsForSvc(any[TermsAndConditionsUpdate], any(), any())(any(), any())
      ).thenReturn(Future.successful(PreferencesCreated))

      val page = controller.submitForm(TestFixtures.taxCreditsHostContext(""))(
        FakeRequest().withFormUrlEncodedBody("termsAndConditions.opt-in" -> "false")
      )

      status(page) mustBe 303

      val eventArg: ArgumentCaptor[MergedDataEvent] = ArgumentCaptor.forClass(classOf[MergedDataEvent])
      verify(mockAuditConnector).sendMergedEvent(eventArg.capture())(any(), any())

      private val value: MergedDataEvent = eventArg.getValue
      value.auditSource mustBe "channel-preferences-frontend"
      value.auditType mustBe EventTypes.Succeeded
      value.request.tags must contain("transactionName" -> "Set Print Preference")
      value.request.detail("cohort") mustBe "TCPage9"
      value.request.detail("journey") mustBe "AccountDetails"
      value.request.detail("utr") mustBe validUtr.value
      value.request.detail("nino") mustBe "N/A"
      value.request.detail("email") mustBe ""
      value.request.detail("digital") mustBe "false"
      value.request.detail("userConfirmedReadTandCs") mustBe "false"
      value.request.detail("newUserPreferencesCreated") mustBe "true"
    }
  }

  "The language form" should {

    "render english radio button checked for undefiend preferences" in new ChoosePaperlessControllerSetup {

      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(
        Future.successful(None)
      )

      val page = controller.displayLanguageForm(
        HostContext(
          returnUrl = "someReturnUrl",
          returnLinkText = "someReturnLinkText"
        )
      )(request)

      status(page) mustBe 200

      val document = Jsoup.parse(contentAsString(page))

      document.getElementById("lang").attributes().hasKey("checked") must be(true)
      document.getElementById("lang-2").attributes().hasKey("checked") must be(false)
    }

    "render english radio button checked for English in preferences" in new ChoosePaperlessControllerSetup {
      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(
        Future.successful(
          Some(
            PreferenceResponse(
              termsAndConditions = Map(),
              email = Some(EmailPreference("test@test.com", false, false, false, None, Some(Language.English)))
            )
          )
        )
      )

      val page = controller.displayLanguageForm(
        HostContext(
          returnUrl = "someReturnUrl",
          returnLinkText = "someReturnLinkText"
        )
      )(request)

      status(page) mustBe 200

      val document = Jsoup.parse(contentAsString(page))

      document.getElementById("lang").attributes().hasKey("checked") must be(true)
      document.getElementById("lang-2").attributes().hasKey("checked") must be(false)
    }

    "render welsh radio button checked for Welsh in preferences" in new ChoosePaperlessControllerSetup {
      when(mockEntityResolverConnector.getPreferences()(any())).thenReturn(
        Future.successful(
          Some(
            PreferenceResponse(
              termsAndConditions = Map(),
              email = Some(EmailPreference("test@test.com", false, false, false, None, Some(Language.Welsh)))
            )
          )
        )
      )
      val page = controller.displayLanguageForm(
        HostContext(
          returnUrl = "someReturnUrl",
          returnLinkText = "someReturnLinkText"
        )
      )(request)

      status(page) mustBe 200

      val document = Jsoup.parse(contentAsString(page))

      document.getElementById("lang").attributes().hasKey("checked") must be(false)
      document.getElementById("lang-2").attributes().hasKey("checked") must be(true)
    }
  }

  "A post to submitLanguageForm" should {
    "send a request to entityResolverConnector with Welsh" in new ChoosePaperlessControllerSetup {

      val requestHostContext = HostContext(
        returnUrl = "someReturnUrl",
        returnLinkText = "someReturnLinkText"
      )

      when(
        mockEntityResolverConnector
          .updateTermsAndConditions(meq(TermsAndConditionsUpdate.fromLanguage(Some(Language.Welsh))))(
            any(),
            meq(requestHostContext)
          )
      ).thenReturn(Future.successful(PreferencesCreated))

      val page =
        controller.submitLanguageForm(requestHostContext)(FakeRequest().withFormUrlEncodedBody("language" -> "true"))

      status(page) mustBe 303

    }

    "send a request to entityResolverConnector with English" in new ChoosePaperlessControllerSetup {

      val requestHostContext = HostContext(
        returnUrl = "someReturnUrl",
        returnLinkText = "someReturnLinkText"
      )
      when(
        mockEntityResolverConnector
          .updateTermsAndConditions(meq(TermsAndConditionsUpdate.fromLanguage(Some(Language.English))))(
            any(),
            meq(requestHostContext)
          )
      ).thenReturn(Future.successful(PreferencesCreated))

      val page =
        controller.submitLanguageForm(requestHostContext)(FakeRequest().withFormUrlEncodedBody("language" -> "false"))

      status(page) mustBe 303

    }

    "return 400 if form is invalid" in new ChoosePaperlessControllerSetup {

      val requestHostContext = HostContext(
        returnUrl = "someReturnUrl",
        returnLinkText = "someReturnLinkText"
      )
      val page =
        controller.submitLanguageForm(requestHostContext)(FakeRequest().withFormUrlEncodedBody("language" -> "foobar"))

      status(page) mustBe 400

    }

  }

}

class ChoosePaperlessControllerSpecAdmin extends PlaySpec with GuiceOneAppPerSuite with ChoosePaperlessControllerSetup {

  val controller = app.injector.instanceOf[ChoosePaperlessController]

  "/paperless/opt-in-cohort/display/:cohort" should {

    "display form of the current ipage cohort" in {
      val request = FakeRequest()
      val page = controller.displayCohort(Some(CohortCurrent.ipage))(request)
      status(page) mustBe 200
    }
    "return BadRequest if cohort is missing" in {
      val request = FakeRequest()
      val page = controller.displayCohort(None)(request)
      status(page) mustBe 400
    }
  }

  "/paperless/opt-in-cohort/list" should {

    "return list of available cohorts" in {
      val request = FakeRequest()
      val page = controller.cohortList()(request)
      status(page) mustBe 200
      contentAsJson(page) mustBe (
        Resources.readJson("CohortList.json")
      )
    }
  }
}
