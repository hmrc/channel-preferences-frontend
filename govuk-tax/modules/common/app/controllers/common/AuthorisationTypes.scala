package controllers.common

import play.api.mvc._
import controllers.common.service._
import play.api.mvc.{SimpleResult, AnyContent, Request}
import uk.gov.hmrc.common.microservice.domain.User
import controllers.common.FrontEndRedirect._
import uk.gov.hmrc.common.microservice.domain.User
import scala.Some
import play.api.mvc.SimpleResult
import play.api.Logger

trait AuthorisationTypes extends Encryption {

  trait AuthorisationType {
    def handleNotAuthorised(request: Request[AnyContent], redirectToOrigin: Boolean): PartialFunction[(Option[String], Option[String]), Either[User, SimpleResult]]
  }

  object Ida extends AuthorisationType {
    def handleRedirect(request: Request[AnyContent], redirectToOrigin: Boolean) = {
      val redirectUrl = if (redirectToOrigin) Some(request.uri) else None
      toSamlLogin.withSession(buildSessionForRedirect(request.session, redirectUrl))
    }
    def handleNotAuthorised(request: Request[AnyContent], redirectToOrigin: Boolean) = {
      case (None, token @ _) =>
        Logger.info(s"No identity cookie found - redirecting to login. user: None token : ${token}")
        Right(handleRedirect(request, redirectToOrigin))
      case (Some(encryptedUserId), Some(token)) =>
        Logger.info(s"Wrong user type - redirecting to login. user : ${decrypt(encryptedUserId)} token : ${token}")
        Right(handleRedirect(request, redirectToOrigin))
    }
  }

  object GovernmentGateway extends AuthorisationType {
    def handleRedirect(request: Request[AnyContent]) = Redirect(routes.HomeController.landing())

    def handleNotAuthorised(request: Request[AnyContent], redirectToOrigin: Boolean) = {
      case (None, token @ _) =>
        Logger.info(s"No identity cookie found - redirecting to login. user: None token : ${token}")
        Right(handleRedirect(request))
      case (Some(encryptedUserId), None) =>
        Logger.info(s"No gateway token - redirecting to login. user : ${decrypt(encryptedUserId)} token : None")
        Right(handleRedirect(request))
    }
  }
}
