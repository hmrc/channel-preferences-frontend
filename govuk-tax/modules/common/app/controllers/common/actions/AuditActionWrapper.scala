package controllers.common.actions

import controllers.common.service.{RunMode, Connectors}
import play.api.{Logger, Play}
import play.api.Play.current
import play.api.mvc._
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import controllers.common.{HeaderNames, CookieNames}
import uk.gov.hmrc.common.microservice.audit.AuditEvent
import scala.util.{Try, Failure}
import scala.Some
import uk.gov.hmrc.common.microservice.domain.User
import com.ning.http.util.Base64
import uk.gov.hmrc.common.MdcLoggingExecutionContext

trait AuditActionWrapper {
  val auditConnector : AuditConnector
  object WithRequestAuditing extends WithRequestAuditing(auditConnector)
}

class WithRequestAuditing(auditConnector : AuditConnector = Connectors.auditConnector) extends RunMode {

  import MdcLoggingExecutionContext.fromLoggingDetails

  lazy val traceRequests = Play.configuration.getBoolean(s"govuk-tax.$env.services.datastream.traceRequests").getOrElse(false)

  def apply(user: User)(action: User => Action[AnyContent]) = applyAudited(user = Some(user), action = action(user))
  def apply(action: Action[AnyContent]) = applyAudited(user = None, action = action)
  
  private def applyAudited(user: Option[User], action: Action[AnyContent]) = Action.async {
    request =>
      if (traceRequests && auditConnector.enabled) {
        implicit val hc = HeaderCarrier(request)
        val possibleFingerprint = deviceFingerprintFrom(request)
        val eventCreator = auditEvent(user, request, possibleFingerprint.flatMap(_.toOption), hc) _

        auditConnector.audit(eventCreator("Request", Map("path" -> request.path), extractFormData(request)))

        action(request).transform(
          result => {
            auditConnector.audit(eventCreator("Response", Map("statusCode" -> result.header.status.toString), Map()))
            possibleFingerprint match {
              case Some(Failure(_)) => result.discardingCookies(DiscardingCookie(CookieNames.deviceFingerprint))
              case _ => result
            }
          },
          throwable => throwable
        )
      }
      else action(request)
  }

  private def deviceFingerprintFrom(request: Request[AnyContent]): Option[Try[String]] =
    request.cookies.get(CookieNames.deviceFingerprint).map { cookie =>
      val decodeAttempt = Try {
        Base64.decode(cookie.value)
      }
      decodeAttempt.failed.foreach { Logger.info(s"Failed to decode device fingerprint: ${cookie.value}", _) }
      decodeAttempt.map { new String(_, "UTF-8") }
    }

  private def auditEvent(userLoggedIn: Option[User], request: Request[AnyContent], deviceFingerprint: Option[String], hc: HeaderCarrier)
                        (auditType: String, extraTags: Map[String, String], extraDetails: Map[String, String]) = {
    val tags = new collection.mutable.HashMap[String, String]

    userLoggedIn foreach { user =>
      tags.put("authId", user.userAuthority.uri)
      user.userAuthority.accounts.paye.foreach(paye => tags.put("nino", paye.nino.nino.toString))
      user.userAuthority.accounts.ct.foreach(ct => tags.put("ctUtr", ct.utr.utr.toString))
      user.userAuthority.accounts.sa.foreach(sa => tags.put("saUtr", sa.utr.utr.toString))
      user.userAuthority.accounts.vat.foreach(vat => tags.put("vatNo", vat.vrn.vrn.toString))
      user.userAuthority.credentials.gatewayId.foreach(ggwid => tags.put("governmentGatewayId", ggwid))
      if(!user.userAuthority.credentials.idaPids.isEmpty) tags.put("idaPid", user.userAuthority.credentials.idaPids.map(idaPid => idaPid.pid).mkString("[", ",", "]"))
    }

    val details = new collection.mutable.HashMap[String, String]
    details.put("ipAddress", hc.forwarded.getOrElse("-"))
    details.put("url", request.uri)
    details.put("method", request.method.toUpperCase)
    details.put("userAgentString", request.headers.get("User-Agent").getOrElse("-"))
    details.put("referrer", request.headers.get("Referer").getOrElse("-"))

    deviceFingerprint.foreach(f => details.put("deviceFingerprint", f))

    AuditEvent(auditType = auditType,
               tags = tags.toMap ++ hc.headers.toMap ++ extraTags,
               detail = details.toMap ++ extraDetails)
  }

  private def extractFormData(request: Request[AnyContent]) = {
    request.body match {
      case formData: AnyContentAsFormUrlEncoded =>
        Map("formData" -> formData.data.map(entry => {
          val (key, values) = entry
          s"$key: " + (values match {
            case Seq() | Seq(null) | Seq("") => "<no values>"
            case theValues => s"{${theValues.mkString(", ")}}"
          })
        }).toSeq.sorted.mkString("[", ", ", "]"))
      case _ => Map[String,String]().empty
    }
  }
}
