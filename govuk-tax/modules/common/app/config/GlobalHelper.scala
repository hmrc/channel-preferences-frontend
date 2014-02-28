package config

import play.api.mvc.{Results, RequestHeader}
import controllers.common.actions.HeaderCarrier
import play.api.i18n.Messages
import play.api.mvc.Results._
import uk.gov.hmrc.common.microservice.audit.AuditEvent
import play.api.mvc.SimpleResult
import uk.gov.hmrc.common.microservice.ApplicationException

object GlobalHelper {

  def resolveError(request: RequestHeader, ex: Throwable)(implicit hc: HeaderCarrier): (AuditEvent, SimpleResult) = {
    val (event, result) = ex.getCause match {
      case ApplicationException(domain, errorTemplateDetails, _) => {
        (buildAuditEvent("ApplicationError", s"Unexpected error in domain: $domain", request, hc, Option(ex.getMessage)),
        InternalServerError(views.html.global_error(errorTemplateDetails.templateTitle, errorTemplateDetails.templateHeading, errorTemplateDetails.templateMeessage)))
      }
      case _ => {
        (buildAuditEvent("ServerInternalError", "Unexpected error", request, hc, Option(ex.getMessage)),
          Results.InternalServerError(views.html.global_error(Messages("global.error.InternalServerError500.title"),
          Messages("global.error.InternalServerError500.heading"),
          Messages("global.error.InternalServerError500.message"))))
      }
    }
    (event, result)
  }

  def buildAuditEvent(eventType: String, transactionName: String, request: RequestHeader, hc: HeaderCarrier, errorMessage: Option[String] = None) = {
    val (details, tags) = buildAuditData(request, hc, transactionName)
    val errorMap = errorMessage.map(message => Map("transactionFailureReason" -> message)).getOrElse(Map())
    AuditEvent(auditType = eventType,
      detail = details ++ errorMap,
      tags = tags)
  }

  private def buildAuditData(request: RequestHeader, hc: HeaderCarrier, transactionName: String) = {
    val details = Map[String, String](
      "input" -> s"Request to ${request.path}",
      "ipAddress" -> hc.forwarded.getOrElse("-"),
      "method" -> request.method.toUpperCase,
      "userAgentString" -> request.headers.get("User-Agent").getOrElse("-"),
      "referrer" -> request.headers.get("Referer").getOrElse("-"))

    val tags = Map[String, String](
      "X-Request-ID" -> hc.requestId.getOrElse("-"),
      "X-Session-ID" -> hc.sessionId.getOrElse("-"),
      "transactionName" -> transactionName,
      "path" -> request.path
    )

    (details, tags)
  }
}
