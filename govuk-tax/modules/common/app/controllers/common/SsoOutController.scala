package controllers.common

import controllers.common.service.Connectors
import play.api.libs.json._
import play.api.mvc.{AnyContent, Request, Action}
import config.PortalConfig
import play.api.Logger
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import controllers.common.actions.Actions
import uk.gov.hmrc.common.crypto.ApplicationCrypto.SsoPayloadCrypto

class SsoOutController(override val auditConnector: AuditConnector)
                      (implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with SessionTimeoutWrapper
  with AllRegimeRoots {

  def this() = this(Connectors.auditConnector)(Connectors.authConnector)

  def encryptPayload = WithSessionTimeoutValidation(Action {
    implicit request =>

      if (requestValid(request)) {
        val destinationUrl = retriveDestinationUrl
        val encodedGovernmentGatewayToken = request.session.get(SessionKeys.token).get
        val encryptedPayload = SsoPayloadCrypto.encrypt(generateJsonPayload(encodedGovernmentGatewayToken, destinationUrl))
        Ok(encryptedPayload)
      } else {
        BadRequest("Error")
      }
  })

  private def retriveDestinationUrl(implicit request: Request[AnyContent]): String = {
    request.queryString.get("destinationUrl") match {
      case Some(Seq(destination)) => destination
      case None => PortalConfig.getDestinationUrl("home")
    }
  }

  private def generateJsonPayload(token: String, dest: String) = {
    Json.stringify(Json.obj(("gw", token), ("dest", dest), ("time", now().getMillis)))
  }

  private def requestValid(request: Request[AnyContent]): Boolean = {

    def theDestinationIsInvalid = {
      request.queryString.get("destinationUrl") match {
        case Some(Seq(destination: String)) => destination match {
          case d if d.startsWith(PortalConfig.destinationRoot) => false
          case _ => {
            Logger.error(s"Host of Single Sign On destination URL $destination is not in the white list")
            true
          }
        }
        case None => false
        case Some(destinations: Seq[String]) => {
          Logger.error(s"Single Sign On was attempted with multilple destination URLs : ${destinations.mkString(", ")}")
          true
        }
      }
    }
    def theTokenIsMissing = {
      request.session.get(SessionKeys.token) match {
        case Some(_) => false
        case _ => {
          Logger.error("Single Sign On was attempted without a valid government gateway token")
          true
        }
      }
    }
    !theTokenIsMissing && !theDestinationIsInvalid
  }
}
