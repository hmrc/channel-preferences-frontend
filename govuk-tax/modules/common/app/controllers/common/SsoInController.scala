package controllers.common

import play.api.data.Forms._
import play.api.data._
import uk.gov.hmrc.common.microservice.governmentgateway.{GovernmentGatewayConnector, SsoLoginRequest}
import service.{ FrontEndConfig, SsoWhiteListService, Connectors }
import play.api.Logger
import play.api.libs.json.Json
import java.net.{ MalformedURLException, URISyntaxException, URI }
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import controllers.common.actions.{HeaderCarrier, Actions}
import play.api.mvc.Session

class SsoInController(ssoWhiteListService : SsoWhiteListService,
                      governmentGatewayConnector : GovernmentGatewayConnector,
                      override val auditConnector: AuditConnector)
                     (implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with SessionTimeoutWrapper
  with AllRegimeRoots {

  def this() = this(new SsoWhiteListService(FrontEndConfig.domainWhiteList), Connectors.governmentGatewayConnector, Connectors.auditConnector)(Connectors.authConnector)

  def in = WithNewSessionTimeout(UnauthorisedAction {
    implicit request =>
      val form = Form(single("payload" -> text))
      val (payload) = form.bindFromRequest.get
      val decryptedPayload = SsoPayloadEncryptor.decrypt(payload)
      Logger.debug(s"token: $payload")
      val json = Json.parse(decryptedPayload)
      val token = (json \ "gw").as[String]
      val time = (json \ "time").as[Long]
      val dest = (json \ "dest").asOpt[String]

      checkDestination(dest) match {
        case false => BadRequest
        case true => {
          val tokenRequest = SsoLoginRequest(token, time)
          try {
            val response = governmentGatewayConnector.ssoLogin(tokenRequest)(HeaderCarrier(request))
            Logger.debug(s"successfully authenticated: $response.name")
            Redirect(dest.get).withSession(Session(Map(
              SessionKeys.userId -> response.authId,
              SessionKeys.name -> response.name,
              SessionKeys.affinityGroup -> response.affinityGroup,
              SessionKeys.token -> response.encodedGovernmentGatewayToken.encodeBase64
            ).mapValues(encrypt)))
          } catch {
            case e: Exception => {
              Logger.info("Failed to validate a token.", e)
              Redirect(routes.HomeController.landing()).withNewSession
            }
          }
        }
      }
  })

  def out = WithNewSessionTimeout(UnauthorisedAction {
    implicit request =>
      Redirect(FrontEndConfig.portalLoggedOutUrl).withNewSession
  })

  private def checkDestination(dest: Option[String]): Boolean = {
    dest match {
      case Some(d) => {
        try {
          val url = URI.create(d).toURL
          ssoWhiteListService.check(url)
        } catch {
          case e: URISyntaxException => Logger.error(s"Requested destination URL is invalid: ${e.getMessage}"); false
          case e: MalformedURLException => Logger.error(s"Requested destination URL is invalid: ${e.getMessage}"); false
          case e: IllegalArgumentException => Logger.error(s"Requested destination URL is invalid: ${e.getMessage}"); false
        }
      }
      case None => Logger.error("Destination field is missing"); false
    }
  }
}
