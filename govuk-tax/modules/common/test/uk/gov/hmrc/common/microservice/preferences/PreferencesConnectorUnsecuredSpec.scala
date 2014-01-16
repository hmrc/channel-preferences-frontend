package uk.gov.hmrc.common.microservice.preferences

import org.scalatest.mock.MockitoSugar
import org.scalatest.{OptionValues, BeforeAndAfterEach, ShouldMatchers, WordSpec}
import play.api.test.WithApplication
import org.mockito.Mockito._
import org.mockito.{Matchers, ArgumentCaptor}
import Matchers.any
import play.api.libs.ws.Response
import play.api.test.FakeApplication
import scala.Some
import scala.concurrent.Future
import org.scalatest.concurrent.ScalaFutures
import controllers.common.actions.HeaderCarrier
import uk.gov.hmrc.common.microservice.preferences.{EmailVerificationLinkResponse, SaPreferenceSimplified, PreferencesConnector, ValidateEmail}

class TestPreferencesConnectorUnsecured extends PreferencesConnector with MockitoSugar {

  val httpWrapper = mock[HttpWrapper]


  override protected def httpPostF[TResult, TBody](uri: String, body: Option[TBody], headers: Map[String, String])(implicit bodyManifest: Manifest[TBody], resultManifest: Manifest[TResult], headerCarrier: HeaderCarrier): Future[Option[TResult]] =
    httpWrapper.httpPostF(uri, body, headers)(bodyManifest, resultManifest, headerCarrier)

  override protected def httpPost[A, B](uri: String, body: A, headers: Map[String, String])(responseProcessor: (Response) => B)(implicit a: Manifest[A], b: Manifest[B], headerCarrier: HeaderCarrier): Future[B] =
    Future.successful(responseProcessor(httpWrapper.httpPost(uri, body, headers)))

  override protected def httpGetF[A](uri: String)(implicit m: Manifest[A], headerCarrier: HeaderCarrier): Future[Option[A]] =
    httpWrapper.httpGetF(uri)(m, headerCarrier)

  abstract class HttpWrapper {
    def httpGetF[A](uri: String)(implicit m: Manifest[A], headerCarrier: HeaderCarrier): Future[Option[A]]

    def httpPostF[TResult, TBody](uri: String, body: Option[TBody], headers: Map[String, String])(implicit bodyManifest: Manifest[TBody], resultManifest: Manifest[TResult], headerCarrier: HeaderCarrier): Future[Option[TResult]]

    def httpPost[A, B](uri: String, body: A, headers: Map[String, String]): Response
  }

}

class PreferencesConnectorUnsecuredSpec extends WordSpec with MockitoSugar with ShouldMatchers with BeforeAndAfterEach with ScalaFutures with OptionValues {

  lazy val preferenceConnector = new TestPreferencesConnectorUnsecured

  override def afterEach = reset(preferenceConnector.httpWrapper)

  val utr = "2134567"

  val email = "someEmail@email.com"

  implicit val hc = HeaderCarrier()

  "SaMicroService" should {
    "save preferences for a user that wants email notifications" in new WithApplication(FakeApplication()) {

      preferenceConnector.savePreferencesUnsecured(utr, true, Some(email))

      val bodyCaptor = ArgumentCaptor.forClass(classOf[Option[SaPreferenceSimplified]])
      verify(preferenceConnector.httpWrapper).httpPostF(Matchers.eq(s"/portal/preferences/sa/individual/$utr/print-suppression"), bodyCaptor.capture(), Matchers.any[Map[String, String]])(any(), any(), Matchers.eq(hc))

      val body = bodyCaptor.getValue.value
      body.digital shouldBe true
      body.email.value shouldBe email
    }

    "save preferences for a user that wants paper notifications" in new WithApplication(FakeApplication()) {

      preferenceConnector.savePreferencesUnsecured(utr, false)

      val bodyCaptor = ArgumentCaptor.forClass(classOf[Option[SaPreferenceSimplified]])
      verify(preferenceConnector.httpWrapper).httpPostF(Matchers.eq(s"/portal/preferences/sa/individual/$utr/print-suppression"), bodyCaptor.capture(), Matchers.any[Map[String, String]])(any(), any(), Matchers.eq(hc))

      val body = bodyCaptor.getValue.value
      body.digital shouldBe false
      body.email should not be 'defined
    }

    "get preferences for a user who opted for email notification" in new WithApplication(FakeApplication()) {

      when(preferenceConnector.httpWrapper.httpGetF[SaPreferenceSimplified](s"/portal/preferences/sa/individual/$utr/print-suppression")).thenReturn(Future.successful(Some(SaPreferenceSimplified(true, Some("someEmail@email.com")))))
      val result = preferenceConnector.getPreferencesUnsecured(utr).futureValue.get
      verify(preferenceConnector.httpWrapper).httpGetF[SaPreferenceSimplified](s"/portal/preferences/sa/individual/$utr/print-suppression")

      result.digital should be (true)
      result.email should be (Some("someEmail@email.com"))
    }

    "get preferences for a user who opted for paper notification" in new WithApplication(FakeApplication()) {

      when(preferenceConnector.httpWrapper.httpGetF[SaPreferenceSimplified](s"/portal/preferences/sa/individual/$utr/print-suppression")).thenReturn(Future.successful(Some(SaPreferenceSimplified(false))))
      val result = preferenceConnector.getPreferencesUnsecured(utr).futureValue.get
      verify(preferenceConnector.httpWrapper).httpGetF[SaPreferenceSimplified](s"/portal/preferences/sa/individual/$utr/print-suppression")

      result.digital should be(false)
      result.email should be(None)
    }

    "return none for a user who has not set preferences" in new WithApplication(FakeApplication()) {
      val mockPlayResponse = mock[Response]
      when(mockPlayResponse.status).thenReturn(404)
      when(preferenceConnector.httpWrapper.httpGetF[SaPreferenceSimplified](s"/portal/preferences/sa/individual/$utr/print-suppression")).thenReturn(Future.successful(None))
      preferenceConnector.getPreferencesUnsecured(utr).futureValue shouldBe None
      verify(preferenceConnector.httpWrapper).httpGetF[SaPreferenceSimplified](s"/portal/preferences/sa/individual/$utr/print-suppression")
    }

  }

