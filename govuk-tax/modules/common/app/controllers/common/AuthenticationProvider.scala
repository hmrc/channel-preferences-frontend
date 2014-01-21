package controllers.common

import scala.concurrent._
import scala._

import play.api.mvc._

import uk.gov.hmrc.common.microservice.saml.SamlConnector

import controllers.common.FrontEndRedirect._
import controllers.common.actions.HeaderCarrier
import uk.gov.hmrc.common.MdcLoggingExecutionContext
import uk.gov.hmrc.common.microservice.domain.User
import play.api.mvc.SimpleResult
import play.api.Logger


trait AuthenticationProvider {
  type FailureResult = SimpleResult
  val id: String
  val login: Call

  def handleNotAuthenticated(request: Request[AnyContent], redirectToOrigin: Boolean): PartialFunction[UserCredentials, Future[Either[User, FailureResult]]]
}

case class UserCredentials(userId: Option[String], token: Option[String])

object UserCredentials {

  import SessionKeys._

  def apply(session: Session): UserCredentials = UserCredentials(session.get(userId), session.get(token))
}

object Ida extends AuthenticationProvider {
  override val id = "IDA"

  override val login = routes.LoginController.samlLogin

  def handleRedirect(implicit request: Request[AnyContent], redirectToOrigin: Boolean) =
    Redirect(login).withSession(buildSessionForRedirect(request.session, redirectUrl))

  private def redirectUrl(implicit request: Request[AnyContent], redirectToOrigin: Boolean) =
    if (redirectToOrigin) Some(request.uri) else None

  def handleNotAuthenticated(request: Request[AnyContent], redirectToOrigin: Boolean) = {
    case UserCredentials(None, token@_) =>
      Logger.info(s"No identity cookie found - redirecting to login. user: None token : $token")
      Future.successful(Right(handleRedirect(request, redirectToOrigin)))
    case UserCredentials(Some(userId), Some(token)) =>
      Logger.info(s"Wrong user type - redirecting to login. user : $userId token : $token")
      Future.successful(Right(handleRedirect(request, redirectToOrigin)))
  }
}

object IdaWithTokenCheckForBeta extends AuthenticationProvider {

  override val id = "IDA"

  override val login = routes.LoginController.samlLogin

  lazy val samlConnector = new SamlConnector

  import MdcLoggingExecutionContext.fromLoggingDetails

  def toBadIdaToken = {
    Logger.info("Redirecting to bad Ida token page")
    Redirect("/paye/company-car/ida-token-required-in-beta")
  }

  def handleRedirect(implicit request: Request[AnyContent], redirectToOrigin: Boolean): Future[SimpleResult] = {
    implicit val hc = HeaderCarrier(request)
    request.getQueryString("token") match {
      case Some(token) => {
        Logger.debug(s"Validating IDA token $token")
        samlConnector.validateToken(token).map { isValid =>
          if (isValid) {
            Logger.info(s"Token $token was valid - redirecting to IDA login")
            toSamlLogin.withSession(buildSessionForRedirect(request.session, redirectUrl))
          }
          else {
            Logger.info(s"Token $token was invalid - redirecting to error page")
            toBadIdaToken
          }
        }
      }
      case None => {
        Logger.info("No token was provided - checking if required")
        if (samlConnector.idaTokenRequired) {
          Logger.info("Token is required - redirecting to error page")
          Future.successful(toBadIdaToken)
        }
        else {
          Logger.info("Token is not required - redirecting to IDA login")
          Future.successful(toSamlLogin.withSession(buildSessionForRedirect(request.session, redirectUrl)))
        }
      }
    }
  }

  private def redirectUrl(implicit request: Request[AnyContent], redirectToOrigin: Boolean) =
    if (redirectToOrigin) Some(request.uri) else None

  def handleNotAuthenticated(request: Request[AnyContent], redirectToOrigin: Boolean): PartialFunction[UserCredentials, Future[Either[User, SimpleResult]]] = {
    case UserCredentials(None, token@_) =>
      implicit val hc = HeaderCarrier(request)
      Logger.info(s"No identity cookie found - redirecting to login. user: None token : $token")
      handleRedirect(request, redirectToOrigin).map(result => Right(result))
    case UserCredentials(Some(userId), Some(token)) =>
      implicit val hc = HeaderCarrier(request)
      Logger.info(s"Wrong user type - redirecting to login. user : $userId token : $token")
      handleRedirect(request, redirectToOrigin).map(result => Right(result))
  }
}

object GovernmentGateway extends AuthenticationProvider {
  override val id = "GGW"

  override val login = routes.LoginController.businessTaxLogin()

  def handleRedirect(request: Request[AnyContent]) = Redirect(login)

  def handleNotAuthenticated(request: Request[AnyContent], redirectToOrigin: Boolean) = {
    case UserCredentials(None, token@_) =>
      Logger.info(s"No identity cookie found - redirecting to login. user: None token : $token")
      Future.successful(Right(handleRedirect(request)))
    case UserCredentials(Some(userId), None) =>
      Logger.info(s"No gateway token - redirecting to login. user : $userId token : None")
      Future.successful(Right(handleRedirect(request)))
  }
}


object AnyAuthenticationProvider extends AuthenticationProvider {

  override val login = routes.LoginController.businessTaxLogin()

  def redirectToLogin(request: Request[AnyContent]) : Future[SimpleResult] = {
    Logger.debug("In AnyAuthenticationProvider - redirecting to login page")
    request.session.get(SessionKeys.authProvider) match {
      case Some(IdaWithTokenCheckForBeta.id) => {
        Logger.debug("Provider is IDA - redirecting to IDA login page")
        Future.successful(Redirect(IdaWithTokenCheckForBeta.login))
      }
      case _ => Future.successful(Redirect(login))
    }
  }

  def handleNotAuthenticated(request: Request[AnyContent], redirectToOrigin: Boolean) = {
    request.session.get(SessionKeys.authProvider) match {
      case Some(GovernmentGateway.id) => GovernmentGateway.handleNotAuthenticated(request, redirectToOrigin)
      case Some(IdaWithTokenCheckForBeta.id) => IdaWithTokenCheckForBeta.handleNotAuthenticated(request, redirectToOrigin)
      case _ => {case _ => Future.successful(Right(Redirect(login).withNewSession))}
    }
  }

  override val id: String = "IDAorGGW"
}


