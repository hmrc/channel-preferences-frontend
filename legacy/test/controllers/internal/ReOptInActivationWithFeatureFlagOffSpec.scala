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
import model.Language.English
import org.joda.time.{ DateTime, DateTimeZone }
import org.mockito.Matchers.any
import org.mockito.Mockito.{ when, _ }
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{ LoginTimes, Name, ~ }

import scala.concurrent.Future

class ReOptInActivationWithFeatureFlagOffSpec
    extends PlaySpec with GuiceOneAppPerSuite with BeforeAndAfterEach with MockitoSugar with ScalaFutures {
  val request = FakeRequest()
  val mockEntityResolverConnector = mock[EntityResolverConnector]
  val mockAuthConnector = mock[AuthConnector]

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "govuk-tax.Test.preferences-frontend.host" -> "",
        "reoptIn.switchOn"                         -> false
      )
      .overrides(
        bind[AuthConnector].toInstance(mockAuthConnector),
        bind[EntityResolverConnector].toInstance(mockEntityResolverConnector)
      )
      .build()
  val controller = app.injector.instanceOf[ActivationController]
  type AuthRetrievals =
    Option[Name] ~ LoginTimes ~ Option[String] ~ Option[String] ~ Option[AffinityGroup] ~ ConfidenceLevel

  "ActivatioController.activate" when {
    "paperless is true and " when {
      "preference's majorVersion is lower than current majorVersion and " when {
        "Affinity group is Individual and " when {
          "ConfidenceLevel == 200 and " when {
            "reoptIn flag is off" should {
              "return OK instead of PRECONDITION_FAILED" in new TestCase(paperless = Some(true)) {
                val response: Future[Result] = controller.activate(TestFixtures.sampleHostContext)(request)
                status(response) mustBe OK
              }
            }
          }
        }
      }
    }
  }

  class TestCase(
    prefMajor: Int = CohortCurrent.ipage.majorVersion - 1,
    confidenceLevel: ConfidenceLevel = ConfidenceLevel.L200,
    affinityGroup: AffinityGroup = AffinityGroup.Individual,
    paperless: Option[Boolean] = Option.empty[Boolean],
    pendingEmail: Option[String] = None
  ) {

    val currentLogin = new DateTime(2015, 1, 1, 12, 0).withZone(DateTimeZone.UTC)
    val previousLogin = new DateTime(2012, 1, 1, 12, 0).withZone(DateTimeZone.UTC)
    def retrievalResult()
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
            Some(affinityGroup)
          ),
          confidenceLevel
        ))
    val email = EmailPreference("test@test.com", false, false, false, None, Some(English), pendingEmail)
    def initMocks() = {
      reset(mockAuthConnector)
      reset(mockEntityResolverConnector)
      when(mockEntityResolverConnector.getPreferencesStatus(any())(any()))
        .thenReturn(
          Future.successful(
            Right(PreferenceFound(true, Some(email), majorVersion = Some(prefMajor), paperless = paperless))))

      when(mockAuthConnector.authorise[AuthRetrievals](any(), any())(any(), any()))
        .thenReturn(retrievalResult())
    }

    val reOptInUrl = routes.ChoosePaperlessController
      .displayForm(
        Some(CohortCurrent.reoptinpage),
        email = None,
        TestFixtures.reOptInHostContext(email.email).copy(cohort = Some(ReOptInPage52)))
    initMocks()
  }
}
