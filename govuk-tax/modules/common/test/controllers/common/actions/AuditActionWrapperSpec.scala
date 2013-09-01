package controllers.common.actions

import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import play.api.mvc.{ AsyncResult, Action, Controller }
import uk.gov.hmrc.microservice.MockMicroServicesForTests
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.verify
import org.mockito.Mockito.never
import org.mockito.Mockito.times
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

  "AuditActionWrapper with traceRequestsEnabled " should {
    "audit the request and the response with values from the MDC" in new WithApplication(
      FakeApplication(additionalConfiguration = Map("govuk-tax.Test.services.audit.traceRequests" -> true))) {
      val auditEventCaptor = ArgumentCaptor.forClass(classOf[AuditEvent])

      MDC.put(authorisation, "/auth/oid/123123123")
      MDC.put(forwardedFor, "192.168.1.1")

      val controller = new AuditTestController()

      try {
        controller.test()(FakeRequest("GET", "/foo"))

        verify(controller.auditMicroService, times(2)).audit(auditEventCaptor.capture())

        val auditEvents = auditEventCaptor.getAllValues
        auditEvents.size must be(2)

        val requestAudit = auditEvents.get(0)

        requestAudit.auditSource must be("frontend")
        requestAudit.auditType must be("Request")

        requestAudit.tags.size must be(3)
        requestAudit.tags must contain(authorisation -> "/auth/oid/123123123")
        requestAudit.tags must contain(forwardedFor -> "192.168.1.1")
        requestAudit.tags must contain("path" -> "/foo")

        val responseAudit = auditEvents.get(1)

        responseAudit.auditSource must be("frontend")
        responseAudit.auditType must be("Response")

        responseAudit.tags.size must be(3)
        responseAudit.tags must contain(authorisation -> "/auth/oid/123123123")
        responseAudit.tags must contain(forwardedFor -> "192.168.1.1")
        responseAudit.tags must contain("statusCode" -> "200")
      } finally {
        MDC.clear
      }
    }

    "audit an async response with values from the MDC" in new WithApplication(
      FakeApplication(additionalConfiguration = Map("govuk-tax.Test.services.audit.traceRequests" -> true))) {
      val auditEventCaptor = ArgumentCaptor.forClass(classOf[AuditEvent])

      MDC.put(authorisation, "/auth/oid/34343434")
      MDC.put(forwardedFor, "192.168.1.2")

      val controller = new AuditTestController()

      try {
        val result = controller.asyncTest()(FakeRequest())
        result.isInstanceOf[AsyncResult] must be(true)

        Await.result(result.asInstanceOf[AsyncResult].result, Duration("3 seconds"))

        verify(controller.auditMicroService, times(2)).audit(auditEventCaptor.capture())

        val auditEvents = auditEventCaptor.getAllValues
        auditEvents.size must be(2)

        val responseAudit = auditEvents.get(1)

        responseAudit.auditSource must be("frontend")
        responseAudit.auditType must be("Response")

        responseAudit.tags.size must be(3)
        responseAudit.tags must contain(authorisation -> "/auth/oid/34343434")
        responseAudit.tags must contain(forwardedFor -> "192.168.1.2")
        responseAudit.tags must contain("statusCode" -> "200")
      } finally {
        MDC.clear
      }
    }
  }

  "AuditActionWrapper with traceRequests disabled " should {
    "not audit any events" in new WithApplication(FakeApplication()) {

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
