package controllers.common


import controllers.common.actions.{HeaderCarrier, Actions}
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.saml.SamlConnector
import controllers.common.service.Connectors
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import scala.concurrent.{ExecutionContext, Future}
import java.util.UUID
import uk.gov.hmrc.microservice.saml.domain._
import play.api.Logger
import uk.gov.hmrc.common.microservice.audit.AuditEvent
import uk.gov.hmrc.common.microservice.auth.domain.Authority
import scala.Some
import play.api.mvc.SimpleResult
import uk.gov.hmrc.common.microservice.auth.AuthTokenExchangeException
import  ExecutionContext.Implicits.global

class IdaLoginController(samlConnector: SamlConnector,
                         override val auditConnector: AuditConnector)
                        (implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with NoRegimeRoots {

  def this() = this(Connectors.samlConnector, Connectors.auditConnector)(Connectors.authConnector)

  def samlLogin(token: Option[String] = None) = WithNewSessionTimeout {
    import IdaWithTokenCheckForBeta._
    UnauthorisedAction.async {
      implicit request =>
        token match {
          case Some(token) => {
            validateToken(token).flatMap {
              isValid =>
                if (isValid) goToIdaLogin
                else {
                  Logger.info("The provided Ida token is not valid")
                  Future.successful(toBadIdaToken)
                }
            }
          }
          case None => {
            if (isIdaTokenRequired) Future.successful(IdaWithTokenCheckForBeta.toBadIdaToken)
            else goToIdaLogin
          }
        }
    }
  }

  private def goToIdaLogin(implicit request: Request[AnyContent]) = {
    samlConnector.create.map {
      authRequestFormData =>
        Ok(views.html.saml_auth_form(authRequestFormData.idaUrl, authRequestFormData.samlRequest))
    }
  }

  def idaLogin: Action[AnyContent] = WithNewSessionTimeout(UnauthorisedAction.async {
    implicit request =>
      validateAngGetSamlResponse.map(samlResponse => processIdaLogin(samlResponse))
        .getOrElse(Future.successful(handleIdaLoginFailure(reason = "SAML authentication response received without SAMLResponse data")))
  })

  private[controllers] def validateAngGetSamlResponse(implicit request: Request[AnyContent]): Option[SAMLResponse] = {
    responseForm.bindFromRequest.fold(
      errors => None,
      samlResponse => Some(samlResponse)
    )
  }


  private val responseForm = Form(
    mapping(
      "SAMLResponse" -> nonEmptyText
    )(SAMLResponse.apply)(SAMLResponse.unapply)
  )

  case class LoginSuccess(hashPid: String, authToken: AuthToken, updatedHC: HeaderCarrier)


  private def handleIdaLoginSuccess(hashPid: String, originalRequestId: Option[String])(implicit request: Request[AnyContent]): Future[SimpleResult] = {

    val updatedHC = hc.copy(requestId = originalRequestId, sessionId = Some(s"session-${UUID.randomUUID().toString}"))

    authConnector.exchangePidForBearerToken(hashPid)(updatedHC).map {
      authToken =>
        redirectToPaye(hashPid, authToken, updatedHC)
    } recover {
      case _: AuthTokenExchangeException => handleIdaLoginFailure(Some(hashPid), "No record found in Auth for the PID", originalRequestId)
    }
  }

  private[controllers] def processIdaLogin(samlResponse: SAMLResponse)(implicit request: Request[AnyContent]): Future[SimpleResult] = {
    samlConnector.validate(samlResponse.response).flatMap {
      authnResponseValidationResult =>
        (authnResponseValidationResult.idaResponse: IdaResponse, authnResponseValidationResult.hashPid, authnResponseValidationResult.originalRequestId) match {
          case (Match, Some(hashPid), originalRequestId) => handleIdaLoginSuccess(hashPid, originalRequestId)
          case (NoMatch, hashPid, originalRequestId) => Future.successful(handleIdaLoginFailure(hashPid, "SAMLResponse failed validation", originalRequestId))
          case (Cancel, _, originalRequestId) => Future.successful(Redirect(routes.LoginController.signedOut))
          case (Error, _, _) => {
            throw new RuntimeException("Got an error response from SAML while performing Ida login. Look at the SAML logs for more details")
          }
        }
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

}

case class SAMLResponse(response: String)

case class AuthExchangeResponse(authToken: AuthToken, authority: Authority)
