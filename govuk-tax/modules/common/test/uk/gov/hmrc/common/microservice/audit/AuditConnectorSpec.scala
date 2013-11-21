package uk.gov.hmrc.common.microservice.audit

import org.scalatest.{ Matchers, WordSpec }
import play.api.libs.json.JsValue
import play.api.test.{ FakeApplication, WithApplication }
import controllers.common.actions.HeaderCarrier
import uk.gov.hmrc.common.BaseSpec

class TestAuditConnector extends AuditConnector {
  var body: JsValue = null
  var headers: Map[String, String] = null

  override protected def httpPostAndForget(uri: String, body: JsValue, headers: Map[String, String] = Map.empty)(implicit hc: HeaderCarrier) {
    this.body = body
    this.headers = headers
  }
}

class AuditConnectorSpec extends BaseSpec {

  "AuditConnector enabled" should {
    "call the audit service with an audit event" in new WithApplication(FakeApplication(additionalConfiguration = Map("govuk-tax.Test.services.datastream.enabled" -> true))) {
      val auditConnector = new TestAuditConnector()

      val auditEvent = AuditEvent("frontend", "request", Map("userId" -> "/auth/oid/099990"), Map("name" -> "Fred"))
      auditConnector.audit(auditEvent)

      auditConnector.headers should be(Map.empty)

      val body = auditConnector.body
      (body \ "auditSource").as[String] should be("frontend")
      (body \ "auditType").as[String] should be("request")
      (body \ "tags" \ "userId").as[String] should be("/auth/oid/099990")
      (body \ "detail" \ "name").as[String] should be("Fred")
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

