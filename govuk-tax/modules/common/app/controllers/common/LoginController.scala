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
import uk.gov.hmrc.common.microservice.auth.{AuthTokenExchangeException, AuthConnector}
import controllers.common.actions.{HeaderCarrier, Actions}
import scala.concurrent.Future
import uk.gov.hmrc.microservice.saml.domain.AuthResponseValidationResult
import uk.gov.hmrc.common.microservice.auth.domain.Authority


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
  
  private def redirectToBizTax(response: GovernmentGatewayLoginResponse, authExchangeResponse: AuthExchangeResponse)(implicit request: Request[AnyContent]): SimpleResult = {
    val sessionId = s"session-${UUID.randomUUID}"
    auditConnector.audit(
      AuditEvent(
        auditType = "TxSucceeded",
        tags = Map("transactionName" -> "GG Login",
          HeaderNames.xSessionId -> sessionId) ++ hc.headers.toMap,
        detail = Map("authId" -> authExchangeResponse.authority.uri)
      )
    )
    FrontEndRedirect.toBusinessTax.withSession(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.userId -> authExchangeResponse.authority.uri, //TODO: Replace this with Bearer
      SessionKeys.authToken -> authExchangeResponse.authToken.toString,
      SessionKeys.name -> response.name,
      SessionKeys.token -> response.encodedGovernmentGatewayToken,
      SessionKeys.affinityGroup -> response.affinityGroup,
      SessionKeys.authProvider -> GovernmentGateway.id
    )
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
  
  private def processGovernmentGatewayLogin(credentials: Credentials, form: Form[Credentials])(implicit request: Request[AnyContent]): Future[SimpleResult] = {
    val result: Future[SimpleResult] = governmentGatewayConnector.login(credentials)(HeaderCarrier(request)).flatMap { ggResponse: GovernmentGatewayLoginResponse =>
      authConnector.exchangeCredIdForBearerToken(ggResponse.credId).map(authToken => redirectToBizTax(ggResponse, authToken))
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

  def notOnBusinessTaxWhitelistPage = views.html.whitelist_notice()

  def idaLogin = WithNewSessionTimeout(UnauthorisedAction.async {
    implicit request =>
      
      val form = responseForm.bindFromRequest()

      form.fold(
        erroredForm => Future.successful(handleIdaLoginFailure(reason = "SAML authentication response received without SAMLResponse data")),
        samlResponse => processIdaLogin(samlResponse)
      )
  })
  
  case class SAMLResponse(response: String)

  val responseForm = Form(
    mapping(
      "SAMLResponse" -> nonEmptyText
    )(SAMLResponse.apply)(SAMLResponse.unapply)
  )

  case class LoginSuccess(hashPid: String, authToken: AuthToken, updatedHC: HeaderCarrier)

  
  private def handleIdaLoginSuccess(hashPid: String, originalRequestId: Option[String])(implicit request: Request[AnyContent]): Future[SimpleResult] = {

    val updatedHC = hc.copy(requestId = originalRequestId, sessionId = Some(s"session-${UUID.randomUUID().toString}"))

    authConnector.exchangePidForBearerToken(hashPid)(updatedHC).map { authToken =>
      redirectToPaye(hashPid, authToken, updatedHC)
    } recover {
      case _ : AuthTokenExchangeException => handleIdaLoginFailure(Some(hashPid), "No record found in Auth for the PID", originalRequestId)
    }
  }
  
  private def processIdaLogin(samlResponse: SAMLResponse)(implicit request: Request[AnyContent]): Future[SimpleResult] = {
    
    samlConnector.validate(samlResponse.response).flatMap {
      case AuthResponseValidationResult(true, Some(hashPid), originalRequestId) => handleIdaLoginSuccess(hashPid, originalRequestId)
      case invalidResult => Future.successful(handleIdaLoginFailure(invalidResult.hashPid, "SAMLResponse failed validation", invalidResult.originalRequestId))
    }
  }

  private def auditInCaseOfSuccessfulLoginToIda(hashPid: String, updatedHC: HeaderCarrier, authority: Authority)(implicit request: Request[AnyContent]) = {
    auditConnector.audit(
      AuditEvent(
        auditType = "TxSucceeded",
        tags = Map("transactionName" -> "IDA Login", HeaderNames.xRequestId + "-Original" -> hc.requestId.getOrElse("")) ++ updatedHC.headers.toMap,
        detail = Map("hashPid" -> hashPid, "authId" -> authority.uri) ++ authority.accounts.toMap
      )
    )
  }
  
  private def redirectToPaye(hashPid: String, authExchangeResponse: AuthExchangeResponse, updatedHC: HeaderCarrier)(implicit request: Request[AnyContent]): SimpleResult = {
    auditInCaseOfSuccessfulLoginToIda(hashPid, updatedHC, authExchangeResponse.authority)
    Logger.info("IDA user signed in successfully.")
    FrontEndRedirect.forSession(session).withSession(
      SessionKeys.userId -> authExchangeResponse.authority.uri,
      SessionKeys.sessionId -> updatedHC.sessionId.get,
      SessionKeys.authProvider -> IdaWithTokenCheckForBeta.id,
      SessionKeys.authToken -> authExchangeResponse.authToken.toString
    )
  }
  
  private def handleIdaLoginFailure(hashPid: Option[String] = None, reason: String, originalRequestId: Option[String] = None)(implicit request: Request[AnyContent]): SimpleResult = {
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


  //  private def redirectToPaye(response: SAMLResponse, authToken: AuthToken)(implicit request: Request[AnyContent]): SimpleResult = {
  //    val sessionId = s"session-${UUID.randomUUID}"
  //    auditConnector.audit(
  //      AuditEvent(
  //        auditType = "TxSucceeded",
  //        tags = Map("transactionName" -> "GG Login",
  //          HeaderNames.xSessionId -> sessionId) ++ hc.headers.toMap,
  //          detail = Map("authId" -> response.authId)
  //      )
  //    )
  //    FrontEndRedirect.toBusinessTax.withSession(
  //      SessionKeys.authToken -> authToken.toString,
  //      SessionKeys.userId -> authority.uri,
  //      SessionKeys.sessionId -> updatedHC.sessionId.get,
  //      SessionKeys.authProvider -> IdaWithTokenCheckForBeta.id
  //    )
  //    LoginSuccess(hashPid, hcWithOriginalRequestIdAndNewSession)
  //  }
  
  

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

case class AuthToken(authToken: String) {
  override val toString = authToken
}

case class AuthExchangeResponse(authToken: AuthToken, authority: Authority)
