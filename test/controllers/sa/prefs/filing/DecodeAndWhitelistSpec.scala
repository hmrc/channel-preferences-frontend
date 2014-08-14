package controllers.sa.prefs.filing

import java.net.URLEncoder

import com.netaporter.uri.Uri
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.mvc.{Action, AnyContent, Results}
import play.api.test.Helpers._
import play.api.test.{FakeApplication, FakeRequest, WithApplication}

class DecodeAndWhitelistSpec extends WordSpec with ShouldMatchers with MockitoSugar with ScalaFutures {
  val allowedHost = "localhost"

  "The DecodeAndWhitelist wrapper" should {

    "pass through an allowed host" in new WithApplication(FakeApplication()) with TestCase {
      allow (s"http://$allowedHost:8080/portal")
    }

    "pass through a subdomain of an allowed host" in new WithApplication(FakeApplication()) with TestCase {
      allow (s"http://something.$allowedHost:8080/portal")
    }

    "reject a disallowed host" in new WithApplication(FakeApplication()) with TestCase {
      reject (s"http://monkey:8080/portal")
    }

    "reject a superdomain of an allowed host" in new WithApplication(FakeApplication()) with TestCase {
      reject (s"http://$allowedHost.something:8080/portal")
    }

    "reject a URL without a host" in new WithApplication(FakeApplication()) with TestCase {
      reject (s"/portal")
    }

    "reject a URL which has the wrong encoding" in new WithApplication(FakeApplication()) with TestCase {
      reject (s"http://$allowedHost:8080/portal", "UTF-16")
    }

    "reject an empty URL" in new WithApplication(FakeApplication()) with TestCase {
      reject ("")
    }
  }

  trait TestCase {
    val action = mock[Uri => Action[AnyContent]]
    when(action.apply(any())).thenReturn(Action(Results.Ok))

    def allow(url: String) {
      val response = DecodeAndWhitelist(URLEncoder.encode(url, "UTF-8"))(action)(Set(allowedHost))(FakeRequest())
      status(response) should be (200)
      verify(action).apply(Uri.parse(url))
    }

    def reject(url: String, encoding: String = "UTF-8") {
      val response = DecodeAndWhitelist(URLEncoder.encode(url, encoding))(action)(Set(allowedHost))(FakeRequest())
      status(response) should be (400)
      verify(action, never()).apply(any())
    }
  }
}