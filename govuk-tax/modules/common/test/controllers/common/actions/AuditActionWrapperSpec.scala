package controllers.common.actions

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
import uk.gov.hmrc.common.BaseSpec
import play.api.Play

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

class AuditActionWrapperSpec extends BaseSpec with HeaderNames {

  "AuditActionWrapper with traceRequestsEnabled " should {
    "audit the request and the response with values from the MDC" in new WithApplication(
      FakeApplication(additionalConfiguration = Map("govuk-tax.Test.services.audit.traceRequests" -> true))) {

      println("MODE:" + Play.current.mode)

      val auditEventCaptor = ArgumentCaptor.forClass(classOf[AuditEvent])

      MDC.put(authorisation, "/auth/oid/123123123")
      MDC.put(forwardedFor, "192.168.1.1")

      val controller = new AuditTestController()

      try {
        controller.test()(FakeRequest("GET", "/foo"))

        verify(controller.auditMicroService, times(2)).audit(auditEventCaptor.capture())

        val auditEvents = auditEventCaptor.getAllValues
        auditEvents.size should be(2)

        val requestAudit = auditEvents.get(0)

        requestAudit.auditSource should be("frontend")
        requestAudit.auditType should be("Request")

        requestAudit.tags.size should be(3)
        requestAudit.tags should contain(authorisation -> "/auth/oid/123123123")
        requestAudit.tags should contain(forwardedFor -> "192.168.1.1")
        requestAudit.tags should contain("path" -> "/foo")

        val responseAudit = auditEvents.get(1)

        responseAudit.auditSource should be("frontend")
        responseAudit.auditType should be("Response")

        responseAudit.tags.size should be(3)
        responseAudit.tags should contain(authorisation -> "/auth/oid/123123123")
        responseAudit.tags should contain(forwardedFor -> "192.168.1.1")
        responseAudit.tags should contain("statusCode" -> "200")
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
        result.isInstanceOf[AsyncResult] should be(true)

        Await.result(result.asInstanceOf[AsyncResult].result, Duration("3 seconds"))

        verify(controller.auditMicroService, times(2)).audit(auditEventCaptor.capture())

        val auditEvents = auditEventCaptor.getAllValues
        auditEvents.size should be(2)

        val responseAudit = auditEvents.get(1)

        responseAudit.auditSource should be("frontend")
        responseAudit.auditType should be("Response")

        responseAudit.tags.size should be(3)
        responseAudit.tags should contain(authorisation -> "/auth/oid/34343434")
        responseAudit.tags should contain(forwardedFor -> "192.168.1.2")
        responseAudit.tags should contain("statusCode" -> "200")
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
