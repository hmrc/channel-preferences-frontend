package uk.gov.hmrc.common.microservice.audit

import uk.gov.hmrc.microservice.{ MicroServiceConfig, MicroService }
import play.api.libs.ws.WS

case class AuditEvent(auditType: String)

class AuditMicroService(override val serviceUrl: String = MicroServiceConfig.auditServiceUrl) extends MicroService {

  def audit(auditEvent: AuditEvent) {
    //WS.url(serviceUrl).post()
  }
}
