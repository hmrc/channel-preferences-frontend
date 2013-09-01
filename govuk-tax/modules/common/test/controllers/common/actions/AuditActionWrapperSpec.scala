package controllers.common.actions

import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import play.api.mvc.{ AsyncResult, Action, Controller }
import uk.gov.hmrc.microservice.MockMicroServicesForTests
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.verify
import org.mockito.Mockito.never
import org.mockito.Matchers.any
import uk.gov.hmrc.common.microservice.audit.AuditEvent
import play.api.test.{ FakeRequest, FakeApplication, WithApplication }
import org.slf4j.MDC
import controllers.common.HeaderNames
import scala.concurrent.{ Await, Future, ExecutionContext }
import scala.concurrent.duration.Duration

class AuditTestController extends Controller with AuditActionWrapper with MockMicroServicesForTests {

  def test() = WithRequestAuditing {
    Action {
      request =>
        Ok("")
    }
  }

  import ExecutionContext.Implicits.global

  def asyncTest() = WithRequestAuditing {
    Action {
      Async {
        Future(Ok("Hello"))
      }
    }
  }
}

class AuditActionWrapperSpec extends WordSpec with MustMatchers with HeaderNames {

  val auditEventCaptor = ArgumentCaptor.forClass(classOf[AuditEvent])

  "AuditActionWrapper enabled " should {
    "add values from the MDC to the audit event tags" in new WithApplication(FakeApplication(additionalConfiguration = Map("govuk-tax.Test.services.audit.requestEnabled" -> true))) {
      MDC.put(authorisation, "/auth/oid/123123123")
      MDC.put(forwardedFor, "192.168.1.1")

      val controller = new AuditTestController()

      try {
        controller.test()(FakeRequest("GET", "/foo"))

        verify(controller.auditMicroService).audit(auditEventCaptor.capture())

        val auditEvent = auditEventCaptor.getValue
        auditEvent.auditSource must be("frontend")
        auditEvent.auditType must be("Request")

        auditEvent.tags.size must be(3)
        auditEvent.tags must contain(authorisation -> "/auth/oid/123123123")
        auditEvent.tags must contain(forwardedFor -> "192.168.1.1")
        auditEvent.tags must contain("path" -> "/foo")
      } finally {
        MDC.clear
      }
    }

    "audit an async response with values from the MDC if enabled" in new WithApplication(FakeApplication(additionalConfiguration = Map("govuk-tax.Test.services.audit.responseEnabled" -> true))) {
      MDC.put(authorisation, "/auth/oid/123123123")
      MDC.put(forwardedFor, "192.168.1.1")

      val controller = new AuditTestController()

      try {
        val result = controller.asyncTest()(FakeRequest())
        result.isInstanceOf[AsyncResult] must be(true)

        Await.result(result.asInstanceOf[AsyncResult].result, Duration("3 seconds"))

        verify(controller.auditMicroService).audit(auditEventCaptor.capture())

        val auditEvent = auditEventCaptor.getValue
        auditEvent.auditSource must be("frontend")
        auditEvent.auditType must be("Response")

        auditEvent.tags.size must be(3)
        auditEvent.tags must contain(authorisation -> "/auth/oid/123123123")
        auditEvent.tags must contain(forwardedFor -> "192.168.1.1")
        auditEvent.tags must contain("statusCode" -> "200")
      } finally {
        MDC.clear
      }
    }
  }

  "AuditActionWrapper disabled " should {
    "not record the request in the audit log" in new WithApplication(FakeApplication()) {

      val controller = new AuditTestController()

      MDC.put(authorisation, "/auth/oid/123123123")
      MDC.put(forwardedFor, "192.168.1.1")

      try {
        controller.test()(FakeRequest())
        verify(controller.auditMicroService, never).audit(any(classOf[AuditEvent]))
      } finally {
        MDC.clear
      }
    }
  }

}
