package controllers.common

import play.api.mvc.{AnyContent, Request}
import controllers.common.FrontEndRedirect._
import uk.gov.hmrc.common.microservice.domain.User
import scala.Some
import play.api.mvc.SimpleResult
import play.api.Logger

trait AuthenticationProvider {
  def handleNotAuthenticated(request: Request[AnyContent], redirectToOrigin: Boolean): PartialFunction[(Option[String], Option[String]), Either[User, SimpleResult]]
}

object Ida extends AuthenticationProvider with CookieEncryption {
  def handleRedirect(implicit request: Request[AnyContent], redirectToOrigin: Boolean) =
    toSamlLogin.withSession(buildSessionForRedirect(request.session, redirectUrl))

  private def redirectUrl(implicit request: Request[AnyContent], redirectToOrigin: Boolean) =
    if (redirectToOrigin) Some(request.uri) else None

  def handleNotAuthenticated(request: Request[AnyContent], redirectToOrigin: Boolean) = {
    case (None, token@_) =>
      Logger.info(s"No identity cookie found - redirecting to login. user: None token : $token")
      Right(handleRedirect(request, redirectToOrigin))
    case (Some(encryptedUserId), Some(token)) =>
      Logger.info(s"Wrong user type - redirecting to login. user : ${decrypt(encryptedUserId)} token : $token")
      Right(handleRedirect(request, redirectToOrigin))
  }
}

object GovernmentGateway extends AuthenticationProvider with CookieEncryption {
  def handleRedirect(request: Request[AnyContent]) = Redirect(routes.HomeController.landing())

  def handleNotAuthenticated(request: Request[AnyContent], redirectToOrigin: Boolean) = {
    case (None, token@_) =>
      Logger.info(s"No identity cookie found - redirecting to login. user: None token : $token")
      Right(handleRedirect(request))
    case (Some(encryptedUserId), None) =>
      Logger.info(s"No gateway token - redirecting to login. user : ${decrypt(encryptedUserId)} token : None")
      Right(handleRedirect(request))
  }
}
