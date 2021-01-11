/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers

import connectors.{ EmailPreference, EntityResolverConnector, PreferenceResponse, TermsAndConditonsAcceptance }
import helpers.Resources
import model.StatusName.{ Alright, BouncedEmail, EmailNotVerified, NewCustomer, NoEmail, Paper }
import model.{ HostContext, StatusName }
import org.joda.time.{ DateTime, DateTimeZone }
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{ Assertion, WordSpec }
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsValue
import play.api.mvc.{ AnyContentAsEmpty, Cookie, Result }
import play.api.test.FakeRequest
import play.api.test.Helpers.{ status, _ }
import service.PaperlessStatusService
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.{ AffinityGroup, AuthConnector, ConfidenceLevel }
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class PaperlessStatusControllerSpec extends WordSpec with MockitoSugar with GuiceOneAppPerSuite {

  "getPaperlessStatus" should {

    "return a 200 with an already opted in json" in new TestContext(statusName = Alright, preference = preferences()) {
      val alrightResponse: JsValue = Resources.readJson("PaperlessStatusAlright.json")
      val welshAlrightResponse: JsValue = Resources.readJson("PaperlessStatusAlrightWelsh.json")
      verify(request = request, response = alrightResponse)
      verify(request = welshRequest, response = welshAlrightResponse)
    }

    "return a 200 with a bounced email json" in new TestContext(
      statusName = BouncedEmail,
      preference = preferences(hasBounces = true)) {
      val bouncedResponse: JsValue = Resources.readJson("PaperlessStatusBounced.json")
      val welshBouncedResponse: JsValue = Resources.readJson("PaperlessStatusBouncedWelsh.json")
      verify(request = request, response = bouncedResponse)
      verify(request = welshRequest, response = welshBouncedResponse)

    }

    "return a 200 with a email not verified json" in new TestContext(
      statusName = EmailNotVerified,
      preference = preferences(isVerified = false)) {
      val notYetVerified: JsValue = Resources.readJson("PaperlessStatusNotVerified.json")
      val welshNotYetVerified: JsValue = Resources.readJson("PaperlessStatusNotVerifiedWelsh.json")
      verify(request = request, response = notYetVerified)
      verify(request = welshRequest, response = welshNotYetVerified)
    }

    "return a 200 with a paper json" in new TestContext(
      statusName = Paper,
      preference = preferences(termsAcceptance = false)) {
      val paper: JsValue = Resources.readJson("PaperlessStatusPaper.json")
      val welshPaper: JsValue = Resources.readJson("PaperlessStatusPaperWelsh.json")
      verify(request = request, response = paper)
      verify(request = welshRequest, response = welshPaper)
    }

    "return a 200 with a new customer json when no preferences are found" in new TestContext(
      statusName = NewCustomer,
      preference = None) {
      val newCustomer: JsValue = Resources.readJson("PaperlessStatusNewCustomer.json")
      val welshNewCustomer: JsValue = Resources.readJson("PaperlessStatusNewCustomerWelsh.json")
      verify(request = request, response = newCustomer)
      verify(request = welshRequest, response = welshNewCustomer)
    }

    "return a 200 with a no email json when a preference record is found but no has no email" in new TestContext(
      statusName = NoEmail,
      preference = preferences(containsEmail = false)) {
      val noEmail: JsValue = Resources.readJson("PaperlessStatusNoEmail.json")
      val welshNoEmail: JsValue = Resources.readJson("PaperlessStatusNoEmailWelsh.json")
      verify(request = request, response = noEmail)
      verify(request = welshRequest, response = welshNoEmail)
    }
  }

  private def preferences(
    isVerified: Boolean = true,
    hasBounces: Boolean = false,
    termsAcceptance: Boolean = true,
    containsEmail: Boolean = true): Option[PreferenceResponse] =
    Some(
      PreferenceResponse(
        Map("generic" -> TermsAndConditonsAcceptance(termsAcceptance)),
        if (!containsEmail)
          None
        else
          Some(
            EmailPreference(
              "pihklyljtgoxeoh@mail.com",
              isVerified = isVerified,
              hasBounces = hasBounces,
              mailboxFull = false,
              linkSent = None))
      ))

  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val welshRequest: FakeRequest[AnyContentAsEmpty.type] = request.withCookies(Cookie("PLAY_LANG", "CY"))
  private implicit val hostContext: HostContext = HostContext(returnUrl = "", returnLinkText = "")
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private lazy val mockAuthConnector: AuthConnector = mock[AuthConnector]
  private lazy val mockEntityResolverConnector: EntityResolverConnector = mock[EntityResolverConnector]
  private lazy val mockPaperlessStatusService: PaperlessStatusService = mock[PaperlessStatusService]
  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[AuthConnector].toInstance(mockAuthConnector),
        bind[EntityResolverConnector].toInstance(mockEntityResolverConnector),
        bind[PaperlessStatusService].toInstance(mockPaperlessStatusService)
      )
      .build()

  class TestContext(statusName: StatusName, preference: Option[PreferenceResponse]) extends PlaySpec {

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
        ))

    private val controller = app.injector.instanceOf[PaperlessStatusController]

    def submitRequest(request: FakeRequest[AnyContentAsEmpty.type]): Future[Result] =
      controller.getPaperlessStatus(hostContext)(request)

    def verify(request: FakeRequest[AnyContentAsEmpty.type], response: JsValue): Assertion = {
      val result: Future[Result] = submitRequest(request)
      status(result) mustBe 200
      contentAsJson(result) mustBe response
    }

    when(mockAuthConnector.authorise[AuthRetrievals](any(), any())(any(), any()))
      .thenReturn(retrievalResult)
    when(mockEntityResolverConnector.getPreferences()(any()))
      .thenReturn(Future.successful(preference))
    when(mockPaperlessStatusService.determineStatus(preference))
      .thenReturn(statusName)
  }

}
