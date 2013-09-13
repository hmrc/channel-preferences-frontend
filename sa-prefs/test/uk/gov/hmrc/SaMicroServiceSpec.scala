package uk.gov.hmrc

import org.scalatest.mock.MockitoSugar
import org.scalatest.{ ShouldMatchers, WordSpec }
import play.api.libs.json.JsValue
import play.api.test.{ FakeApplication, WithApplication }
import org.mockito.Mockito._
import org.mockito.{ ArgumentCaptor, Captor, Matchers }
import play.api.libs.ws.WS.WSRequestHolder
import play.api.libs.ws.Response

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

class SaMicroServiceSpec extends WordSpec with MockitoSugar with ShouldMatchers {
  //
  lazy val saMicroService = new TestSaMicroservice
  val utr = "2134567"
  val email = "someEmail@email.com"

  "SaMicroService" should {
    "save preferences for a user that wants email notifications" in new WithApplication(FakeApplication()) {
      //
      //      saMicroService.savePreferences(utr, true, Some(email))
      //
      //      val bodyCaptor: ArgumentCaptor[JsValue] = ArgumentCaptor.forClass(manifest.runtimeClass.asInstanceOf[Class[JsValue]])
      //      verify(saMicroService.httpWrapper).httpPut(Matchers.eq(utr), bodyCaptor.capture())
      //
      //      val body = bodyCaptor.getValue
      pending
    }

  }

}

