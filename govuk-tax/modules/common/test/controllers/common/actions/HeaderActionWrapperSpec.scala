package controllers.common.actions

import controllers.common.{SessionKeys, HeaderNames}
import play.api.mvc.{Action, Controller}
import play.api.test.{FakeApplication, WithApplication, FakeRequest}
import org.slf4j.MDC
import play.api.test.Helpers._
import uk.gov.hmrc.common.BaseSpec

object HeaderTestController extends Controller with MdcHeaders with RequestLogging {

  def test() =
    logRequest {
      storeHeaders {
        Action {
          request =>
            Ok(s"${MDC.get(MdcKeys.xSessionId)}:${MDC.get(MdcKeys.authorisation)}:${MDC.get(MdcKeys.token)}:${MDC.get(MdcKeys.forwardedFor)}:${MDC.get(MdcKeys.xRequestId)}")
          }
        }
    }

  def fail() =
    logRequest {
      storeHeaders {
        Action {
          request =>
            throw new Exception
        }
      }
    }
}

class HeaderActionWrapperSpec extends BaseSpec {

  "HeaderActionWrapper" should {
    "add parameters from the session and the headers to the MDC" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
        .withHeaders(HeaderNames.xForwardedFor -> "192.168.1.1")
        .withSession(SessionKeys.sessionId -> "012345", SessionKeys.userId -> "john", SessionKeys.token -> "12345")

      val result = HeaderTestController.test()(request)
      val fields = contentAsString(result) split ":"
      fields(0) shouldBe "012345"
      fields(1) shouldBe "john"
      fields(2) shouldBe "12345"
      fields(3) shouldBe "192.168.1.1"
      fields(4) should startWith("govuk-tax-")
      MDC.getCopyOfContextMap should be(null)
    }

    "return an internal server error " in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
        .withHeaders(HeaderNames.xForwardedFor -> "192.168.1.1")
        .withSession(SessionKeys.userId -> "john", SessionKeys.token -> "12345")

      val result = HeaderTestController.fail()(request)

      status(result) should be(INTERNAL_SERVER_ERROR)
      MDC.getCopyOfContextMap should be(null)
    }
  }
}