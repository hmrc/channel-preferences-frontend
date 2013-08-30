package controllers.common.actions

import controllers.common.service.MicroServices
import org.slf4j.MDC
import play.api.Play
import play.api.Play.current
import play.api.mvc._
import uk.gov.hmrc.common.microservice.audit.AuditEvent
import scala.concurrent.ExecutionContext

trait AuditActionWrapper extends MdcHelper {
  self: Controller with MicroServices =>

  import ExecutionContext.Implicits.global

  lazy val requestEnabled = Play.configuration.getBoolean(s"govuk-tax.${Play.mode}.services.audit.requestEnabled").getOrElse(false)

  lazy val responseEnabled = Play.configuration.getBoolean(s"govuk-tax.${Play.mode}.services.audit.responseEnabled").getOrElse(false)

  object WithRequestAuditing {

    def apply(action: Action[AnyContent]) = Action {
      request =>
        val context = fromMDC

        if (requestEnabled) {
          auditEvent("Request", context ++ Map("path" -> request.path))
        }

        def audit(result: PlainResult): Result = {
          if (responseEnabled) {
            auditEvent("Response", context ++ Map("statusCode" -> result.header.status.toString))
          }
          result
        }

        action(request) match {
          case plain: PlainResult => audit(plain)
          case async: AsyncResult => async.transform(audit)
        }
    }

    private def auditEvent(auditType: String, tags: Map[String, String]) {
      val auditEvent = AuditEvent("frontend", auditType, tags)
      auditMicroService.audit(auditEvent)
    }
  }

}
