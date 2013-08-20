package uk.gov.hmrc.common.microservice.audit

import uk.gov.hmrc.microservice.{ MicroServiceConfig, MicroService }
import play.api.libs.json.Json
import controllers.common.domain.Transform._

case class AuditEvent(auditSource: String, auditType: String, tags: Map[String, String], detail: Map[String, String] = Map.empty)

class AuditMicroService(override val serviceUrl: String = MicroServiceConfig.auditServiceUrl) extends MicroService {

  def audit(auditEvent: AuditEvent) {
    httpPostAndForget("/write/audit", Json.parse(toRequestBody(auditEvent)))
  }
}
