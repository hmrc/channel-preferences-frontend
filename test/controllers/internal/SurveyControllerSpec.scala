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
import uk.gov.hmrc.play.audit.model.{ EventTypes, ExtendedDataEvent }
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

trait SurveyControllerSetup {

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

class SurveyControllerSpec
    extends PlaySpec with MockitoSugar with BeforeAndAfterEach with GuiceOneAppPerSuite with SurveyControllerSetup {

  val mockAuditConnector = mock[AuditConnector]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  implicit val hc: HeaderCarrier = HeaderCarrier()

  when(mockAuthConnector.authorise[AuthRetrievals](any(), any())(any(), any())).thenReturn(retrievalResult)

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "sso.encryption.key"           -> "P5xsJ9Nt+quxGZzB4DeLfw==",
        "sso.encryption.previousKeys"  -> Seq.empty,
        "survey.ReOptInPage10.enabled" -> true
      )
      .overrides(
        bind[AuditConnector].toInstance(mockAuditConnector),
        bind[AuthConnector].toInstance(mockAuthConnector),
        bind[Metrics].toInstance(Mockito.mock(classOf[Metrics]))
      )
      .build()

  val messageApi = fakeApplication.injector.instanceOf[DefaultMessagesApiProvider].get
  val controller = app.injector.instanceOf[SurveyController]

  override def beforeEach(): Unit = {
    reset(mockAuditConnector)
    reset(mockAuthConnector)
    when(mockAuthConnector.authorise[AuthRetrievals](any(), any())(any(), any())).thenReturn(retrievalResult)
  }

  "displayReOptInDeclinedSurveyForm for survey request" should {

    "show main banner" in new SurveyControllerSetup {
      val page = controller.displayReOptInDeclinedSurveyForm(TestFixtures.reOptInHostContext("foo@bar.com"))(request)
      status(page) mustBe 200
      val document = Jsoup.parse(contentAsString(page))
      document.getElementsByTag("nav").attr("class") mustBe "hmrc-sign-out-nav"
    }

    "show survey title" in new SurveyControllerSetup {
      val reOptInTitle = messageApi.translate("paperless.survey.reoptin_declined.title", Nil)(Lang("en", "")).get
      val page = controller.displayReOptInDeclinedSurveyForm(TestFixtures.reOptInHostContext("foo@bar.com"))(request)
      status(page) mustBe 200
      val document = Jsoup.parse(contentAsString(page))
      document.getElementsByTag("title").get(0).text mustBe reOptInTitle
    }

    "have correct form action to submit the survey" in new SurveyControllerSetup {
      val page = controller.displayReOptInDeclinedSurveyForm(TestFixtures.reOptInHostContext("foo@bar.com"))(request)
      status(page) mustBe 200
      val document = Jsoup.parse(contentAsString(page))
      document.select("#form-submit-survey").attr("action") must endWith(
        routes.SurveyController.submitReOptInDeclinedSurveyForm(TestFixtures.reOptInHostContext("foo@bar.com")).url
      )
    }
  }

  "An audit event" should {

    "be created as EventTypes.Succeeded when a user submits a survey" in new SurveyControllerSetup {

      val page = controller.submitReOptInDeclinedSurveyForm(TestFixtures.reOptInHostContext("foo@bar.com"))(
        FakeRequest().withFormUrlEncodedBody(
          "choice-0305d33f-2e8d-4cb2-82d2-52132fc325fe" -> "true",
          "choice-ce34aa17-df2a-44fb-9d5c-4d930396483a" -> "true",
          "choice-d0edb491-6dcb-48a8-aeca-b16f01c541a5" -> "true",
          "choice-1e825e7d-6fc8-453f-8c20-1a7ed4d84ea5" -> "false",
          "choice-15d28c3f-9f33-4c44-aefa-165fc84b5e23" -> "true",
          "reason"                                      -> "test test test"
        ))

      status(page) mustBe 303

      val eventArg: ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      verify(mockAuditConnector).sendExtendedEvent(eventArg.capture())(any(), any())

      private val value: ExtendedDataEvent = eventArg.getValue
      value.auditSource mustBe "preferences-frontend"
      value.auditType mustBe EventTypes.Succeeded
      value.tags must contain("transactionName" -> "Re-OptIn Declined Survey Answered")
      val detail = Json.fromJson[EventDetail](value.detail).get
      detail.utr mustBe validUtr.value
      detail.nino mustBe "N/A"
      detail.choices.get("choice-0305d33f-2e8d-4cb2-82d2-52132fc325fe").get.answer mustBe "true"
      detail.choices.get("choice-ce34aa17-df2a-44fb-9d5c-4d930396483a").get.answer mustBe "true"
      detail.choices.get("choice-d0edb491-6dcb-48a8-aeca-b16f01c541a5").get.answer mustBe "true"
      detail.choices.get("choice-1e825e7d-6fc8-453f-8c20-1a7ed4d84ea5").get.answer mustBe "false"
      detail.choices.get("choice-15d28c3f-9f33-4c44-aefa-165fc84b5e23").get.answer mustBe "true"
      detail.reason mustBe "test test test"
    }

    "not be created when a user submits an invalid survey form with more than 500 characters in the reason field" in new SurveyControllerSetup {

      val page = controller.submitReOptInDeclinedSurveyForm(TestFixtures.reOptInHostContext("foo@bar.com"))(
        FakeRequest().withFormUrlEncodedBody(
          "choice-0305d33f-2e8d-4cb2-82d2-52132fc325fe" -> "true",
          "choice-ce34aa17-df2a-44fb-9d5c-4d930396483a" -> "true",
          "choice-d0edb491-6dcb-48a8-aeca-b16f01c541a5" -> "true",
          "choice-1e825e7d-6fc8-453f-8c20-1a7ed4d84ea5" -> "false",
          "choice-15d28c3f-9f33-4c44-aefa-165fc84b5e23" -> "true",
          "reason"                                      -> "A" * 501
        ))

      status(page) mustBe 400

      val document = Jsoup.parse(contentAsString(page))
      document
        .getElementsByClass("error-notification")
        .get(0)
        .childNode(0)
        .toString
        .trim mustBe "Maximum length is 500 characters"

      verifyZeroInteractions(mockAuditConnector)
    }
  }
}
