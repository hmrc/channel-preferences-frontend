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
import helpers.TestFixtures
import model.Language
import org.joda.time.{ DateTime, DateTimeZone }
import org.jsoup.Jsoup
import org.mockito.Matchers.{ any, eq => is }
import org.mockito.Mockito.{ when, _ }
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Results.{ NotFound, Ok, PreconditionFailed }
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{ LoginTimes, Name, ~ }

import scala.concurrent.Future

class ActivationControllerSpec
    extends PlaySpec with GuiceOneAppPerSuite with BeforeAndAfterEach with MockitoSugar with ScalaFutures {

  import scala.concurrent.ExecutionContext.Implicits.global

  private val gracePeriod = 10
  private val request = FakeRequest()
  private val mockEntityResolverConnector = mock[EntityResolverConnector]
  private val mockAuthConnector = mock[AuthConnector]
  private val updatedAtLong = 1599130916579L

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "govuk-tax.Test.preferences-frontend.host" -> "",
        "Test.activation.gracePeriodInMin"         -> gracePeriod
      )
      .overrides(
        bind[AuthConnector].toInstance(mockAuthConnector),
        bind[EntityResolverConnector].toInstance(mockEntityResolverConnector)
      )
      .build()
  private val controller = app.injector.instanceOf[ActivationController]

  type AuthRetrievals =
    Option[Name] ~ LoginTimes ~ Option[String] ~ Option[String] ~ Option[AffinityGroup] ~ ConfidenceLevel

  private val currentLogin = new DateTime(2015, 1, 1, 12, 0).withZone(DateTimeZone.UTC)
  private val previousLogin = new DateTime(2012, 1, 1, 12, 0).withZone(DateTimeZone.UTC)

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

  when(mockAuthConnector.authorise[AuthRetrievals](any(), any())(any(), any()))
    .thenReturn(retrievalResult)
  "The Activation with an AuthContext" should {
    "store the current user's language stored in the journey cookie when there is no language setting in preferences and" should {
      "return a json body with optedIn set to true if preference is found and opted-in and no alreadyOptedInUrl is present" in {
        val email = EmailPreference("test@test.com", isVerified = false, hasBounces = false, mailboxFull = false, None)
        reset(mockEntityResolverConnector)
        when(mockEntityResolverConnector.getPreferencesStatus(any())(any()))
          .thenReturn(Future.successful(Right(PreferenceFound(accepted = true, Some(email), paperless = None))))
        when(
          mockEntityResolverConnector
            .updateTermsAndConditions(is(TermsAndConditionsUpdate.fromLanguage(Some(Language.English))))(
              any(),
              is(TestFixtures.sampleHostContext)
            )
        )
          .thenReturn(Future.successful(PreferencesCreated))
        val cookies = Cookie("PLAY_LANG", "en")
        val res: Future[Result] = controller.activate(TestFixtures.sampleHostContext)(request.withCookies(cookies))
        status(res) mustBe Ok.header.status
        val document = Jsoup.parse(contentAsString(res))
        document.getElementsByTag("body").first().html() must include("""{"optedIn":true,"verifiedEmail":false}""")
        verify(mockEntityResolverConnector, times(1))
          .updateTermsAndConditions(is(TermsAndConditionsUpdate.fromLanguage(Some(Language.English))))(
            any(),
            is(TestFixtures.sampleHostContext)
          )
      }
      "redirect to the alreadyOptedInUrl if preference is found and opted-in and an alreadyOptedInUrl is present" in {
        val email = EmailPreference("test@test.com", isVerified = false, hasBounces = false, mailboxFull = false, None)
        reset(mockEntityResolverConnector)
        when(mockEntityResolverConnector.getPreferencesStatus(any())(any()))
          .thenReturn(Future.successful(Right(PreferenceFound(accepted = true, Some(email), paperless = None))))
        when(
          mockEntityResolverConnector
            .updateTermsAndConditions(is(TermsAndConditionsUpdate.fromLanguage(Some(Language.Welsh))))(
              any(),
              is(TestFixtures.alreadyOptedInUrlHostContext)
            )
        )
          .thenReturn(Future.successful(PreferencesCreated))
        val cookies = Cookie("PLAY_LANG", "cy")
        val res: Future[Result] =
          controller.activate(TestFixtures.alreadyOptedInUrlHostContext)(request.withCookies(cookies))

        status(res) mustBe SEE_OTHER
        res.map { result =>
          result.header.headers must contain("Location")
          result.header.headers.get("Location") mustBe TestFixtures.alreadyOptedInUrlHostContext.alreadyOptedInUrl.get
        }
        verify(mockEntityResolverConnector, times(1))
          .updateTermsAndConditions(is(TermsAndConditionsUpdate.fromLanguage(Some(Language.Welsh))))(
            any(),
            is(TestFixtures.alreadyOptedInUrlHostContext)
          )
      }

      "not attempt to store in preferences the user's language held in the user's cookie when there is an existing language setting in preferences and" should {
        "return a json body with optedIn set to true if preference is found, opted-in and no alreadyOptedInUrl is present" in {
          val email = EmailPreference(
            "test@test.com",
            isVerified = false,
            hasBounces = false,
            mailboxFull = false,
            None,
            Some(Language.Welsh)
          )
          reset(mockEntityResolverConnector)
          when(mockEntityResolverConnector.getPreferencesStatus(any())(any()))
            .thenReturn(Future.successful(Right(PreferenceFound(accepted = true, Some(email), paperless = None))))
          val cookies = Cookie("PLAY_LANG", "cy")
          val res: Future[Result] = controller.activate(TestFixtures.sampleHostContext)(request.withCookies(cookies))
          status(res) mustBe Ok.header.status
          val document = Jsoup.parse(contentAsString(res))
          document.getElementsByTag("body").first().html() must include("""{"optedIn":true,"verifiedEmail":false}""")
          verify(mockEntityResolverConnector, never()).updateTermsAndConditions(any())(any(), any())
        }

        "redirect to the alreadyOptedInUrl if preference is found, opted-in and an alreadyOptedInUrl is present" in {
          val email = EmailPreference(
            "test@test.com",
            isVerified = false,
            hasBounces = false,
            mailboxFull = false,
            None,
            Some(Language.English)
          )
          reset(mockEntityResolverConnector)
          when(mockEntityResolverConnector.getPreferencesStatus(any())(any()))
            .thenReturn(Future.successful(Right(PreferenceFound(accepted = true, Some(email), paperless = None))))
          val cookies = Cookie("PLAY_LANG", "cy")
          val res: Future[Result] =
            controller.activate(TestFixtures.alreadyOptedInUrlHostContext)(request.withCookies(cookies))

          status(res) mustBe SEE_OTHER
          res.map { result =>
            result.header.headers must contain("Location")
            result.header.headers.get("Location") mustBe TestFixtures.alreadyOptedInUrlHostContext.alreadyOptedInUrl.get
          }
          verify(mockEntityResolverConnector, never()).updateTermsAndConditions(any())(any(), any())
        }
      }

      "not attempt to store in preferences the user's language held in the user's cookie when user is not opted-in and" should {
        "return a json body with optedIn set to false if preference is found, opted-out and no alreadyOptedInUrl is present" in {
          val email =
            EmailPreference("test@test.com", isVerified = false, hasBounces = false, mailboxFull = false, None)
          reset(mockEntityResolverConnector)
          when(mockEntityResolverConnector.getPreferencesStatus(any())(any()))
            .thenReturn(Future.successful(Right(PreferenceFound(accepted = false, Some(email), paperless = None))))
          val res: Future[Result] = controller.activate(TestFixtures.sampleHostContext)(request)

          status(res) mustBe Ok.header.status
          verify(mockEntityResolverConnector, never()).updateTermsAndConditions(any())(any(), any())
        }

        "return OK if customer is opted-out but with existing email" in {
          val email =
            EmailPreference("test@test.com", isVerified = false, hasBounces = false, mailboxFull = false, None)
          reset(mockEntityResolverConnector)
          when(mockEntityResolverConnector.getPreferencesStatus(any())(any()))
            .thenReturn(Future.successful(Right(PreferenceFound(accepted = false, Some(email), paperless = None))))
          val res: Future[Result] = controller.activate(TestFixtures.alreadyOptedInUrlHostContext)(request)

          status(res) mustBe OK
          verify(mockEntityResolverConnector, never()).updateTermsAndConditions(any())(any(), any())
        }
      }

      "not attempt to store in preferences the user's language held in the user's cookie when user has not accepted T&C and" should {
        "return a json body with optedIn set to false if T&C accepted is false and updatedAt is within the grace period" in {
          val now = DateTime.now
          val lastUpdated = now.minusMinutes(gracePeriod - gracePeriod / 2)
          val email =
            EmailPreference("test@test.com", isVerified = false, hasBounces = false, mailboxFull = false, None)

          reset(mockEntityResolverConnector)
          when(mockEntityResolverConnector.getPreferencesStatus(any())(any()))
            .thenReturn(
              Future.successful(
                Right(PreferenceFound(accepted = false, Some(email), Some(lastUpdated), paperless = None))
              )
            )
          val res: Future[Result] = controller.activate(TestFixtures.sampleHostContext)(request)

          status(res) mustBe Ok.header.status
          val document = Jsoup.parse(contentAsString(res))
          document.getElementsByTag("body").first().html() must include("""{"optedIn":false}""")
          verify(mockEntityResolverConnector, never()).updateTermsAndConditions(any())(any(), any())
        }

        "return a json body with optedIn set to false if T&C accepted is false and updatedAt is outside of the grace period" in {
          val now = DateTime.now
          val lastUpdated = now.minusMinutes(gracePeriod + gracePeriod / 2)
          val email =
            EmailPreference("test@test.com", isVerified = false, hasBounces = false, mailboxFull = false, None)

          reset(mockEntityResolverConnector)
          when(mockEntityResolverConnector.getPreferencesStatus(any())(any()))
            .thenReturn(
              Future.successful(
                Right(PreferenceFound(accepted = false, Some(email), Some(lastUpdated), paperless = None))
              )
            )
          val res: Future[Result] = controller.activate(TestFixtures.sampleHostContext)(request)

          status(res) mustBe Ok.header.status
          val document = Jsoup.parse(contentAsString(res))
          document.getElementsByTag("body").first().html() must include("""{"optedIn":false}""")
          verify(mockEntityResolverConnector, never()).updateTermsAndConditions(any())(any(), any())
        }
      }

      "not attempt to store in preferences the user's language held in the user's cookie when no preferences found and" should {
        "return PRECONDITION failed if no preferences are found and no alreadyOptedInUrl is present" in {
          val email =
            EmailPreference("test@test.com", isVerified = false, hasBounces = false, mailboxFull = false, None)
          reset(mockEntityResolverConnector)
          when(mockEntityResolverConnector.getPreferencesStatus(any())(any()))
            .thenReturn(Future.successful(Right(PreferenceNotFound(Some(email)))))
          val res: Future[Result] = controller.activate(TestFixtures.sampleHostContext)(request)

          status(res) mustBe PRECONDITION_FAILED
          val document = Jsoup.parse(contentAsString(res))
          document.getElementsByTag("body").first().html() must startWith(
            """{"redirectUserTo":"/paperless/choose?email="""
          )
          verify(mockEntityResolverConnector, never()).updateTermsAndConditions(any())(any(), any())
        }

        "return PRECONDITION failed if no preferences are found and an alreadyOptedInUrl is present" in {
          val email =
            EmailPreference("test@test.com", isVerified = false, hasBounces = false, mailboxFull = false, None)
          reset(mockEntityResolverConnector)
          when(mockEntityResolverConnector.getPreferencesStatus(any())(any()))
            .thenReturn(Future.successful(Right(PreferenceNotFound(Some(email)))))
          val res: Future[Result] = controller.activate(TestFixtures.alreadyOptedInUrlHostContext)(request)

          status(res) mustBe PRECONDITION_FAILED
          val document = Jsoup.parse(contentAsString(res))
          document.getElementsByTag("body").first().html() must startWith(
            """{"redirectUserTo":"/paperless/choose?email="""
          )
          verify(mockEntityResolverConnector, never()).updateTermsAndConditions(any())(any(), any())
        }
      }
    }

    "The Activation with a token" should {

      "fail when not supplied with a mtdfbit service" in {
        when(mockEntityResolverConnector.getPreferencesStatusByToken(any(), any(), any())(any()))
          .thenReturn(Future.successful(Left(NOT_FOUND)))
        val res: Future[Result] =
          controller.activateFromToken("svc", "token", TestFixtures.sampleHostContext)(request)
        status(res) mustBe NotFound.header.status
      }

      "succeed when the service is mtdfbit" in {
        val email = EmailPreference("test@test.com", isVerified = false, hasBounces = false, mailboxFull = false, None)
        when(mockEntityResolverConnector.getPreferencesStatusByToken(any(), any(), any())(any()))
          .thenReturn(Future.successful(Right(PreferenceNotFound(Some(email)))))
        val res: Future[Result] =
          controller.activateFromToken("mtdfbit", "token", TestFixtures.sampleHostContext)(request)
        status(res) mustBe PreconditionFailed.header.status
        val document = Jsoup.parse(contentAsString(res))
        document.getElementsByTag("body").first().html() must startWith(
          """{"redirectUserTo":"/paperless/choose/cohort/mtdfbit/token?email="""
        )
      }

      "succeed when the preferences retrieved contain a majorVersion (opt-in feature flag on)" in {
        when(mockEntityResolverConnector.getPreferencesStatusByToken(any(), any(), any())(any()))
          .thenReturn(
            Future.successful(
              Right(
                PreferenceFound(
                  accepted = true,
                  Some(
                    EmailPreference(
                      "test@test.com",
                      isVerified = true,
                      hasBounces = false,
                      mailboxFull = false,
                      None,
                      Some(Language.English)
                    )
                  ),
                  Some(new DateTime(updatedAtLong)),
                  majorVersion = Some(1),
                  paperless = Some(true)
                )
              )
            )
          )
        val res: Future[Result] =
          controller.activateFromToken("mtdfbit", "token", TestFixtures.sampleHostContext)(request)
        status(res) mustBe Ok.header.status
      }

      "succeed when the preferences retrieved do not contain a majorVersion (opt-in feature flag off)" in {
        when(mockEntityResolverConnector.getPreferencesStatusByToken(any(), any(), any())(any()))
          .thenReturn(
            Future.successful(
              Right(
                PreferenceFound(
                  accepted = true,
                  Some(
                    EmailPreference(
                      "test@test.com",
                      isVerified = true,
                      hasBounces = false,
                      mailboxFull = false,
                      None,
                      Some(Language.English)
                    )
                  ),
                  Some(new DateTime(updatedAtLong)),
                  majorVersion = None,
                  paperless = Some(true)
                )
              )
            )
          )
        val res: Future[Result] =
          controller.activateFromToken("mtdfbit", "token", TestFixtures.sampleHostContext)(request)
        status(res) mustBe Ok.header.status
      }

      "store the current user's language stored in the journey cookie when there is no language setting in preferences and" should {
        "return a json body with optedIn set to true if preference is found and opted-in and no alreadyOptedInUrl is present but without a language setting" in {
          val email =
            EmailPreference("test@test.com", isVerified = false, hasBounces = false, mailboxFull = false, None)
          reset(mockEntityResolverConnector)
          when(mockEntityResolverConnector.getPreferencesStatusByToken(any(), any(), any())(any()))
            .thenReturn(Future.successful(Right(PreferenceFound(accepted = true, Some(email), paperless = None))))
          when(
            mockEntityResolverConnector
              .updateTermsAndConditions(is(TermsAndConditionsUpdate.fromLanguage(Some(Language.English))))(
                any(),
                is(TestFixtures.sampleHostContext)
              )
          )
            .thenReturn(Future.successful(PreferencesCreated))
          val cookies = Cookie("PLAY_LANG", "en")
          val res: Future[Result] =
            controller.activateFromToken("mtdfbit", "token", TestFixtures.sampleHostContext)(
              request.withCookies(cookies)
            )

          status(res) mustBe Ok.header.status
          val document = Jsoup.parse(contentAsString(res))
          document.getElementsByTag("body").first().html() must include("""{"optedIn":true,"verifiedEmail":false}""")
          verify(mockEntityResolverConnector, times(1))
            .updateTermsAndConditions(is(TermsAndConditionsUpdate.fromLanguage(Some(Language.English))))(
              any(),
              is(TestFixtures.sampleHostContext)
            )
        }

        "return a json body with optedIn set to true if preference is found and opted-in and an alreadyOptedInUrl is present but without a language setting" in {
          val email =
            EmailPreference("test@test.com", isVerified = false, hasBounces = false, mailboxFull = false, None)
          reset(mockEntityResolverConnector)
          when(mockEntityResolverConnector.getPreferencesStatusByToken(any(), any(), any())(any()))
            .thenReturn(Future.successful(Right(PreferenceFound(accepted = true, Some(email), paperless = None))))
          when(
            mockEntityResolverConnector
              .updateTermsAndConditions(is(TermsAndConditionsUpdate.fromLanguage(Some(Language.Welsh))))(
                any(),
                is(TestFixtures.alreadyOptedInUrlHostContext)
              )
          )
            .thenReturn(Future.successful(PreferencesCreated))
          val cookies = Cookie("PLAY_LANG", "cy")
          val res: Future[Result] =
            controller.activateFromToken("mtdfbit", "token", TestFixtures.alreadyOptedInUrlHostContext)(
              request.withCookies(cookies)
            )

          status(res) mustBe Ok.header.status
          val document = Jsoup.parse(contentAsString(res))
          document.getElementsByTag("body").first().html() must include("""{"optedIn":true,"verifiedEmail":false}""")
          verify(mockEntityResolverConnector, times(1))
            .updateTermsAndConditions(is(TermsAndConditionsUpdate.fromLanguage(Some(Language.Welsh))))(
              any(),
              is(TestFixtures.alreadyOptedInUrlHostContext)
            )
        }
      }

      "not attempt to store in preferences the user's language held in the user's cookie when user has not accepted T&C and" should {
        "return a json body with optedIn set to false and a return sign up URL if preference is found and opted-out" in {
          val email =
            EmailPreference("test@test.com", isVerified = false, hasBounces = false, mailboxFull = false, None)
          reset(mockEntityResolverConnector)
          when(mockEntityResolverConnector.getPreferencesStatusByToken(any(), any(), any())(any()))
            .thenReturn(Future.successful(Right(PreferenceFound(accepted = false, Some(email), paperless = None))))
          val res: Future[Result] =
            controller.activateFromToken("mtdfbit", "token", TestFixtures.sampleHostContext)(request)

          status(res) mustBe Ok.header.status
          val document = Jsoup.parse(contentAsString(res))
          document.getElementsByTag("body").first().html() must startWith(
            """{"optedIn":false,"redirectUserTo":"/paperless/choose/cohort/mtdfbit/token?email="""
          )
          verify(mockEntityResolverConnector, never()).updateTermsAndConditions(any())(any(), any())
        }
      }

      "not attempt to store in preferences the user's language held in the user's cookie when no preferences found" in {
        val email = EmailPreference("test@test.com", isVerified = false, hasBounces = false, mailboxFull = false, None)
        reset(mockEntityResolverConnector)
        when(mockEntityResolverConnector.getPreferencesStatusByToken(any(), any(), any())(any()))
          .thenReturn(Future.successful(Right(PreferenceNotFound(Some(email)))))
        val res: Future[Result] =
          controller.activateFromToken("mtdfbit", "token", TestFixtures.sampleHostContext)(request)

        status(res) mustBe PreconditionFailed.header.status
        val document = Jsoup.parse(contentAsString(res))
        document.getElementsByTag("body").first().html() must startWith("""{"redirectUserTo":""")
        verify(mockEntityResolverConnector, never()).updateTermsAndConditions(any())(any(), any())
      }
    }
  }
}
