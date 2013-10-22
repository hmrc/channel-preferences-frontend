package controllers.common.actions

import controllers.common.service.MicroServices
import org.slf4j.MDC
import play.api.Play
import play.api.Play.current
import play.api.mvc._
import uk.gov.hmrc.common.microservice.audit.{AuditMicroService, AuditEvent}
import concurrent.{Future, ExecutionContext}
import uk.gov.hmrc.common.microservice.domain.User
import controllers.common.HeaderNames
import util.Success

trait AuditActionWrapper extends MicroServices with HeaderNames {
  object WithRequestAuditing extends WithRequestAuditing(auditMicroService)

  def auditRequest(user: User, request: Request[AnyContent]) {
    if (auditMicroService.enabled) {
      val tags = new collection.mutable.HashMap[String, String]
      tags.put("requestId", MDC.get(requestId))
      tags.put("sessionId", MDC.get(xSessionId))
      tags.put("authId", user.userAuthority.id)
      user.userAuthority.nino.foreach(nino => tags.put("nino", nino.toString))
      user.userAuthority.ctUtr.foreach(utr => tags.put("ctUtr", utr.toString))
      user.userAuthority.saUtr.foreach(utr => tags.put("saUtr", utr.toString))
      user.userAuthority.vrn.foreach(vrn => tags.put("vatNo", vrn.toString))
      user.userAuthority.governmentGatewayCredential.foreach(ggwid => tags.put("governmentGatewayId", ggwid.toString))
      user.userAuthority.idaCredential.foreach(ida => tags.put("idaPid", ida.pids.mkString("[", ",", "]")))

      val details = new collection.mutable.HashMap[String, String]
      details.put("ipAddress", MDC.get(forwardedFor))
      details.put("url", request.uri)
      details.put("method", request.method.toUpperCase)
      details.put("userAgentString", request.headers.get("User-Agent").getOrElse("-"))
      details.put("referrer", request.headers.get("Referer").getOrElse("-"))

      val auditEvent = AuditEvent("frontend", "Request", tags.toMap, details.toMap)
      auditMicroService.audit(auditEvent)
    }
  }
}

class WithRequestAuditing(auditMicroService : AuditMicroService = MicroServices.auditMicroService) extends MdcHelper {

  import ExecutionContext.Implicits.global

  lazy val traceRequests = Play.configuration.getBoolean(s"govuk-tax.${Play.mode}.services.datastream.traceRequests").getOrElse(false)

  def apply(action: Action[AnyContent]) = Action.async {
    request =>
      if (traceRequests) {
        val context = fromMDC

        auditEvent("Request", context ++ Map("path" -> request.path))

        action(request).map(result => {
          auditEvent("Response", context ++ Map("statusCode" -> result.header.status.toString))
          result
        })
      } else {
        action(request)
      }
  }

  private def auditEvent(auditType: String, tags: Map[String, String]) {
    val auditEvent = AuditEvent("frontend", auditType, tags)
    auditMicroService.audit(auditEvent)
  }
}
