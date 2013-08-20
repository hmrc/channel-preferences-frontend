package controllers.common.actions

import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import controllers.common.{CookieEncryption, HeaderNames}
import play.api.mvc.{Action, Controller}
import uk.gov.hmrc.microservice.MockMicroServicesForTests
import play.api.test.{FakeApplication, WithApplication, FakeRequest}
import org.slf4j.MDC
import play.api.test.Helpers._


object HeaderTestController extends Controller with HeaderActionWrapper with MockMicroServicesForTests {

  def test() = WithHeaders {
    Action {
      request =>
        Ok(s"${MDC.get(authorisation)}:${MDC.get("token")}:${MDC.get(forwardedFor)}:${MDC.get(requestId)}")
    }
  }

  def fail() = WithHeaders {
    Action {
      request =>
        throw new Exception
    }
  }
}

class HeaderActionWrapperSpec extends WordSpec with MustMatchers with HeaderNames with CookieEncryption {

  "HeaderActionWrapper" should {
    "add parameters from the session and the headers to the MDC " in new WithApplication(FakeApplication()) {
      val headers = Seq((forwardedFor, "192.168.1.1"))
      val sessionParams = Seq(("userId",encrypt("john")),("token", "12345"))
      val request = FakeRequest().withHeaders(headers: _*).withSession(sessionParams: _*)

      val result = HeaderTestController.test()(request)
      val fields = contentAsString(result) split(":")

      fields(0) must be("john")
      fields(1) must be("12345")
      fields(2) must be("192.168.1.1")
      fields(3) must startWith("frontend-")
      MDC.getCopyOfContextMap must be(null)

    }

    "return an internal server error " in new WithApplication(FakeApplication()) {
      val headers = Seq((forwardedFor, "192.168.1.1"))
      val sessionParams = Seq(("userId",encrypt("john")),("token", "12345"))
      val request = FakeRequest().withHeaders(headers: _*).withSession(sessionParams: _*)

      val result = HeaderTestController.fail()(request)

      status(result) must be(INTERNAL_SERVER_ERROR)
      MDC.getCopyOfContextMap must be(null)

    }

  }

}
