package uk.gov.hmrc.common.microservice.audit

import org.scalatest.{ Matchers, WordSpec }
import play.api.libs.json.JsValue
import play.api.test.{ FakeApplication, WithApplication }
import controllers.common.actions.HeaderCarrier
import uk.gov.hmrc.common.BaseSpec
import org.scalautils.Tolerance

class TestAuditConnector extends AuditConnector {
  var body: JsValue = null
  var headers: Map[String, String] = null

  override protected def httpPostAndForget(uri: String, body: JsValue, headers: Map[String, String] = Map.empty)(implicit hc: HeaderCarrier) {
    this.body = body
    this.headers = headers
  }
}

class AuditConnectorSpec extends BaseSpec {

  implicit val jsToStringEquality = new org.scalautils.Equality[JsValue] {
    def areEqual(a: JsValue, b: Any): Boolean = {
      b match {
        case s: String => a.as[String].equals(b)
        case _ => a.equals(b)
      }
    }
  }

  "AuditConnector enabled" should {
    "call the audit service with an audit event" in new WithApplication(FakeApplication(additionalConfiguration = Map("govuk-tax.Test.services.datastream.enabled" -> true))) {
      val auditConnector = new TestAuditConnector()

      val auditEvent = AuditEvent("frontend", "request", Map("userId" -> "/auth/oid/099990"), Map("name" -> "Fred"))
      auditConnector.audit(auditEvent)

      auditConnector.headers should be(Map.empty)

      val body = auditConnector.body
      body \ "auditSource" should equal ("frontend")
      body \ "auditType" should equal ("request")
      body \ "tags" \ "userId" should equal ("/auth/oid/099990")
      body \ "detail" \ "name" should equal ("Fred")
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

