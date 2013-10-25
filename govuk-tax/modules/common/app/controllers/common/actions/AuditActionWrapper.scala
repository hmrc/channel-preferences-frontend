package controllers.common.actions

import controllers.common.service.MicroServices
import org.slf4j.MDC
import play.api.Play
import play.api.Play.current
import play.api.mvc._
import uk.gov.hmrc.common.microservice.audit.{AuditMicroService, AuditEvent}
import concurrent.ExecutionContext
import uk.gov.hmrc.common.microservice.domain.User
import controllers.common.HeaderNames

trait AuditActionWrapper extends MicroServices with HeaderNames {
  object WithRequestAuditing extends WithRequestAuditing(auditMicroService)
}

class WithRequestAuditing(auditMicroService : AuditMicroService = MicroServices.auditMicroService) extends MdcHelper with HeaderNames {

  import ExecutionContext.Implicits.global

  lazy val traceRequests = Play.configuration.getBoolean(s"govuk-tax.${Play.mode}.services.datastream.traceRequests").getOrElse(false)

  def apply(user: Option[User])(action: Action[AnyContent]) = Action.async {
    request =>
      if (traceRequests) {
        val context = fromMDC
        auditEvent(user, request, "Request", context ++ Map("path" -> request.path), context.get(forwardedFor))
        action(request).map(result => {
          auditEvent(user, request, "Response", context ++ Map("statusCode" -> result.header.status.toString), context.get(forwardedFor))
          result
        })
      } else {
        action(request)
      }
  }

  private def auditEvent(userLoggedIn: Option[User], request: Request[AnyContent], auditType: String, extraTags: Map[String, String], ipAddress: Option[String]) {

   if (auditMicroService.enabled) {
        val tags = new collection.mutable.HashMap[String, String]

        userLoggedIn foreach {
              user =>
                tags.put("authId", user.userAuthority.id)
                user.userAuthority.nino.foreach(nino => tags.put("nino", nino.toString))
                user.userAuthority.ctUtr.foreach(utr => tags.put("ctUtr", utr.toString))
                user.userAuthority.saUtr.foreach(utr => tags.put("saUtr", utr.toString))
                user.userAuthority.vrn.foreach(vrn => tags.put("vatNo", vrn.toString))
                user.userAuthority.governmentGatewayCredential.foreach(ggwid => tags.put("governmentGatewayId", ggwid.credentialId))
                user.userAuthority.idaCredential.foreach(ida => tags.put("idaPid", ida.pids.mkString("[", ",", "]")))
           }

        val details = new collection.mutable.HashMap[String, String]
        details.put("ipAddress", ipAddress.getOrElse("-"))
        details.put("url", request.uri)
        details.put("method", request.method.toUpperCase)
        details.put("userAgentString", request.headers.get("User-Agent").getOrElse("-"))
        details.put("referrer", request.headers.get("Referer").getOrElse("-"))

        auditMicroService.audit(AuditEvent("frontend", auditType, tags.toMap ++ extraTags, details.toMap))
      }
   }
}
