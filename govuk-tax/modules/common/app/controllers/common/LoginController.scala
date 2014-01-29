package controllers.common

import play.api.mvc.{Request, SimpleResult, AnyContent, Action}
import play.api.Logger
import play.api.data._
import play.api.data.Forms._
import uk.gov.hmrc.common.microservice.governmentgateway.{GovernmentGatewayLoginResponse, GovernmentGatewayConnector, Credentials}
import uk.gov.hmrc.common.microservice.{ForbiddenException, UnauthorizedException}
import controllers.common.service.{FrontEndConfig, Connectors}
import java.util.UUID
import uk.gov.hmrc.common.microservice.saml.SamlConnector
import uk.gov.hmrc.common.microservice.audit.{AuditEvent, AuditConnector}
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import controllers.common.actions.{HeaderCarrier, Actions}
import scala.concurrent.Future
import uk.gov.hmrc.microservice.saml.domain.AuthResponseValidationResult
import uk.gov.hmrc.common.microservice.auth.domain.Authority
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global


class LoginController(samlConnector: SamlConnector,
                      governmentGatewayConnector: GovernmentGatewayConnector,
                      override val auditConnector: AuditConnector)
                     (implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with AllRegimeRoots {

  def this() = this(Connectors.samlConnector, Connectors.governmentGatewayConnector, Connectors.auditConnector)(Connectors.authConnector)

  def samlLogin = WithNewSessionTimeout {
    UnauthorisedAction.async {
      implicit request =>
        samlConnector.create.map {
          authRequestFormData =>
            Ok(views.html.saml_auth_form(authRequestFormData.idaUrl, authRequestFormData.samlRequest))
        }
    }
  }

  def businessTaxLogin = WithNewSessionTimeout(UnauthorisedAction {
    implicit request =>
      Ok(views.html.ggw_login_form())
  })

  private def redirectToBizTax(response: GovernmentGatewayLoginResponse, authToken: BearerToken)(implicit request: Request[AnyContent]): SimpleResult = {
    val sessionId = s"session-${UUID.randomUUID}"
    auditConnector.audit(
      AuditEvent(
        auditType = "TxSucceeded",
        tags = Map("transactionName" -> "GG Login",
          HeaderNames.xSessionId -> sessionId) ++ hc.headers.toMap,
        detail = Map("authId" -> response.authId)
      )
    )
    FrontEndRedirect.toBusinessTax.withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.userId -> response.authId, //TODO: Replace this with Bearer
      SessionKeys.authToken -> authToken.toString,
      SessionKeys.name -> response.name,
      SessionKeys.token -> response.encodedGovernmentGatewayToken,
      SessionKeys.affinityGroup -> response.affinityGroup,
      SessionKeys.authProvider -> GovernmentGateway.id
    )
  }

  private def processGovernmentGatewayLogin(credentials: Credentials, form: Form[Credentials])(implicit request: Request[AnyContent]): Future[SimpleResult] = {
    val result: Future[SimpleResult] = governmentGatewayConnector.login(credentials)(HeaderCarrier(request)).flatMap { ggResponse: GovernmentGatewayLoginResponse =>
      authConnector.exchangeCredIdForBearerToken(ggResponse.credId).map(bearerToken => redirectToBizTax(ggResponse, bearerToken))
    }

    result.recover {
      case _: UnauthorizedException =>
        auditConnector.audit(
          AuditEvent(
            auditType = "TxFailed",
            tags = Map("transactionName" -> "GG Login") ++ hc.headers.toMap,
            detail = Map("transactionFailureReason" -> "Invalid Credentials")
          )
        )
        Unauthorized(views.html.ggw_login_form(form.withGlobalError("Invalid user ID or password. Try again.")))
      case _: ForbiddenException => {
        auditConnector.audit(
          AuditEvent(
            auditType = "TxFailed",
            tags = Map("transactionName" -> "GG Login") ++ hc.headers.toMap,
            detail = Map("transactionFailureReason" -> "Not on the whitelist")
          )
        )
        Forbidden(notOnBusinessTaxWhitelistPage)
      }
    }
  }

  def governmentGatewayLogin: Action[AnyContent] = WithNewSessionTimeout(UnauthorisedAction.async {
    implicit request =>

      val loginForm = Form(
        mapping(
          "userId" -> nonEmptyText,
          "password" -> nonEmptyText
        )(Credentials.apply)(Credentials.unapply)
      )
      val form = loginForm.bindFromRequest()

      form.fold(
        erroredForm => Future.successful(Ok(views.html.ggw_login_form(erroredForm))),
        credentials => processGovernmentGatewayLogin(credentials, form))
  })

  def notOnBusinessTaxWhitelistPage = views.html.whitelist_notice()

  case class SAMLResponse(response: String)

  val responseForm = Form(
    mapping(
      "SAMLResponse" -> nonEmptyText
    )(SAMLResponse.apply)(SAMLResponse.unapply)
  )

  case class LoginSuccess(hashPid: String, authority: Authority, updatedHC: HeaderCarrier)

  case class LoginFailure(reason: String, hashPid: Option[String] = None, originalRequestId: Option[String] = None)

  def idaLogin = WithNewSessionTimeout(UnauthorisedAction.async {
    implicit request =>
      responseForm.bindFromRequest.fold(
        errors => {
          Future.successful(LoginFailure("SAML authentication response received without SAMLResponse data"))
        },
        samlResponse => {
          samlConnector.validate(samlResponse.response).flatMap {
            case AuthResponseValidationResult(true, Some(hashPid), originalRequestId) =>
              val hcWithOriginalRequestIdAndNewSession = hc.copy(requestId = originalRequestId, sessionId = Some(s"session-${UUID.randomUUID().toString}"))

              authConnector.loginWithPid(hashPid)(hcWithOriginalRequestIdAndNewSession).map {
                case Some(authority) => LoginSuccess(hashPid, authority, hcWithOriginalRequestIdAndNewSession)
                case _ => LoginFailure(s"No record found in Auth for the PID", Some(hashPid), originalRequestId)
              }
            case invalidResult => Future.successful(LoginFailure("SAMLResponse failed validation", invalidResult.hashPid, invalidResult.originalRequestId))
          }
        }
      ).map {
        case LoginSuccess(hashPid, authority, updatedHC) =>
          auditConnector.audit(
            AuditEvent(
              auditType = "TxSucceeded",
              tags = Map("transactionName" -> "IDA Login", HeaderNames.xRequestId + "-Original" -> hc.requestId.getOrElse("")) ++ updatedHC.headers.toMap,
              detail = Map("hashPid" -> hashPid, "authId" -> authority.uri) ++ authority.accounts.toMap
            )
          )
          Logger.info("IDA user signed in successfully.")
          FrontEndRedirect.forSession(session).withSession(
            SessionKeys.userId -> authority.uri,
            SessionKeys.sessionId -> updatedHC.sessionId.get,
            SessionKeys.authProvider -> IdaWithTokenCheckForBeta.id
          )
        case LoginFailure(reason, hashPid, originalRequestId) =>
          Logger.warn(s"IDA sign in failed because '$reason'")
          auditConnector.audit(
            AuditEvent(
              auditType = "TxFailed",
              tags = Map("transactionName" -> "IDA Login", HeaderNames.xRequestId + "-Original" -> hc.requestId.getOrElse("")) ++ originalRequestId.map(HeaderNames.xRequestId -> _).toMap,
              detail = Map("transactionFailureReason" -> reason) ++ hashPid.map("hashPid" -> _).toMap
            )
          )
          Unauthorized(views.html.login_error())
      }
  })

  def logout = Action {
    request => request.session.get(SessionKeys.authProvider) match {
      case Some(IdaWithTokenCheckForBeta.id) => Redirect(routes.LoginController.payeSignedOut())
      case _ => Redirect(FrontEndConfig.portalSsoInLogoutUrl)
    }
  }

  def redirectToSignedOut = UnauthorisedAction {
    implicit request => Redirect(routes.LoginController.signedOut())
  }

  def payeSignedOut = signedOut

  def signedOut = WithNewSessionTimeout(UnauthorisedAction {
    implicit request =>
      Ok(views.html.logged_out()).withNewSession
  })
}

case class BearerToken(bearerToken: String) {
  override val toString = bearerToken
}
