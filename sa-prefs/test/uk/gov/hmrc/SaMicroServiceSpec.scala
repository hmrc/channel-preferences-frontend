package uk.gov.hmrc

import org.scalatest.mock.MockitoSugar
import org.scalatest.{ BeforeAndAfterEach, BeforeAndAfterAll, ShouldMatchers, WordSpec }
import play.api.libs.json.{ JsBoolean, JsValue }
import play.api.test.{ FakeApplication, WithApplication }
import org.mockito.Mockito._
import org.mockito.{ ArgumentCaptor, Captor, Matchers }
import play.api.libs.ws.WS.WSRequestHolder
import play.api.libs.ws.Response
import org.specs2.specification.{ AfterEach, BeforeAfterEach }

class TestSaMicroservice extends SaMicroService with MockitoSugar {

  val httpWrapper = mock[HttpWrapper]

  override protected def httpPutNoResponse(uri: String, body: JsValue, headers: Map[String, String] = Map.empty) = {
    httpWrapper.httpPut(uri, body)
  }

  override protected def httpGet[A](uri: String)(implicit m: Manifest[A]): Option[A] = {
    httpWrapper.get(uri)
  }

  class HttpWrapper {
    def get[T](uri: String): Option[T] = None

    def post[T](uri: String, body: JsValue, headers: Map[String, String]): Option[T] = None

    def httpDeleteAndForget(uri: String) {}

    def httpPut[A](uri: String, body: JsValue, headers: Map[String, String] = Map.empty): Option[A] = None
  }

}

class SaMicroServiceSpec extends WordSpec with MockitoSugar with ShouldMatchers with BeforeAndAfterEach {
  //

  lazy val saMicroService: TestSaMicroservice = new TestSaMicroservice

  override def afterEach = reset(saMicroService.httpWrapper)

  val utr = "2134567"

  val email = "someEmail@email.com"
  "SaMicroService" should {
    "save preferences for a user that wants email notifications" in new WithApplication(FakeApplication()) {
      saMicroService.savePreferences(utr, true, Some(email))

      val bodyCaptor: ArgumentCaptor[JsValue] = ArgumentCaptor.forClass(manifest.runtimeClass.asInstanceOf[Class[JsValue]])
      verify(saMicroService.httpWrapper).httpPut(Matchers.eq(s"/sa/utr/$utr/preferences"), bodyCaptor.capture(), Matchers.any[Map[String, String]])

      val body = bodyCaptor.getValue
      (body \ "digital").as[JsBoolean].value shouldBe (true)
      (body \ "email").as[String] shouldBe email
    }

    "save preferences for a user that wants paper notifications" in new WithApplication(FakeApplication()) {

      saMicroService.savePreferences(utr, false)

      val bodyCaptor: ArgumentCaptor[JsValue] = ArgumentCaptor.forClass(manifest.runtimeClass.asInstanceOf[Class[JsValue]])
      verify(saMicroService.httpWrapper).httpPut(Matchers.eq(s"/sa/utr/$utr/preferences"), bodyCaptor.capture(), Matchers.any[Map[String, String]])

      val body = bodyCaptor.getValue
      (body \ "digital").as[JsBoolean].value shouldBe (false)
      (body \ "email").asOpt[String] shouldBe (None)

    }

    "get preferences for a user who opted for email notification" in new WithApplication(FakeApplication()) {

      when(saMicroService.httpWrapper.get[SaPreference](s"/sa/utr/$utr/preferences")).thenReturn(Some(SaPreference(true, Some("someEmail@email.com"))))
      val result = saMicroService.getPreferences(utr).get
      verify(saMicroService.httpWrapper).get[SaPreference](s"/sa/utr/$utr/preferences")

      result.digital shouldBe (true)
      result.email shouldBe (Some("someEmail@email.com"))
    }

    "get preferences for a user who opted for paper notification" in new WithApplication(FakeApplication()) {

      when(saMicroService.httpWrapper.get[SaPreference](s"/sa/utr/$utr/preferences")).thenReturn(Some(SaPreference(false)))
      val result = saMicroService.getPreferences(utr).get
      verify(saMicroService.httpWrapper).get[SaPreference](s"/sa/utr/$utr/preferences")

      result.digital shouldBe (false)
      result.email shouldBe (None)
    }

    "return none for a user who has not set preferences" in new WithApplication(FakeApplication()) {
      val mockPlayResponse = mock[Response]
      when(mockPlayResponse.status).thenReturn(404)
      when(saMicroService.httpWrapper.get[SaPreference](s"/sa/utr/$utr/preferences")).thenThrow(new MicroServiceException("Not Found", mockPlayResponse))
      saMicroService.getPreferences(utr) shouldBe (None)
      verify(mockPlayResponse).status
      verify(saMicroService.httpWrapper).get[SaPreference](s"/sa/utr/$utr/preferences")
    }

  }

}

