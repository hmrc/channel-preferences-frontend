package controllers.common.actions

import controllers.common.service.MicroServices
import play.api.mvc.{ Action, AnyContent, Controller }
import uk.gov.hmrc.common.microservice.audit.AuditEvent
import org.slf4j.MDC
import play.api.Play
import play.api.Play.current

trait AuditActionWrapper extends MicroServices {
  self: Controller =>

  lazy val requestEnabled = Play.configuration.getBoolean(s"govuk-tax.${Play.mode}.services.audit.requestEnabled").getOrElse(false)

  object WithRequestAuditing {

    def fromMDC(): Map[String, String] = {
      import collection.JavaConversions._
      MDC.getCopyOfContextMap.toMap.asInstanceOf[Map[String, String]]
    }

    def apply(action: Action[AnyContent]) = Action {
      request =>
        if (requestEnabled) {
          val auditEvent = AuditEvent("frontend", "Request", fromMDC())
          auditMicroService.audit(auditEvent)
        }
        action(request)
    }
  }

}
