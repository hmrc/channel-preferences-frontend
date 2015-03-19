package authentication

import play.api.Logger
import play.api.mvc.{AnyContent, Request, Results}
import uk.gov.hmrc.play.frontend.auth.{UserCredentials, AuthenticationProvider}

import scala.concurrent.Future

object ValidSessionCredentialsProvider extends AuthenticationProvider with Results {
  def id: String = "ValidSessionCredentials"

  def redirectToLogin(redirectToOrigin: Boolean)(implicit request: Request[AnyContent]) =
    Future.successful(Unauthorized)

  def handleNotAuthenticated(redirectToOrigin: Boolean)(implicit request: Request[AnyContent]) = {
    case UserCredentials(None, token@_) =>
      Logger.info(s"No userId found - unauthorized. user: None token : $token")
      Future.successful(Right(Unauthorized))
    case UserCredentials(Some(userId), None) =>
      Logger.info(s"No token - unauthorized. user : $userId token : None")
      Future.successful(Right(Unauthorized))
  }
}
