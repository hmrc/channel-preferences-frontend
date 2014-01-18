package controllers.common

import scala.concurrent._
import scala._

import play.api.mvc._


import controllers.common.FrontEndRedirect._
import uk.gov.hmrc.common.microservice.domain.User
import scala.Some
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


object AnyAuthenticationProvider extends AuthenticationProvider{

  override val login = routes.LoginController.businessTaxLogin()

  def redirectToLogin(request: Request[AnyContent]) = {
    request.session.get(SessionKeys.authProvider) match {
      case Some(Ida.id) => Redirect(Ida.login)
      case _ => Redirect(login)
    }
  }

  def handleNotAuthenticated(request: Request[AnyContent], redirectToOrigin: Boolean) = {
    request.session.get(SessionKeys.authProvider) match {
      case Some(GovernmentGateway.id) => GovernmentGateway.handleNotAuthenticated(request, redirectToOrigin)
      case Some(Ida.id) => Ida.handleNotAuthenticated(request, redirectToOrigin)
      case _ => { case _ => Future.successful(Right(Redirect(login).withNewSession)) }
    }
  }

  override val id: String = "IDAorGGW"
}


