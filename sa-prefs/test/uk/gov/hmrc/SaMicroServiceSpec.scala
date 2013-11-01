package uk.gov.hmrc

import org.scalatest.mock.MockitoSugar
import org.scalatest.{ BeforeAndAfterEach, ShouldMatchers, WordSpec }
import play.api.libs.json.{Json, JsValue}
import play.api.test.WithApplication
import org.mockito.Mockito._
import org.mockito.{Matchers, ArgumentCaptor}
import uk.gov.hmrc.Transform._
import play.api.libs.ws.Response
import play.api.test.FakeApplication
import play.api.libs.json.JsBoolean
import scala.Some

class TestPreferencesMicroservice extends PreferencesMicroService with MockitoSugar {

  val httpWrapper = mock[HttpWrapper]

  override protected def httpPostAndForget(uri: String, body: JsValue, headers: Map[String, String] = Map.empty) = {
    httpWrapper.post(uri, body, headers)
  }

  override protected def httpGet[A](uri: String)(implicit m: Manifest[A]): Option[A] = {
    httpWrapper.get(uri)
  }

  override protected def httpPostSynchronous(uri: String, body: JsValue, headers: Map[String, String] = Map.empty): Response = {
    httpWrapper.httpPostSynchronous(uri, body, headers)
  }

  class HttpWrapper {
    def get[T](uri: String): Option[T] = None

    def post[T](uri: String, body: JsValue, headers: Map[String, String]): Option[T] = None

    def httpPostSynchronous(uri: String, body: JsValue, headers: Map[String, String]): Response = mock[Response]

    def httpDeleteAndForget(uri: String) {}

    def httpPut[A](uri: String, body: JsValue, headers: Map[String, String] = Map.empty): Option[A] = None
  }

}

class SaMicroServiceSpec extends WordSpec with MockitoSugar with ShouldMatchers with BeforeAndAfterEach {
  //

  lazy val preferenceMicroService: TestPreferencesMicroservice = new TestPreferencesMicroservice

  override def afterEach = reset(preferenceMicroService.httpWrapper)

  val utr = "2134567"

