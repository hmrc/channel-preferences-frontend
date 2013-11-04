package controllers.common.actions

import controllers.common.service.Connectors
import play.api.Play
import play.api.Play.current
import play.api.mvc._
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import scala.concurrent.ExecutionContext
import controllers.common.HeaderNames
import uk.gov.hmrc.common.microservice.audit.AuditEvent
import util.Failure
import scala.Some
import uk.gov.hmrc.common.microservice.domain.User
import util.Success

trait AuditActionWrapper extends HeaderNames {
  val auditConnector : AuditConnector
  object WithRequestAuditing extends WithRequestAuditing(auditConnector)
}

class WithRequestAuditing(auditConnector : AuditConnector = Connectors.auditConnector) extends MdcHelper with HeaderNames {

  import ExecutionContext.Implicits.global

  lazy val traceRequests = Play.configuration.getBoolean(s"govuk-tax.${Play.mode}.services.datastream.traceRequests").getOrElse(false)

  def apply(user: User)(action: User => Action[AnyContent]) = applyAudited(user = Some(user), action = action(user))
  def apply(action: Action[AnyContent]) = applyAudited(user = None, action = action)
  
  private def applyAudited(user: Option[User], action: Action[AnyContent]) = Action.async {
    request =>
      if (traceRequests && auditConnector.enabled) {
        val context = fromMDC
        val eventCreator = auditEvent(user, request, context) _

        auditConnector.audit(eventCreator("Request", Map("path" -> request.path), extractFormData(request)))

        action(request).andThen({
          case Success(result) => auditConnector.audit(eventCreator("Response", Map("statusCode" -> result.header.status.toString), Map()))
          case Failure(exception) => // FIXME!!!
        })
      }
      else action(request)
  }

  private def auditEvent(userLoggedIn: Option[User], request: Request[AnyContent], mdcContext: Map[String, String])(auditType: String, extraTags: Map[String, String], extraDetails: Map[String, String]) = {
    val tags = new collection.mutable.HashMap[String, String]

    userLoggedIn foreach { user =>
      tags.put("authId", user.userAuthority.id)
      user.userAuthority.nino.foreach(nino => tags.put("nino", nino.toString))
      user.userAuthority.ctUtr.foreach(utr => tags.put("ctUtr", utr.toString))
      user.userAuthority.saUtr.foreach(utr => tags.put("saUtr", utr.toString))
      user.userAuthority.vrn.foreach(vrn => tags.put("vatNo", vrn.toString))
      user.userAuthority.governmentGatewayCredential.foreach(ggwid => tags.put("governmentGatewayId", ggwid.credentialId))
      user.userAuthority.idaCredential.foreach(ida => tags.put("idaPid", ida.pids.mkString("[", ",", "]")))
    }

    val details = new collection.mutable.HashMap[String, String]
    details.put("ipAddress", mdcContext.get(forwardedFor).getOrElse("-"))
    details.put("url", request.uri)
    details.put("method", request.method.toUpperCase)
    details.put("userAgentString", request.headers.get("User-Agent").getOrElse("-"))
    details.put("referrer", request.headers.get("Referer").getOrElse("-"))


    AuditEvent("frontend", auditType, tags.toMap ++ mdcContext ++ extraTags, details.toMap ++ extraDetails)
  }

  private def extractFormData(request: Request[AnyContent]) = {
    request.body match {
      case formData: AnyContentAsFormUrlEncoded =>
        Map("formData" -> formData.data.map(entry => {
          val (key, values) = entry
          s"${key}: " + (values match {
            case Seq() | Seq(null) | Seq("") => "<no values>"
            case values => s"{${values.mkString(", ")}}"
          })
        }).toSeq.sorted.mkString("[", ", ", "]"))
      case _ => Map[String,String]().empty
    }
  }
}
