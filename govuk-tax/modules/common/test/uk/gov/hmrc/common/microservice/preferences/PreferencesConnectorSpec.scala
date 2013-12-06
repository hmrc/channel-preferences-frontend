package uk.gov.hmrc.common.microservice.preferences

import org.scalatest.mock.MockitoSugar
import org.scalatest.TestData
import play.api.libs.json.JsValue
import play.api.test.WithApplication
import org.mockito.Mockito._
import org.mockito.{Matchers, ArgumentCaptor}
import play.api.test.FakeApplication
import play.api.libs.json.JsBoolean
import uk.gov.hmrc.domain.SaUtr
import controllers.common.actions.HeaderCarrier
import uk.gov.hmrc.common.BaseSpec
import scala.concurrent.Future
import org.scalatest.concurrent.ScalaFutures

class TestPreferencesConnector extends PreferencesConnector with MockitoSugar {

  val httpWrapper = mock[HttpWrapper]

  override protected def httpGetF[A](uri: String)(implicit m: Manifest[A], hc: HeaderCarrier): Future[Option[A]] = {
    httpWrapper.getF(uri)
  }

  override protected def httpPost[A](uri: String, body: JsValue, headers: Map[String, String])(implicit m: Manifest[A], headerCarrier: HeaderCarrier): Option[A] = {
    httpWrapper.post(uri, body, headers)
  }

  class HttpWrapper {

    def getF[T](uri: String): Future[Option[T]] = Future.successful(None)

    def post[T](uri: String, body: JsValue, headers: Map[String, String]): Option[T] = None

  }

}

class PreferencesConnectorSpec extends BaseSpec with ScalaFutures {

  lazy val preferenceMicroService: TestPreferencesConnector = new TestPreferencesConnector

  override def afterEach(testData: TestData) = reset(preferenceMicroService.httpWrapper)

  val utr = SaUtr("2134567")
  val email = "someEmail@email.com"
  val preferencesUri: String = s"/preferences/sa/individual/$utr/print-suppression"

  "SaMicroService" should {
    "save preferences for a user that wants email notifications" in new WithApplication(FakeApplication()) {

      preferenceMicroService.savePreferences(utr, digital = true, Some(email))

      val bodyCaptor: ArgumentCaptor[JsValue] = ArgumentCaptor.forClass(manifest.runtimeClass.asInstanceOf[Class[JsValue]])
      verify(preferenceMicroService.httpWrapper).post(Matchers.eq(preferencesUri), bodyCaptor.capture(), Matchers.any[Map[String, String]])

      val body = bodyCaptor.getValue
      (body \ "digital").as[JsBoolean].value shouldBe true
      (body \ "email").as[String] shouldBe email
    }

    "save preferences for a user that wants paper notifications" in new WithApplication(FakeApplication()) {

      preferenceMicroService.savePreferences(utr, digital = false)

      val bodyCaptor: ArgumentCaptor[JsValue] = ArgumentCaptor.forClass(manifest.runtimeClass.asInstanceOf[Class[JsValue]])
      verify(preferenceMicroService.httpWrapper).post(Matchers.eq(preferencesUri), bodyCaptor.capture(), Matchers.any[Map[String, String]])

      val body = bodyCaptor.getValue
      (body \ "digital").as[JsBoolean].value shouldBe false
      (body \ "email").asOpt[String] shouldBe None

    }

    "get preferences for a user who opted for email notification" in new WithApplication(FakeApplication()) {

      when(preferenceMicroService.httpWrapper.getF[SaPreference](preferencesUri)).thenReturn(Future.successful(Some(SaPreference(digital = true, Some("someEmail@email.com")))))
      val result = preferenceMicroService.getPreferences(utr)(HeaderCarrier())
      verify(preferenceMicroService.httpWrapper).getF[SaPreference](preferencesUri)

      whenReady(result) {
        _ shouldBe Some(SaPreference(digital = true, Some("someEmail@email.com")))
      }
    }

    "get preferences for a user who opted for paper notification" in new WithApplication(FakeApplication()) {

      when(preferenceMicroService.httpWrapper.getF[SaPreference](preferencesUri)).thenReturn(Future.successful(Some(SaPreference(digital = false))))
      val result = preferenceMicroService.getPreferences(utr)(HeaderCarrier())
      verify(preferenceMicroService.httpWrapper).getF[SaPreference](preferencesUri)

      whenReady(result) {
        _ shouldBe Some(SaPreference(digital = false, None))
      }
    }

    "return None for a user who has not set preferences" in new WithApplication(FakeApplication()) {
      when(preferenceMicroService.httpWrapper.getF[SaPreference](preferencesUri)).thenReturn(Future.successful(None))
      val result = preferenceMicroService.getPreferences(utr)(HeaderCarrier())
      verify(preferenceMicroService.httpWrapper).getF[SaPreference](preferencesUri)

      whenReady(result) {
        _ shouldBe None
      }

    }

  }
}
