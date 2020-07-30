/*
 * Copyright 2020 HM Revenue & Customs
 *
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
import play.api.libs.json.Json
import play.api.mvc._
import play.api.routing.Router
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{ LoginTimes, Name, ~ }

import scala.concurrent.Future

class ReOptInActivationSpec
    extends PlaySpec with GuiceOneAppPerSuite with BeforeAndAfterEach with MockitoSugar with ScalaFutures {

  val gracePeriod = 10
  val request = FakeRequest()
  val mockEntityResolverConnector = mock[EntityResolverConnector]
  val mockAuthConnector = mock[AuthConnector]
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
  val controller = app.injector.instanceOf[ActivationController]
  val router = app.injector.instanceOf[Router]

  type AuthRetrievals =
    Option[Name] ~ LoginTimes ~ Option[String] ~ Option[String] ~ Option[AffinityGroup] ~ ConfidenceLevel

  "ActivatioController.activate" when {
    "preference's majorVersion is lower than current majorVersion and " when {
      "Affinity group is Individual and " when {
        "ConfidenceLeve == 200" should {
          "return PRECONDITION_FAILED" in new TestCase {
            val prefMajor = CohortCurrent.ipage.majorVersion - 1
            val confidenceLevel = ConfidenceLevel.L200
            val affinityGroup = AffinityGroup.Individual
            initMocks()
            val response: Future[Result] = controller.activate(TestFixtures.sampleHostContext)(request)
            status(response) mustBe PRECONDITION_FAILED
            withClue("response content should have a redirect link to a current ReOptIn page") {
              contentAsJson(response) mustBe (Json.parse(s"""{"redirectUserTo": "$reOptInUrl"}"""))
            }
          }
        }
      }
    }
    "preference's majorVersion is lower than the current majorVersion and " when {
      "Affinity group is Organization and " when {
        "ConfidenceLeve is == 200" should {
          "return OK" in new TestCase {
            val prefMajor = CohortCurrent.ipage.majorVersion - 1
            val confidenceLevel = ConfidenceLevel.L200
            val affinityGroup = AffinityGroup.Organisation
            initMocks()
            val response: Future[Result] = controller.activate(TestFixtures.sampleHostContext)(request)
            status(response) mustBe OK
          }
        }
      }
    }

    "preference's majorVersion is lower than the current majorVersion and " when {
      "Affinity group is Individual and " when {
        "ConfidenceLeve is <  200" should {
          "return OK" in new TestCase {
            val prefMajor = CohortCurrent.ipage.majorVersion - 1
            val confidenceLevel = ConfidenceLevel.L100
            val affinityGroup = AffinityGroup.Individual
            initMocks()
            val response: Future[Result] = controller.activate(TestFixtures.sampleHostContext)(request)
            status(response) mustBe OK
          }
        }
      }
    }

    "preference's majorVersion is lower than the current majorVersion and " when {
      "Affinity group is Individual and " when {
        "ConfidenceLeve is >  200" should {
          "return OK" in new TestCase {
            val prefMajor = CohortCurrent.ipage.majorVersion - 1
            val confidenceLevel = ConfidenceLevel.L300
            val affinityGroup = AffinityGroup.Individual
            initMocks()
            val response: Future[Result] = controller.activate(TestFixtures.sampleHostContext)(request)
            status(response) mustBe OK
          }
        }
      }
    }
    "preference's majorVersion is the same as current majorVersion and " when {
      "Affinity group is Individual and " when {
        "ConfidenceLeve is >= 200" should {
          "return OK" in new TestCase {
            val prefMajor = CohortCurrent.ipage.majorVersion
            val confidenceLevel = ConfidenceLevel.L200
            val affinityGroup = AffinityGroup.Individual
            initMocks()
            val response: Future[Result] = controller.activate(TestFixtures.sampleHostContext)(request)
            status(response) mustBe OK
          }
        }
      }
    }

    "preference's majorVersion is lower than the current majorVersion and " when {
      "there is a pending email in preferneces" when {
        "Affinity group is Individual and " when {
          "ConfidenceLeve is ==  200" should {
            "return OK" in new TestCase {
              override def pendingEmail = Some("foo@bar.com")
              val prefMajor = CohortCurrent.ipage.majorVersion - 1
              val confidenceLevel = ConfidenceLevel.L200
              val affinityGroup = AffinityGroup.Individual
              initMocks()
              val response: Future[Result] = controller.activate(TestFixtures.sampleHostContext)(request)
              status(response) mustBe OK
            }
          }
        }
      }
    }
  }
  trait TestCase {
    val prefMajor: Int
    val confidenceLevel: ConfidenceLevel
    val affinityGroup: AffinityGroup
    def pendingEmail: Option[String] = None

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
        .thenReturn(Future.successful(Right(PreferenceFound(true, Some(email), majorVersion = Some(prefMajor)))))

      when(mockAuthConnector.authorise[AuthRetrievals](any(), any())(any(), any()))
        .thenReturn(retrievalResult())
    }

    val reOptInUrl = routes.ChoosePaperlessController
      .displayForm(
        Some(CohortCurrent.reoptinpage),
        email = None,
        TestFixtures.reOptInHostContext(email.email).copy(cohort = Some(ReOptInPage10)))
  }

}
