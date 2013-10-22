package controllers.common.actions

import play.api.mvc.{ Action, Controller }
import uk.gov.hmrc.common.microservice.MockMicroServicesForTests
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.verify
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Matchers.any
import uk.gov.hmrc.common.microservice.audit.AuditEvent
import play.api.test.{ FakeRequest, FakeApplication, WithApplication }
import org.slf4j.MDC
import controllers.common.HeaderNames
import scala.concurrent.{ Future, ExecutionContext }
import uk.gov.hmrc.common.BaseSpec
import org.scalatest.concurrent.ScalaFutures

class AuditTestController extends Controller with AuditActionWrapper with MockMicroServicesForTests {

  def test() = WithRequestAuditing {
    Action {
      request =>
        Ok("")
    }
  }

  import ExecutionContext.Implicits.global

  def asyncTest() = WithRequestAuditing {
    Action.async {
        Future(Ok("Hello"))
    }
  }
}

class AuditActionWrapperSpec extends BaseSpec with HeaderNames with ScalaFutures {

  "AuditActionWrapper with traceRequestsEnabled " should {
    "audit the request and the response with values from the MDC" in new WithApplication(
      FakeApplication(additionalConfiguration = Map("govuk-tax.Test.services.datastream.traceRequests" -> true))) {

      val auditEventCaptor = ArgumentCaptor.forClass(classOf[AuditEvent])

      MDC.put(authorisation, "/auth/oid/123123123")
      MDC.put(forwardedFor, "192.168.1.1")

      val controller = new AuditTestController()

      try {
        val response = controller.test()(FakeRequest("GET", "/foo"))

        whenReady(response) { result => 
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
        }
      } finally {
        MDC.clear
      }
    }

    "audit an async response with values from the MDC" in new WithApplication(
      FakeApplication(additionalConfiguration = Map("govuk-tax.Test.services.datastream.traceRequests" -> true))) {
      val auditEventCaptor = ArgumentCaptor.forClass(classOf[AuditEvent])

      MDC.put(authorisation, "/auth/oid/34343434")
      MDC.put(forwardedFor, "192.168.1.2")

      val controller = new AuditTestController()

      try {
        val response = controller.asyncTest()(FakeRequest())
        whenReady(response) { result =>
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
        }

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
