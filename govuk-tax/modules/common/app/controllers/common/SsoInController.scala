package controllers.common

import play.api.data.Forms._
import play.api.data._
import uk.gov.hmrc.common.microservice.governmentgateway.{GovernmentGatewayResponse, GovernmentGatewayConnector, SsoLoginRequest}
import service.{FrontEndConfig, SsoWhiteListService, Connectors}
import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.common.microservice.audit.AuditEvent
import java.net.{URLDecoder, MalformedURLException, URISyntaxException, URI}
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import controllers.common.actions.{HeaderCarrier, Actions}
import scala.concurrent.Future
import play.api.mvc.{Request, Session}
import uk.gov.hmrc.common.microservice.UnauthorizedException

class SsoInController(ssoWhiteListService: SsoWhiteListService,
                      governmentGatewayConnector: GovernmentGatewayConnector,
                      override val auditConnector: AuditConnector)
                     (implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with SessionTimeoutWrapper
  with AllRegimeRoots {

  def this() = this(new SsoWhiteListService(FrontEndConfig.domainWhiteList), Connectors.governmentGatewayConnector, Connectors.auditConnector)(Connectors.authConnector)

  case class LoginFailure(reason: String)

  def postIn = WithNewSessionTimeout(UnauthorisedAction.async {
    implicit request => {
      val form = Form(single("payload" -> text))
      val payload = form.bindFromRequest.get
      in(payload)
    }
  })

  def getIn(payload:String) = WithNewSessionTimeout(UnauthorisedAction.async {
    implicit request => {
      in(URLDecoder.decode(payload, "UTF-8"))
    }
  })

  def in (payload: String)(implicit request: Request[_]) = {
      val decryptedPayload = SsoPayloadEncryptor.decrypt(payload)
      Logger.debug(s"token: $payload")
      val json = Json.parse(decryptedPayload)
      val token = (json \ "gw").as[String]
      val time = (json \ "time").as[Long]
      val destination = (json \ "dest").asOpt[String]

      destination
        .filter(destinationIsAllowed)
        .map { d =>
          governmentGatewayConnector.ssoLogin(SsoLoginRequest(token, time))(HeaderCarrier(request))
            .recover {
              case _: IllegalStateException => LoginFailure("Invalid Token")
              case _: UnauthorizedException => LoginFailure("Unauthorized")
              case e: Exception => LoginFailure(s"Unknown - ${e.getMessage}")
            }
            .map {
              case response: GovernmentGatewayResponse => handleSuccessfulLogin(response, d)
              case LoginFailure(reason) => handleFailedLogin(reason, token)
            }
        }
        .getOrElse {
          Logger.error(s"Destination was invalid: $destination.")
          auditConnector.audit(
            AuditEvent(
              auditType = "TxFailed",
              tags = Map("transactionName" -> "SSO Login") ++ hc.headers.toMap,
              detail = Map("token" -> token, "transactionFailureReason" -> "Invalid destination")
            )
          )
          Future.successful(BadRequest)
        }
  }

  private def handleSuccessfulLogin(response: GovernmentGatewayResponse, destination: String)(implicit request: Request[_]) = {
    Logger.debug(s"successfully authenticated: ${response.name}")
    auditConnector.audit(
      AuditEvent(
        auditType = "TxSucceeded",
        tags = Map("transactionName" -> "SSO Login") ++ hc.headers.toMap,
        detail = Map("authId" -> response.authId, "name" -> response.name, "affinityGroup" -> response.affinityGroup)
      )
    )
    Redirect(destination).withSession(Session(Map(
      SessionKeys.userId -> response.authId,
      SessionKeys.name -> response.name,
      SessionKeys.affinityGroup -> response.affinityGroup,
      SessionKeys.token -> response.encodedGovernmentGatewayToken.encodeBase64
    ).mapValues(encrypt)))
  }

  private def handleFailedLogin(reason: String, token: String)(implicit request: Request[_]) = {
    Logger.info(s"Failed to validate, reason: $reason")
    auditConnector.audit(
      AuditEvent(
        auditType = "TxFailed",
        tags = Map("transactionName" -> "SSO Login") ++ hc.headers.toMap,
        detail = Map("token" -> token, "transactionFailureReason" -> reason)
      )
    )
    Redirect(routes.HomeController.landing()).withNewSession
  }

  def out = WithNewSessionTimeout(UnauthorisedAction {
    implicit request =>
      Redirect(FrontEndConfig.portalLoggedOutUrl).withNewSession
  })

  private def destinationIsAllowed(dest: String): Boolean =
    try {
      val url = URI.create(dest).toURL
      ssoWhiteListService.check(url)
    } catch {
      case e @ (_: URISyntaxException | _: MalformedURLException | _: IllegalArgumentException) =>
        Logger.debug(s"Destination URL failed to validate", e)
        false
    }
}
