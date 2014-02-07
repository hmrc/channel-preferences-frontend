package controllers.common

import play.api.data.Forms._
import play.api.data._
import uk.gov.hmrc.common.microservice.governmentgateway.{GovernmentGatewayLoginResponse, GovernmentGatewayConnector, SsoLoginRequest}
import service.{FrontEndConfig, SsoWhiteListService, Connectors}
import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.common.microservice.audit.AuditEvent
import java.net.{URLDecoder, MalformedURLException, URISyntaxException, URI}
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import controllers.common.actions.{HeaderCarrier, Actions}
import scala.concurrent.Future
import play.api.mvc.{SimpleResult, Request}
import uk.gov.hmrc.common.microservice.UnauthorizedException
import uk.gov.hmrc.common.crypto.ApplicationCrypto.SsoPayloadCrypto
import java.util.UUID

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

  val base64 = "^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$"
  def getIn(payload:String) = WithNewSessionTimeout(UnauthorisedAction.async {
    implicit request => {
      if (payload.matches(base64)) {
        in(payload)
      } else {
        in(URLDecoder.decode(payload, "UTF-8"))
      }
    }
  })

  def in (payload: String)(implicit request: Request[_]): Future[SimpleResult] = {
      val decryptedPayload = SsoPayloadCrypto.decrypt(payload)
      Logger.debug(s"token: $payload")
      val json = Json.parse(decryptedPayload)
      val token = (json \ "gw").as[String]
      val time = (json \ "time").as[Long]
      val destination = (json \ "dest").asOpt[String]

      destination
        .filter(destinationIsAllowed)
        .map { destination =>
          governmentGatewayConnector.ssoLogin(SsoLoginRequest(token, time))(HeaderCarrier(request))
            .recover {
              case _: IllegalStateException => LoginFailure("Invalid Token")
              case _: UnauthorizedException => LoginFailure("Unauthorized")
              case e: Exception => LoginFailure(s"Unknown - ${e.getMessage}")
            }
            .flatMap {
              case response: GovernmentGatewayLoginResponse => {
                authConnector.exchangeCredIdForBearerToken(response.credId).map(authToken => handleSuccessfulLogin(response, authToken, destination))
              }
              case LoginFailure(reason) => Future.successful(handleFailedLogin(reason, token))
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

  private def handleSuccessfulLogin(response: GovernmentGatewayLoginResponse, authExchangeResponse: AuthExchangeResponse, destination: String)(implicit request: Request[_]): SimpleResult = {
    Logger.debug(s"successfully authenticated: ${response.name}")
    auditConnector.audit(
      AuditEvent(
        auditType = "TxSucceeded",
        tags = Map("transactionName" -> "SSO Login") ++ hc.headers.toMap,
        detail = Map("authId" -> response.authId, "name" -> response.name, "affinityGroup" -> response.affinityGroup)
      )
    )
    Redirect(destination).withSession(
      SessionKeys.sessionId -> s"session-${UUID.randomUUID}",
      SessionKeys.userId -> authExchangeResponse.authority.uri, //TODO: Replace this with Bearer
      SessionKeys.authToken -> authExchangeResponse.authToken.toString,
      SessionKeys.name -> response.name,
      SessionKeys.token -> response.encodedGovernmentGatewayToken,
      SessionKeys.affinityGroup -> response.affinityGroup,
      SessionKeys.authProvider -> GovernmentGateway.id
    )
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
    Redirect(GovernmentGateway.login).withNewSession
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
