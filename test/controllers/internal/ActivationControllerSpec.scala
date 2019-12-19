/*
 * Copyright 2019 HM Revenue & Customs
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
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen, MustMatchers}
import uk.gov.hmrc.auth.core.retrieve.{LoginTimes, Name}
import helpers.TestFixtures
import org.jsoup.Jsoup
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Results.{NotFound, Ok, PreconditionFailed}
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.~
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Future
import org.scalatest.concurrent.ScalaFutures

class ActivationControllerSpec extends PlaySpec with GuiceOneAppPerSuite with BeforeAndAfterEach with MockitoSugar with ScalaFutures {

  import scala.concurrent.ExecutionContext.Implicits.global

  val gracePeriod = 10
  val request = FakeRequest()
  val mockEntityResolverConnector = mock[EntityResolverConnector]
  val mockAuthConnector = mock[AuthConnector]
  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "govuk-tax.Test.preferences-frontend.host" -> "",
        "Test.activation.gracePeriodInMin" -> gracePeriod
      )
      .overrides(
        bind[AuthConnector].toInstance(mockAuthConnector),
        bind[EntityResolverConnector].toInstance(mockEntityResolverConnector)
      )
      .build()
  val controller = app.injector.instanceOf[ActivationController]

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
  "The Activation with an AuthContext" should {
    "return a json body with optedIn set to true if preference is found and opted-in and no alreadyOptedInUrl is present" in {
      val email = EmailPreference("test@test.com", false, false, false, None)
      when(mockEntityResolverConnector.getPreferencesStatus(any())(any()))
        .thenReturn(Future.successful(Right(PreferenceFound(true, Some(email)))))
      val res: Future[Result] = controller.preferencesStatus(TestFixtures.sampleHostContext)(request)
      status(res) mustBe Ok.header.status
      val document = Jsoup.parse(contentAsString(res))
      document.getElementsByTag("body").first().html() must include("""{"optedIn":true,"verifiedEmail":false}""")
    }

    "redirect to the alreadyOptedInUrl if preference is found and opted-in and an alreadyOptedInUrl is present" in {
      val email = EmailPreference("test@test.com", false, false, false, None)
      when(mockEntityResolverConnector.getPreferencesStatus(any())(any()))
        .thenReturn(Future.successful(Right(PreferenceFound(true, Some(email)))))
      val res: Future[Result] = controller.preferencesStatus(TestFixtures.alreadyOptedInUrlHostContext)(request)

      status(res) mustBe SEE_OTHER
      res.map { result =>
        result.header.headers must contain("Location")
        result.header.headers.get("Location") mustBe TestFixtures.alreadyOptedInUrlHostContext.alreadyOptedInUrl.get
      }
    }

    "return a json body with optedIn set to false if preference is found and opted-in and no alreadyOptedInUrl is present" in {
      val email = EmailPreference("test@test.com", false, false, false, None)
      when(mockEntityResolverConnector.getPreferencesStatus(any())(any()))
        .thenReturn(Future.successful(Right(PreferenceFound(false, Some(email)))))
      val res: Future[Result] = controller.preferencesStatus(TestFixtures.sampleHostContext)(request)

      status(res) mustBe Ok.header.status
    }

    "return OK if customer is opted-out but with exsiting email" in {
      val email = EmailPreference("test@test.com", false, false, false, None)
      when(mockEntityResolverConnector.getPreferencesStatus(any())(any()))
        .thenReturn(Future.successful(Right(PreferenceFound(false, Some(email)))))
      val res: Future[Result] = controller.preferencesStatus(TestFixtures.alreadyOptedInUrlHostContext)(request)

      status(res) mustBe OK
    }

    "return a json body with optedIn set to false if T&C accepted is false and updatedAt is within the grace period" in {
      val now = DateTime.now
      val lastUpdated = now.minusMinutes(gracePeriod - gracePeriod/2)
      val email = EmailPreference("test@test.com", false, false, false, None)

      when(mockEntityResolverConnector.getPreferencesStatus(any())(any()))
        .thenReturn(Future.successful(Right(PreferenceFound(false, Some(email), Some(lastUpdated)))))
      val res: Future[Result] = controller.preferencesStatus(TestFixtures.sampleHostContext)(request)

      status(res) mustBe Ok.header.status
      val document = Jsoup.parse(contentAsString(res))
      document.getElementsByTag("body").first().html() must include("""{"optedIn":false}""")
    }

    "return a json body with optedIn set to false if T&C accepted is false and updatedAt is outside of the grace period" in {
      val now = DateTime.now
      val lastUpdated = now.minusMinutes(gracePeriod + gracePeriod/2)
      val email = EmailPreference("test@test.com", false, false, false, None)

      when(mockEntityResolverConnector.getPreferencesStatus(any())(any()))
        .thenReturn(Future.successful(Right(PreferenceFound(false, Some(email), Some(lastUpdated)))))
      val res: Future[Result] = controller.preferencesStatus(TestFixtures.sampleHostContext)(request)

      status(res) mustBe Ok.header.status
      val document = Jsoup.parse(contentAsString(res))
      document.getElementsByTag("body").first().html() must include("""{"optedIn":false}""")
    }

    "return PRECONDITION failed if no preferences are found and no alreadyOptedInUrl is present" in {
      val email = EmailPreference("test@test.com", false, false, false, None)
      when(mockEntityResolverConnector.getPreferencesStatus(any())(any()))
        .thenReturn(Future.successful(Right(PreferenceNotFound(Some(email)))))
      val res: Future[Result] = controller.preferencesStatus(TestFixtures.sampleHostContext)(request)

      status(res) mustBe PRECONDITION_FAILED
      val document = Jsoup.parse(contentAsString(res))
      document.getElementsByTag("body").first().html() must startWith("""{"redirectUserTo":"/paperless/choose?email=""")
    }

    "return PRECONDITION failed if no preferences are found and an alreadyOptedInUrl is present" in {
      val email = EmailPreference("test@test.com", false, false, false, None)
      when(mockEntityResolverConnector.getPreferencesStatus(any())(any()))
        .thenReturn(Future.successful(Right(PreferenceNotFound(Some(email)))))
      val res: Future[Result] = controller.preferencesStatus(TestFixtures.alreadyOptedInUrlHostContext)(request)

      status(res) mustBe PRECONDITION_FAILED
      val document = Jsoup.parse(contentAsString(res))
      document.getElementsByTag("body").first().html() must startWith("""{"redirectUserTo":"/paperless/choose?email=""")
    }
  }

  "The Activation with a token" should {

    "fail when not supplied with a mtdfbit service" in {
      when(mockEntityResolverConnector.getPreferencesStatusByToken(any(), any(), any())(any()))
        .thenReturn(Future.successful(Left(NOT_FOUND)))
      val res: Future[Result] =
        controller.preferencesStatusBySvc("svc", "token", TestFixtures.sampleHostContext)(request)
      status(res) mustBe NotFound.header.status
    }

    "succeed when the service is mtdfbit" in {
      val email = EmailPreference("test@test.com", false, false, false, None)
      when(mockEntityResolverConnector.getPreferencesStatusByToken(any(), any(), any())(any()))
        .thenReturn(Future.successful(Right(PreferenceNotFound(Some(email)))))
      val res: Future[Result] =
        controller.preferencesStatusBySvc("mtdfbit", "token", TestFixtures.sampleHostContext)(request)
      status(res) mustBe PreconditionFailed.header.status
      val document = Jsoup.parse(contentAsString(res))
      document.getElementsByTag("body").first().html() must startWith(
        """{"redirectUserTo":"/paperless/choose/cohort/mtdfbit/token?email=""")
    }

    "return a json body with optedIn set to true if preference is found and opted-in and no alreadyOptedInUrl present" in {
      val email = EmailPreference("test@test.com", false, false, false, None)
      when(mockEntityResolverConnector.getPreferencesStatusByToken(any(), any(), any())(any()))
        .thenReturn(Future.successful(Right(PreferenceFound(true, Some(email)))))
      val res: Future[Result] =
        controller.preferencesStatusBySvc("mtdfbit", "token", TestFixtures.sampleHostContext)(request)

      status(res) mustBe Ok.header.status
      val document = Jsoup.parse(contentAsString(res))
      document.getElementsByTag("body").first().html() must include("""{"optedIn":true,"verifiedEmail":false}""")
    }

    "return a json body with optedIn set to true if preference is found and opted-in and an alreadyOptedInUrl present" in {
      val email = EmailPreference("test@test.com", false, false, false, None)
      when(mockEntityResolverConnector.getPreferencesStatusByToken(any(), any(), any())(any()))
        .thenReturn(Future.successful(Right(PreferenceFound(true, Some(email)))))
      val res: Future[Result] =
        controller.preferencesStatusBySvc("mtdfbit", "token", TestFixtures.alreadyOptedInUrlHostContext)(request)

      status(res) mustBe Ok.header.status
      val document = Jsoup.parse(contentAsString(res))
      document.getElementsByTag("body").first().html() must include("""{"optedIn":true,"verifiedEmail":false}""")
    }

    "return a json body with optedIn set to false and a return sign up URL if preference is found and opted-out" in {
      val email = EmailPreference("test@test.com", false, false, false, None)
      when(mockEntityResolverConnector.getPreferencesStatusByToken(any(), any(), any())(any()))
        .thenReturn(Future.successful(Right(PreferenceFound(false, Some(email)))))
      val res: Future[Result] =
        controller.preferencesStatusBySvc("mtdfbit", "token", TestFixtures.sampleHostContext)(request)

      status(res) mustBe Ok.header.status
      val document = Jsoup.parse(contentAsString(res))
      document.getElementsByTag("body").first().html() must startWith(
        """{"optedIn":false,"redirectUserTo":"/paperless/choose/cohort/mtdfbit/token?email=""")
    }
  }
}

