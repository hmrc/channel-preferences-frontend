package config

import uk.gov.hmrc.common.BaseSpec
import controllers.common.actions.HeaderCarrier
import play.api.test.{WithApplication, FakeHeaders}
import play.api.mvc.{Headers, RequestHeader}
import uk.gov.hmrc.common.microservice.audit.AuditEvent
import play.api.test.Helpers._
import uk.gov.hmrc.common.microservice.{ErrorTemplateDetails, ApplicationException}

class GlobalHelperSpec extends BaseSpec {

  private trait Setup {
    val headerCarrier = HeaderCarrier(requestId = Some("requestId"), sessionId = Some("sessionId"))
    val eventTags = Map("X-Request-ID" -> "requestId", "X-Session-ID" -> "sessionId",
      "transactionName" -> "some-transaction-name", "path" -> "some-path")


    val eventDetail = Map("input" -> "Request to some-path", "ipAddress" -> "-",
      "method" -> "GET", "userAgentString" -> "-", "referrer" -> "-")
  }

  "resolving an error" should {
    "return and audit event for a generic error along with a generic InternalServerError result" in new WithApplication with Setup {
      val exception = new RuntimeException("Runtime exception")

      val expectedAuditEvent = AuditEvent(
        auditType = "ServerInternalError",
        tags = eventTags - "transactionName" + ("transactionName" -> "Unexpected error"),
        detail = eventDetail + ("transactionFailureReason" -> exception.getMessage)
      )

      val (auditEvent, result) = GlobalHelper.resolveError(FakeRequestHeader, exception)(headerCarrier)

      auditEvent should have(
        'auditType(expectedAuditEvent.auditType),
        'tags(expectedAuditEvent.tags),
        'detail(expectedAuditEvent.detail)
      )

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return and audit event for a generic error along with a generic InternalServerError result if the exception cause is null" in new WithApplication with Setup {
      val exception = new RuntimeException("Runtime exception", null)

      val expectedAuditEvent = AuditEvent(
        auditType = "ServerInternalError",
        tags = eventTags - "transactionName" + ("transactionName" -> "Unexpected error"),
        detail = eventDetail + ("transactionFailureReason" -> exception.getMessage)
      )

      val (auditEvent, result) = GlobalHelper.resolveError(FakeRequestHeader, exception)(headerCarrier)

      auditEvent should have(
        'auditType(expectedAuditEvent.auditType),
        'tags(expectedAuditEvent.tags),
        'detail(expectedAuditEvent.detail)
      )

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return and audit event for an application error along with the InternalServerError result" in new WithApplication with Setup {
      val appException = new ApplicationException("paye", ErrorTemplateDetails("title", "heading", "message"), "application exception")
      val exception = new RuntimeException(appException)

      val expectedAuditEvent = AuditEvent(
        auditType = "ApplicationError",
        tags = eventTags - "transactionName" + ("transactionName" -> "Unexpected error in domain: paye"),
        detail = eventDetail + ("transactionFailureReason" -> exception.getMessage)
      )

      val (auditEvent, result) = GlobalHelper.resolveError(FakeRequestHeader, exception)(headerCarrier)

      auditEvent should have(
        'auditType(expectedAuditEvent.auditType),
        'tags(expectedAuditEvent.tags),
        'detail(expectedAuditEvent.detail)
      )

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "building audit event should" should {

    "return an audit event with no message" in new Setup {
      val expectedAuditEvent = AuditEvent(
        auditType = "some-event-type",
        tags = eventTags,
        detail = eventDetail
      )

      val actualAuditEvent = GlobalHelper.buildAuditEvent("some-event-type", "some-transaction-name", FakeRequestHeader, headerCarrier, None)

      actualAuditEvent should have(
        'auditType(expectedAuditEvent.auditType),
        'tags(expectedAuditEvent.tags),
        'detail(expectedAuditEvent.detail)
      )
    }

    "return an audit event with message" in new Setup {
      val expectedAuditEvent = AuditEvent(
        auditType = "some-event-type",
        tags = eventTags,
        detail = eventDetail + ("transactionFailureReason" -> "error-message")
      )

      val actualAuditEvent = GlobalHelper.buildAuditEvent("some-event-type", "some-transaction-name", FakeRequestHeader, headerCarrier, Some("error-message"))

      actualAuditEvent should have(
        'auditType(expectedAuditEvent.auditType),
        'tags(expectedAuditEvent.tags),
        'detail(expectedAuditEvent.detail)
      )
    }

  }
}

case object FakeRequestHeader extends RequestHeader {
  override def id: Long = 0L

  override def remoteAddress: String = ""

  override def headers: Headers = FakeHeaders()

  override def queryString: Map[String, Seq[String]] = Map.empty

  override def version: String = ""

  override def method: String = "GET"

  override def path: String = "some-path"

  override def uri: String = ""

  override def tags: Map[String, String] = Map.empty
}
