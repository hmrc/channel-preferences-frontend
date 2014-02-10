package controllers.common

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import uk.gov.hmrc.common.microservice.governmentgateway.GovernmentGatewayConnector
import controllers.common.service.{FrontEndConfig, Connectors}
import java.util.UUID
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import controllers.common.actions.{HeaderCarrier, Actions}
import scala.concurrent.Future
import uk.gov.hmrc.common.microservice.UnauthorizedException
import uk.gov.hmrc.common.microservice.audit.AuditEvent
import uk.gov.hmrc.common.microservice.governmentgateway.GovernmentGatewayLoginResponse
import play.api.mvc.SimpleResult
import uk.gov.hmrc.common.microservice.governmentgateway.Credentials
import uk.gov.hmrc.common.microservice.ForbiddenException

class LoginController(governmentGatewayConnector: GovernmentGatewayConnector,
                      override val auditConnector: AuditConnector)
                     (implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with NoRegimeRoots {

  def this() = this(Connectors.governmentGatewayConnector, Connectors.auditConnector)(Connectors.authConnector)



  def redirectToAccountHome = UnauthorisedAction(request => FrontEndRedirect.toBusinessTax)

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
    val result: Future[SimpleResult] = governmentGatewayConnector.login(credentials)(HeaderCarrier(request)).flatMap {
      ggResponse: GovernmentGatewayLoginResponse =>
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
      case _ => Redirect(routes.LoginController.redirectToRemoteSsoLogout()).withNewSession
    }
  }

  def redirectToRemoteSsoLogout = UnauthorisedAction {
    implicit request => Redirect(FrontEndConfig.portalSsoInLogoutUrl)
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
