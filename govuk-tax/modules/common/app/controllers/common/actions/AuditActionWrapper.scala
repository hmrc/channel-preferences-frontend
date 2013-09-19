package controllers.common.actions

import controllers.common.service.MicroServices
import org.slf4j.MDC
import play.api.Play
import play.api.Play.current
import play.api.mvc._
import uk.gov.hmrc.common.microservice.audit.{AuditMicroService, AuditEvent}
import scala.concurrent.ExecutionContext

trait AuditActionWrapper extends MicroServices {
  object WithRequestAuditing extends WithRequestAuditing(auditMicroService)
}

class WithRequestAuditing(auditMicroService : AuditMicroService = MicroServices.auditMicroService) extends MdcHelper {

  import ExecutionContext.Implicits.global

  lazy val traceRequests = Play.configuration.getBoolean(s"govuk-tax.${Play.mode}.services.datastream.traceRequests").getOrElse(false)

  def apply(action: Action[AnyContent]) = Action {
    request =>
      if (traceRequests) {
        val context = fromMDC

        def audit(result: PlainResult): Result = {
          auditEvent("Response", context ++ Map("statusCode" -> result.header.status.toString))
          result
        }

        auditEvent("Request", context ++ Map("path" -> request.path))

        action(request) match {
          case plain: PlainResult => audit(plain)
          case async: AsyncResult => async.transform(audit)
        }
      } else {
        action(request)
      }
  }

  private def auditEvent(auditType: String, tags: Map[String, String]) {
    val auditEvent = AuditEvent("frontend", auditType, tags)
    auditMicroService.audit(auditEvent)
  }
}