  val email = "someEmail@email.com"
  "SaMicroService" should {
    "save preferences for a user that wants email notifications" in new WithApplication(FakeApplication()) {

      preferenceMicroService.savePreferences(utr, true, Some(email))

      val bodyCaptor: ArgumentCaptor[JsValue] = ArgumentCaptor.forClass(manifest.runtimeClass.asInstanceOf[Class[JsValue]])
      verify(preferenceMicroService.httpWrapper).post(Matchers.eq(s"/preferences/sa/individual/$utr/print-suppression"), bodyCaptor.capture(), Matchers.any[Map[String, String]])

      val body = bodyCaptor.getValue
      (body \ "digital").as[JsBoolean].value shouldBe (true)
      (body \ "email").as[String] shouldBe email
    }

    "save preferences for a user that wants paper notifications" in new WithApplication(FakeApplication()) {

      preferenceMicroService.savePreferences(utr, false)

      val bodyCaptor: ArgumentCaptor[JsValue] = ArgumentCaptor.forClass(manifest.runtimeClass.asInstanceOf[Class[JsValue]])
      verify(preferenceMicroService.httpWrapper).post(Matchers.eq(s"/preferences/sa/individual/$utr/print-suppression"), bodyCaptor.capture(), Matchers.any[Map[String, String]])

      val body = bodyCaptor.getValue
      (body \ "digital").as[JsBoolean].value shouldBe (false)
      (body \ "email").asOpt[String] shouldBe (None)

    }

    "get preferences for a user who opted for email notification" in new WithApplication(FakeApplication()) {

      when(preferenceMicroService.httpWrapper.get[SaPreference](s"/preferences/sa/individual/$utr/print-suppression")).thenReturn(Some(SaPreference(true, Some("someEmail@email.com"))))
      val result = preferenceMicroService.getPreferences(utr).get
      verify(preferenceMicroService.httpWrapper).get[SaPreference](s"/preferences/sa/individual/$utr/print-suppression")

      result.digital shouldBe (true)
      result.email shouldBe (Some("someEmail@email.com"))
    }

    "get preferences for a user who opted for paper notification" in new WithApplication(FakeApplication()) {

      when(preferenceMicroService.httpWrapper.get[SaPreference](s"/preferences/sa/individual/$utr/print-suppression")).thenReturn(Some(SaPreference(false)))
      val result = preferenceMicroService.getPreferences(utr).get
      verify(preferenceMicroService.httpWrapper).get[SaPreference](s"/preferences/sa/individual/$utr/print-suppression")

      result.digital shouldBe (false)
      result.email shouldBe (None)
    }

    "return none for a user who has not set preferences" in new WithApplication(FakeApplication()) {
      val mockPlayResponse = mock[Response]
      when(mockPlayResponse.status).thenReturn(404)
      when(preferenceMicroService.httpWrapper.get[SaPreference](s"/preferences/sa/individual/$utr/print-suppression")).thenThrow(new MicroServiceException("Not Found", mockPlayResponse))
      preferenceMicroService.getPreferences(utr) shouldBe (None)
      verify(mockPlayResponse).status
      verify(preferenceMicroService.httpWrapper).get[SaPreference](s"/preferences/sa/individual/$utr/print-suppression")
    }

    "return true if updateEmailValidationStatus returns 200" in {
      val token = "someGoodToken"
      val expected = ValidateEmail(token)
      val response = mock[Response]

      when(response.status).thenReturn(200)
      when(preferenceMicroService.httpWrapper.httpPostSynchronous(Matchers.eq("/preferences/sa/verifyEmailAndSuppressPrint"),
                                                                  Matchers.eq(Json.parse(toRequestBody(expected))),
                                                                  Matchers.any[Map[String, String]])).thenReturn(response)

      val result = preferenceMicroService.updateEmailValidationStatus(token)

      result shouldBe true
    }

    "return true if updateEmailValidationStatus returns 204" in {
      val token = "someGoodToken"
      val expected = ValidateEmail(token)
      val response = mock[Response]

      when(response.status).thenReturn(204)
      when(preferenceMicroService.httpWrapper.httpPostSynchronous(Matchers.eq("/preferences/sa/verifyEmailAndSuppressPrint"),
                                                                  Matchers.eq(Json.parse(toRequestBody(expected))),
                                                                  Matchers.any[Map[String, String]])).thenReturn(response)

      val result = preferenceMicroService.updateEmailValidationStatus(token)

      result shouldBe true
    }

    "return false if updateEmailValidationStatus returns 400" in {
      val token = "someGoodToken"
      val expected = ValidateEmail(token)
      val response = mock[Response]

      when(response.status).thenReturn(400)
      when(preferenceMicroService.httpWrapper.httpPostSynchronous(Matchers.eq("/preferences/sa/verifyEmailAndSuppressPrint"),
                                                                  Matchers.eq(Json.parse(toRequestBody(expected))),
                                                                  Matchers.any[Map[String, String]])).thenReturn(response)

      val result = preferenceMicroService.updateEmailValidationStatus(token)

      result shouldBe false
    }

    "return false if updateEmailValidationStatus returns 404" in {
      val token = "someGoodToken"
      val expected = ValidateEmail(token)
      val response = mock[Response]

      when(response.status).thenReturn(404)
      when(preferenceMicroService.httpWrapper.httpPostSynchronous(Matchers.eq("/preferences/sa/verifyEmailAndSuppressPrint"),
                                                                  Matchers.eq(Json.parse(toRequestBody(expected))),
                                                                  Matchers.any[Map[String, String]])).thenReturn(response)

      val result = preferenceMicroService.updateEmailValidationStatus(token)

      result shouldBe false
    }

    "return false if updateEmailValidationStatus returns 500" in {
      val token = "someGoodToken"
      val expected = ValidateEmail(token)
      val response = mock[Response]

      when(response.status).thenReturn(500)
      when(preferenceMicroService.httpWrapper.httpPostSynchronous(Matchers.eq("/preferences/sa/verifyEmailAndSuppressPrint"),
                                                                  Matchers.eq(Json.parse(toRequestBody(expected))),
                                                                  Matchers.any[Map[String, String]])).thenReturn(response)

      val result = preferenceMicroService.updateEmailValidationStatus(token)

      result shouldBe false
    }
  }
}
