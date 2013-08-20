package controllers.common.actions

import controllers.common.service.MicroServices
import play.api.mvc.{ Action, AnyContent, Controller }
import uk.gov.hmrc.common.microservice.audit.AuditEvent
import org.slf4j.MDC

trait AuditActionWrapper extends MicroServices {
  self: Controller =>

  object WithRequestAuditing {

    def fromMDC(): Map[String, String] = {
      import collection.JavaConversions._
      MDC.getCopyOfContextMap.toMap.asInstanceOf[Map[String, String]]
    }

    def apply(action: Action[AnyContent]) = Action {
      request =>
        val auditEvent = AuditEvent("frontend", "Request", fromMDC())
        auditMicroService.audit(auditEvent)
        action(request)
    }
  }

}
