package uk.gov.hmrc.common.microservice.audit

import uk.gov.hmrc.microservice.{ MicroServiceConfig, Connector }
import play.api.Play
import play.api.Play.current
import org.joda.time.{DateTimeZone, DateTime}
import controllers.common.actions.HeaderCarrier

case class AuditEvent(auditSource: String = "frontend",
                      auditType: String,
                      tags: Map[String, String] = Map.empty,
                      detail: Map[String, String] = Map.empty,
                      generatedAt: DateTime = DateTime.now.withZone(DateTimeZone.UTC))

class AuditConnector(override val serviceUrl: String = MicroServiceConfig.auditServiceUrl) extends Connector {

  lazy val enabled : Boolean = Play.configuration.getBoolean(s"govuk-tax.${Play.mode}.services.datastream.enabled").getOrElse(false)

  def audit(auditEvent: AuditEvent)(implicit hc: HeaderCarrier) : Unit = if (enabled) httpPostF("/write/audit", Some(auditEvent))
}
