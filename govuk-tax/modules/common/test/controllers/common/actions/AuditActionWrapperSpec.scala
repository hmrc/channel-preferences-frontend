package controllers.common.actions

import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import play.api.mvc.{ Action, Controller }
import uk.gov.hmrc.microservice.MockMicroServicesForTests
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.verify
import uk.gov.hmrc.common.microservice.audit.AuditEvent
import play.api.test.{ FakeRequest, FakeApplication, WithApplication }
import org.slf4j.MDC
import controllers.common.HeaderNames

object AuditTestController extends Controller with AuditActionWrapper with MockMicroServicesForTests {

  def test() = WithRequestAuditing {
    Action {
      request =>
        Ok("")
    }
  }
}

class AuditActionWrapperSpec extends WordSpec with MustMatchers with HeaderNames {

  val auditEventCaptor = ArgumentCaptor.forClass(classOf[AuditEvent])

  "AuditActionWrapper" should {
    "add values from the MDC to the audit event tags" in new WithApplication(FakeApplication()) {
      MDC.put(authorisation, "/auth/oid/123123123")
      MDC.put(forwardedFor, "192.168.1.1")

      try {
        AuditTestController.test()(FakeRequest())

        verify(AuditTestController.auditMicroService).audit(auditEventCaptor.capture())

        val auditEvent = auditEventCaptor.getValue
        auditEvent.auditSource must be("frontend")
        auditEvent.auditType must be("Request")

        auditEvent.tags.size must be(2)
        auditEvent.tags must contain(authorisation -> "/auth/oid/123123123")
        auditEvent.tags must contain(forwardedFor -> "192.168.1.1")
      } finally {
        MDC.clear
      }
    }
  }
}