  "The updateEmailValidationStatusUnsecured" should {
    import EmailVerificationLinkResponse._

    "return ok if updateEmailValidationStatusUnsecured returns 200" in {
      val token = "someGoodToken"
      val expected = ValidateEmail(token)
      val response = mock[Response]

      when(response.status).thenReturn(200)
      when(preferenceConnector.httpWrapper.httpPost(Matchers.eq("/preferences/sa/verify-email"),
        Matchers.eq(expected),
        Matchers.any[Map[String, String]])).thenReturn(response)

      val result = preferenceConnector.updateEmailValidationStatusUnsecured(token)

      result.futureValue shouldBe OK
    }

    "return ok if updateEmailValidationStatusUnsecured returns 204" in {
      val token = "someGoodToken"
      val expected = ValidateEmail(token)
      val response = mock[Response]

      when(response.status).thenReturn(204)
      when(preferenceConnector.httpWrapper.httpPost(Matchers.eq("/preferences/sa/verify-email"),
        Matchers.eq(expected),
        Matchers.any[Map[String, String]])).thenReturn(response)

      val result = preferenceConnector.updateEmailValidationStatusUnsecured(token)

      result.futureValue shouldBe OK
    }

    "return error if updateEmailValidationStatusUnsecured returns 400" in {
      val token = "someGoodToken"
      val expected = ValidateEmail(token)
      val response = mock[Response]

      when(response.status).thenReturn(400)
      when(preferenceConnector.httpWrapper.httpPost(Matchers.eq("/preferences/sa/verify-email"),
        Matchers.eq(expected),
        Matchers.any[Map[String, String]])).thenReturn(response)

      val result = preferenceConnector.updateEmailValidationStatusUnsecured(token)

      result.futureValue shouldBe ERROR
    }

    "return error if updateEmailValidationStatusUnsecured returns 404" in {
      val token = "someGoodToken"
      val expected = ValidateEmail(token)
      val response = mock[Response]

      when(response.status).thenReturn(404)
      when(preferenceConnector.httpWrapper.httpPost(Matchers.eq("/preferences/sa/verify-email"),
        Matchers.eq(expected),
        Matchers.any[Map[String, String]])).thenReturn(response)

      val result = preferenceConnector.updateEmailValidationStatusUnsecured(token)

      result.futureValue shouldBe ERROR
    }

    "return error if updateEmailValidationStatusUnsecured returns 500" in {
      val token = "someGoodToken"
      val expected = ValidateEmail(token)
      val response = mock[Response]

      when(response.status).thenReturn(500)
      when(preferenceConnector.httpWrapper.httpPost(Matchers.eq("/preferences/sa/verify-email"),
        Matchers.eq(expected),
        Matchers.any[Map[String, String]])).thenReturn(response)

      val result = preferenceConnector.updateEmailValidationStatusUnsecured(token)

      result.futureValue shouldBe ERROR
    }

    "return expired if updateEmailValidationStatusUnsecured returns 410" in {
      val token = "someGoodToken"
      val expected = ValidateEmail(token)
      val response = mock[Response]

      when(response.status).thenReturn(410)
      when(preferenceConnector.httpWrapper.httpPost(Matchers.eq("/preferences/sa/verify-email"),
        Matchers.eq(expected),
        Matchers.any[Map[String, String]])).thenReturn(response)

      val result = preferenceConnector.updateEmailValidationStatusUnsecured(token)

      result.futureValue shouldBe EXPIRED
    }
  }
}
