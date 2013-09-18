package uk.gov.hmrc.common.microservice.audit

import uk.gov.hmrc.microservice.{ MicroServiceConfig, MicroService }
import play.api.libs.json.Json
import controllers.common.domain.Transform._
import play.api.Play
import play.api.Play.current

case class AuditEvent(auditSource: String,
  auditType: String,
  tags: Map[String, String] = Map.empty,
  detail: Map[String, String] = Map.empty)

class AuditMicroService(override val serviceUrl: String = MicroServiceConfig.auditServiceUrl) extends MicroService {

  lazy val enabled = Play.configuration.getBoolean(s"govuk-tax.${Play.mode}.services.datastream.enabled").getOrElse(false)

  def audit(auditEvent: AuditEvent) {
    if (enabled) httpPostAndForget("/write/audit", Json.parse(toRequestBody(auditEvent)))
  }
}
