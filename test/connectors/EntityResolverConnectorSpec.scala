/*
 * Copyright 2020 HM Revenue & Customs
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

package connectors

import connectors.PreferenceResponse._
import model.HostContext
import model.Language.Welsh
import org.mockito.Matchers
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{ Eventually, ScalaFutures }
import org.scalatest.time._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.circuitbreaker.UnhealthyServiceException
import uk.gov.hmrc.domain.{ Nino, SaUtr }
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.Future

class EntityResolverConnectorSpec
    extends PlaySpec with ScalaFutures with GuiceOneAppPerSuite with BeforeAndAfterEach with MockitoSugar
    with Eventually with SpanSugar with play.api.http.Status {
  val mockHttpClient = mock[HttpClient]

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "govuk-tax.Test.services.contact-frontend.host"                                                -> "localhost",
        "govuk-tax.Test.services.contact-frontend.port"                                                -> "9250",
        "govuk-tax.Test.assets.url"                                                                    -> "fake/url",
        "govuk-tax.Test.assets.version"                                                                -> "54321",
        "application.langs"                                                                            -> "en,cy",
        "govuk-tax.Test.google-analytics.host"                                                         -> "host",
        "govuk-tax.Test.google-analytics.token"                                                        -> "aToken",
        "Test.microservice.services.entity-resolver.circuitBreaker.numberOfCallsToTriggerStateChange"  -> "5",
        "Test.microservice.services.entity-resolver.circuitBreaker.unavailablePeriodDurationInSeconds" -> "1"
      )
      .overrides(bind[HttpClient].toInstance(mockHttpClient))
      .build()

  val connector = app.injector.instanceOf[EntityResolverConnector]
  val http = app.injector.instanceOf[HttpClient]

  implicit val hc = new HeaderCarrier
  implicit val hostContext: HostContext = new HostContext(returnUrl = "", returnLinkText = "")

  override protected def beforeEach(): Unit = {
    when(http.GET[Option[PreferenceResponse]](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(None))
    eventually(timeout(2 seconds), interval(100 milliseconds))(
      connector.getPreferencesStatus().futureValue mustBe Right(PreferenceNotFound(None)))
  }
  "getPreferencesStatusByToken" should {
    val GOOD_SERVICE = "mtdfbit"
    val BAD_SERVICE = "rubbish"
    val GOOD_TOKEN = "91abdbb1-6ad4-4419-8f33-a7ea6cf8e388"
    val BAD_TOKEN = "rubbish"

    "map no preference to PreferenceNotFound with no email" in {

      when(http.GET[Option[PreferenceResponse]](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(None))

      connector.getPreferencesStatusByToken(GOOD_SERVICE, GOOD_TOKEN).futureValue mustBe Right(PreferenceNotFound(None))
    }
  }

  "getPreferencesStatus" should {

    "map no preference to PreferenceNotFound with no email" in {

      when(http.GET[Option[PreferenceResponse]](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(None))

      connector.getPreferencesStatus().futureValue mustBe Right(PreferenceNotFound(None))
    }

    "map found paperless preference to true" in {
      val json = Json.parse("""
                              |{
                              |   "digital": true,
                              |   "email": {
                              |     "email": "test@mail.com",
                              |     "status": "verified",
                              |     "mailboxFull": false
                              |   }
                              |}
          """.stripMargin)
      when(http.GET[Option[PreferenceResponse]](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(Json.fromJson[PreferenceResponse](json).get)))

      val expectedResult = new SaPreference(
        digital = true,
        email = Some(new SaEmailPreference("test@mail.com", SaEmailPreference.Status.Verified)))

      val preferenceResponse = connector.getPreferencesStatus().futureValue
      preferenceResponse mustBe Right(PreferenceFound(true, expectedResult.toNewPreference().email))
    }

    "map found non-paperless preference to false" in {
      val json = Json.parse("""
                              |{
                              |   "digital": false
                              |}
          """.stripMargin)

      when(http.GET[Option[PreferenceResponse]](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(Json.fromJson[PreferenceResponse](json).get)))
      val expectedResult = new SaPreference(digital = false, email = None)

      connector.getPreferencesStatus().futureValue mustBe Right(
        PreferenceFound(expectedResult.toNewPreference().termsAndConditions.get("generic").get.accepted, None))
    }

    "map an auth failure to UNAUTHORIZED" in {
      when(http.GET[Option[PreferenceResponse]](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.failed(Upstream4xxResponse("", UNAUTHORIZED, UNAUTHORIZED, Map())))

      connector.getPreferencesStatus().futureValue mustBe Left(UNAUTHORIZED)
    }

    "circuit breaker configuration should be applied and unhealthy service exception will kick in after 5th failed call to preferences" in {
      when(http.GET[Option[PreferenceResponse]](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.failed(Upstream5xxResponse("", 500, 500)))

      1 to 5 foreach { _ =>
        connector.getPreferencesStatus().failed.futureValue mustBe an[Upstream5xxResponse]
      }
      connector.getPreferencesStatus().failed.futureValue mustBe an[UnhealthyServiceException]
    }
  }

  "The getPreferences method" should {
    val nino = Nino("CE123457D")

    "return the preferences for utr only" in {
      val json = Json.parse("""
                              |{
                              |   "digital": true,
                              |   "email": {
                              |     "email": "test@mail.com",
                              |     "status": "verified",
                              |     "mailboxFull": false
                              |   }
                              |}
           """.stripMargin)
      when(http.GET[Option[PreferenceResponse]](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(Json.fromJson[PreferenceResponse](json).get)))

      connector.getPreferences().futureValue mustBe Some(
        SaPreference(
          digital = true,
          email = Some(SaEmailPreference(email = "test@mail.com", status = SaEmailPreference.Status.Verified))
        ).toNewPreference())
    }

    "return None for a 404" in {

      when(http.GET[Option[PreferenceResponse]](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.failed(new NotFoundException("")))

      connector.getPreferences().futureValue mustBe None
    }

    "return None for a 410" in {
      when(http.GET[Option[PreferenceResponse]](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.failed(Upstream4xxResponse("", GONE, GONE, Map())))

      connector.getPreferences().futureValue mustBe None
    }

    "circuit breaker configuration should be applied and unhealthy service exception will kick in after 5th failed call to preferences" in {
      when(http.GET[Option[PreferenceResponse]](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.failed(new InternalServerException("some exception")))

      1 to 5 foreach { _ =>
        connector.getPreferences().failed.futureValue mustBe an[InternalServerException]
      }
      connector.getPreferences().failed.futureValue mustBe an[UnhealthyServiceException]
    }
  }

  "The getEmailAddress method" should {
    "return None for a 404" in {

      when(http.GET[Option[Email]](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(None))
      connector.getEmailAddress(SaUtr("1")).futureValue must be(None)
    }

    "return Error for other status code" in {
      when(http.GET[Option[Email]](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.failed(new BadRequestException("")))
      connector.getEmailAddress(SaUtr("1")).failed.futureValue must be(an[Exception])
    }

    "return an email address when there is an email preference" in {

      val json = Json.parse("""{
                              |  "email" : "a@b.com"
                              |}
         """.stripMargin)
      when(http.GET[Option[Email]](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(Json.fromJson[Email](json).get)))

      connector.getEmailAddress(SaUtr("1")).futureValue must be(Some("a@b.com"))
    }
  }

  "The responseToEmailVerificationLinkStatus method" should {

    "return ok if updateEmailValidationStatusUnsecured returns 200" in {
      val result = connector.responseToEmailVerificationLinkStatus(Future.successful(HttpResponse(200)))
      result.futureValue mustBe Validated
    }

    "return ok if updateEmailValidationStatusUnsecured returns 204" in {
      val result = connector.responseToEmailVerificationLinkStatus(Future.successful(HttpResponse(204)))
      result.futureValue mustBe Validated
    }

    "return ok with the return link text and return url if updateEmailValidationStatusUnsecured returns 201" in {
      val responseJson = Json.parse("""{
                                      |     "returnLinkText": "Return Link Text",
                                      |     "returnUrl": "ReturnUrl"
                                      |}""".stripMargin)
      val result = connector.responseToEmailVerificationLinkStatus(
        Future.successful(HttpResponse(201, responseJson = Some(responseJson))))
      result.futureValue mustBe ValidatedWithReturn("Return Link Text", "ReturnUrl")
    }

    "return error if updateEmailValidationStatusUnsecured returns 400" in {
      val result = connector.responseToEmailVerificationLinkStatus(Future.failed(new BadRequestException("")))
      result.futureValue mustBe ValidationError
    }

    "return 'error with return' with error if updateEmailValidationStatusUnsecured returns 412" in {
      val result = connector.responseToEmailVerificationLinkStatus(
        Future.failed(
          Upstream4xxResponse(
            """PUT of something Response body: '{"returnLinkText":"a message", "returnUrl": "https://some/place"}'""",
            PRECONDITION_FAILED,
            0,
            Map())))
      result.futureValue mustBe ValidationErrorWithReturn("a message", "https://some/place")
    }

    "return error if updateEmailValidationStatusUnsecured returns 404" in {
      val result = connector.responseToEmailVerificationLinkStatus(Future.failed(new NotFoundException("")))
      result.futureValue mustBe ValidationError
    }

    "pass through the failure if updateEmailValidationStatusUnsecured returns 500" in {
      val expectedErrorResponse = Upstream5xxResponse("", 500, 500)
      val result = connector.responseToEmailVerificationLinkStatus(Future.failed(expectedErrorResponse))

      result.failed.futureValue mustBe expectedErrorResponse
    }

    "return expired if updateEmailValidationStatusUnsecured returns 410" in {
      val result =
        connector.responseToEmailVerificationLinkStatus(Future.failed(Upstream4xxResponse("", 410, 500)))
      result.futureValue mustBe ValidationExpired
    }

    "return wrong token if updateEmailValidationStatusUnsecured returns 409" in {
      val result =
        connector.responseToEmailVerificationLinkStatus(Future.failed(Upstream4xxResponse("", 409, 500)))
      result.futureValue mustBe WrongToken
    }
  }

  "The updateTermsAndConditions method" should {
    trait PayloadCheck {
      def postReturns(status: Int) =
        when(
          http.POST[TermsAndConditionsUpdate, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(
            Matchers.any(),
            Matchers.any(),
            Matchers.any(),
            Matchers.any())).thenReturn(Future.successful(HttpResponse(status)))
      def checkPayload(url: String, payload: TermsAndConditionsUpdate) =
        verify(http).POST(Matchers.endsWith(s"$url"), Matchers.eq(payload), Matchers.any())(
          Matchers.any(),
          Matchers.any(),
          Matchers.any(),
          Matchers.any())
    }

    "send generic accepted true and return preferences created if terms and conditions are accepted and updated and preferences created" in new PayloadCheck {
      postReturns(OK)
      val payload =
        TermsAndConditionsUpdate
          .from(GenericTerms -> TermsAccepted(true), email = None, includeLinkDetails = false, language = Welsh)
      connector.updateTermsAndConditions(payload).futureValue must be(PreferencesExists)
      checkPayload("/preferences/terms-and-conditions", payload)
    }

    "send generic accepted false and return preferences created if terms and conditions are not accepted and updated and preferences created" in new PayloadCheck {
      postReturns(OK)
      val payload =
        TermsAndConditionsUpdate
          .from(GenericTerms -> TermsAccepted(false), email = None, includeLinkDetails = false, language = Welsh)
      connector.updateTermsAndConditions(payload).futureValue must be(PreferencesExists)
      checkPayload("/preferences/terms-and-conditions", payload)
    }

    "send taxCredits accepted true and return preferences created if terms and conditions are accepted and updated and preferences created" in new PayloadCheck {
      postReturns(OK)
      val payload =
        TermsAndConditionsUpdate
          .from(TaxCreditsTerms -> TermsAccepted(true), email = None, includeLinkDetails = false, language = Welsh)
      connector.updateTermsAndConditions(payload).futureValue must be(PreferencesExists)
      checkPayload("/preferences/terms-and-conditions", payload)
    }

    "send taxCredits accepted false and return preferences created if terms and conditions are not accepted and updated and preferences created" in new PayloadCheck {
      postReturns(OK)
      val payload =
        TermsAndConditionsUpdate
          .from(TaxCreditsTerms -> TermsAccepted(false), email = None, includeLinkDetails = false, language = Welsh)
      connector.updateTermsAndConditions(payload).futureValue must be(PreferencesExists)
      checkPayload("/preferences/terms-and-conditions", payload)
    }

    "include the returnUrl and returnLinkText in the post when called by a service and token" in new PayloadCheck {
      postReturns(OK)
      val payload = TermsAndConditionsUpdate
        .from(GenericTerms -> TermsAccepted(true), email = None, includeLinkDetails = true, language = Welsh)

      connector
        .updateTermsAndConditionsForSvc(payload, svc = Some("MTDFBIT"), token = Some("A TOKEN"))
        .futureValue must be(PreferencesExists)
      checkPayload("/preferences/terms-and-conditions/MTDFBIT/A TOKEN", payload)
    }

    "return failure if any problems" in new PayloadCheck {
      when(
        http.POST[TermsAndConditionsUpdate, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(
          Matchers.any(),
          Matchers.any(),
          Matchers.any(),
          Matchers.any())).thenReturn(Future.failed(Upstream4xxResponse("", 401, 401)))

      val payload = TermsAndConditionsUpdate
        .from(GenericTerms -> TermsAccepted(true), email = None, includeLinkDetails = true, language = Welsh)
      connector
        .updateTermsAndConditions(payload)
        .failed
        .futureValue mustBe an[Upstream4xxResponse]
    }
  }

  "New user" should {
    trait NewUserPayloadCheck {

      def postReturns(status: Int): OngoingStubbing[Future[HttpResponse]] =
        when(
          http.POST[TermsAndConditionsUpdate, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(
            Matchers.any(),
            Matchers.any(),
            Matchers.any(),
            Matchers.any())).thenReturn(Future.successful(HttpResponse(status)))
      def checkPayload(url: String, payload: TermsAndConditionsUpdate): Future[HttpResponse] =
        verify(http, times(1)).POST[TermsAndConditionsUpdate, HttpResponse](
          Matchers.endsWith(s"$url"),
          Matchers.eq(payload),
          Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())
      val email = "test@test.com"

    }

    "send generic accepted true with email" in new NewUserPayloadCheck {
      postReturns(201)
      val termsAndConditionsUpdate =
        TermsAndConditionsUpdate.from(
          GenericTerms -> TermsAccepted(true),
          email = Some("test@test.com"),
          includeLinkDetails = true,
          language = Welsh)

      connector.updateTermsAndConditions(termsAndConditionsUpdate).futureValue must be(PreferencesCreated)

      checkPayload("/preferences/terms-and-conditions", termsAndConditionsUpdate)
    }

    "send generic accepted false with no email" in new NewUserPayloadCheck {

      postReturns(201)
      val expectedPayload =
        TermsAndConditionsUpdate
          .from(GenericTerms -> TermsAccepted(true), email = None, includeLinkDetails = true, language = Welsh)

      connector.updateTermsAndConditions(expectedPayload).futureValue must be(PreferencesCreated)
    }

    "send taxCredits accepted true with email" in new NewUserPayloadCheck {
      postReturns(201)
      val expectedPayload =
        TermsAndConditionsUpdate.from(
          TaxCreditsTerms -> TermsAccepted(true),
          email = Some("test@test.com"),
          includeLinkDetails = true,
          language = Welsh)

      connector.updateTermsAndConditions(expectedPayload).futureValue must be(PreferencesCreated)
    }

    "send taxCredits accepted false with no email" in new NewUserPayloadCheck {
      postReturns(201)
      val expectedPayload =
        TermsAndConditionsUpdate
          .from(TaxCreditsTerms -> TermsAccepted(false), email = None, includeLinkDetails = true, language = Welsh)

      connector.updateTermsAndConditions(expectedPayload).futureValue must be(PreferencesCreated)
    }

    "try and send accepted true with email where preferences not working" in new NewUserPayloadCheck {
      val expectedPayload =
        TermsAndConditionsUpdate
          .from(GenericTerms -> TermsAccepted(true), email = None, includeLinkDetails = true, language = Welsh)

      when(
        http.POST[TermsAndConditionsUpdate, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(
          Matchers.any(),
          Matchers.any(),
          Matchers.any(),
          Matchers.any())).thenReturn(Future.failed(Upstream4xxResponse("", 401, 401)))

      connector
        .updateTermsAndConditions(expectedPayload)
        .failed
        .futureValue mustBe an[Upstream4xxResponse]

    }
  }

}
