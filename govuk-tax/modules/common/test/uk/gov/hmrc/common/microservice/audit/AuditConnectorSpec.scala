package uk.gov.hmrc.common.microservice.audit

import org.scalatest.{ Matchers, WordSpec }
import play.api.libs.json.JsValue
import play.api.test.{ FakeApplication, WithApplication }
import controllers.common.actions.HeaderCarrier
import uk.gov.hmrc.common.BaseSpec
import org.scalautils.Tolerance
import scala.concurrent.Future

class TestAuditConnector extends AuditConnector {
  var body: AuditEvent = null
  var headers: Map[String, String] = null

  override protected def httpPostF[A, B](uri: String, body: A, headers: Map[String, String] = Map.empty)(implicit a: Manifest[A], b: Manifest[B], headerCarrier: HeaderCarrier): Future[Option[B]] = {
    this.body = body.asInstanceOf[AuditEvent]
    this.headers = headers
    Future.successful(None)
  }
}

class AuditConnectorSpec extends BaseSpec {

  "AuditConnector enabled" should {
    "call the audit service with an audit event" in new WithApplication(FakeApplication(additionalConfiguration = Map("govuk-tax.Test.services.datastream.enabled" -> true))) {
      val auditConnector = new TestAuditConnector()

      val auditEvent = AuditEvent("frontend", "request", Map("userId" -> "/auth/oid/099990"), Map("name" -> "Fred"))
      auditConnector.audit(auditEvent)

      auditConnector.headers should be(Map.empty)

      auditConnector.body shouldBe auditEvent
    }
  }

  "AuditConnector disabled" should {
    "call the audit service with an audit event" in new WithApplication(FakeApplication(additionalConfiguration = Map("govuk-tax.Test.services.datastream.enabled" -> false))) {

      val auditConnector = new TestAuditConnector()
      val auditEvent = AuditEvent("frontend", "request", Map("userId" -> "/auth/oid/099990"), Map("name" -> "Fred"))
      auditConnector.audit(auditEvent)

      auditConnector.body should be(null)
      auditConnector.headers should be(null)

    }
  }
}

