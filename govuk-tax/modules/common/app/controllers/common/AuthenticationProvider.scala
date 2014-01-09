package controllers.common

import play.api.mvc.{Session, AnyContent, Request, SimpleResult}
import controllers.common.FrontEndRedirect._
import uk.gov.hmrc.common.microservice.domain.User
import play.api.Logger
import scala.concurrent._


trait AuthenticationProvider {
  type FailureResult = SimpleResult
  def handleNotAuthenticated(request: Request[AnyContent], redirectToOrigin: Boolean): PartialFunction[UserCredentials, Future[Either[User, FailureResult]]]
}

case class UserCredentials(userId: Option[String], token: Option[String])
object UserCredentials {
  import SessionKeys._
  def apply(session: Session): UserCredentials = UserCredentials(session.get(userId), session.get(token))
}

object Ida extends AuthenticationProvider {
  def handleRedirect(implicit request: Request[AnyContent], redirectToOrigin: Boolean) =
    toSamlLogin.withSession(buildSessionForRedirect(request.session, redirectUrl))

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
  def handleRedirect(request: Request[AnyContent]) = Redirect(routes.HomeController.landing())

  def handleNotAuthenticated(request: Request[AnyContent], redirectToOrigin: Boolean) = {
    case UserCredentials(None, token@_) =>
      Logger.info(s"No identity cookie found - redirecting to login. user: None token : $token")
      Future.successful(Right(handleRedirect(request)))
    case UserCredentials(Some(userId), None) =>
      Logger.info(s"No gateway token - redirecting to login. user : $userId token : None")
      Future.successful(Right(handleRedirect(request)))
  }
}


