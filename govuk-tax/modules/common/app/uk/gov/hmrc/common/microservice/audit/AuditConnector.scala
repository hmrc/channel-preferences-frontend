package uk.gov.hmrc.common.microservice.audit

import uk.gov.hmrc.common.microservice.{ MicroServiceConfig, Connector }
import play.api.Play
import play.api.Play.current
import org.joda.time.{DateTimeZone, DateTime}
import controllers.common.actions.HeaderCarrier
import controllers.common.service.RunMode

case class AuditEvent(auditSource: String = "frontend",
                      auditType: String,
                      tags: Map[String, String] = Map.empty,
                      detail: Map[String, String] = Map.empty,
                      generatedAt: DateTime = DateTime.now.withZone(DateTimeZone.UTC))

class AuditConnector(override val serviceUrl: String = MicroServiceConfig.auditServiceUrl) extends Connector with RunMode {

  lazy val enabled : Boolean = Play.configuration.getBoolean(s"govuk-tax.$env.services.datastream.enabled").getOrElse(false)

  def audit(auditEvent: AuditEvent)(implicit hc: HeaderCarrier) : Unit = if (enabled) httpPostF("/write/audit", Some(auditEvent))
}
