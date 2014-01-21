package controllers.common

import scala.concurrent._
import scala._

import play.api.mvc._

import uk.gov.hmrc.common.microservice.saml.SamlConnector

import controllers.common.FrontEndRedirect._
import controllers.common.actions.HeaderCarrier
import uk.gov.hmrc.common.MdcLoggingExecutionContext
import MdcLoggingExecutionContext.fromLoggingDetails
import uk.gov.hmrc.common.microservice.domain.User
import play.api.mvc.SimpleResult
import play.api.Logger


sealed trait AuthenticationProvider {
  type FailureResult = SimpleResult
  val id: String

  def redirectToLogin(request: Request[AnyContent], redirectToOrigin: Boolean = false): Future[SimpleResult] = ???

  def handleNotAuthenticated(redirectToOrigin: Boolean)(implicit request: Request[AnyContent]): PartialFunction[UserCredentials, Future[Either[User, FailureResult]]] = ???

  implicit def hc(implicit request: Request[_]) = HeaderCarrier(request)
}

case class UserCredentials(userId: Option[String], token: Option[String])

object UserCredentials {

  import SessionKeys._

  def apply(session: Session): UserCredentials = UserCredentials(session.get(userId), session.get(token))
}

//object Ida extends AuthenticationProvider {
//  override val id = "IDA"
//
//   val login = routes.LoginController.samlLogin
//
//  def handleRedirect(implicit request: Request[AnyContent], redirectToOrigin: Boolean) =
//    Redirect(login).withSession(buildSessionForRedirect(request.session, redirectUrl))
//
//  private def redirectUrl(implicit request: Request[AnyContent], redirectToOrigin: Boolean) =
//    if (redirectToOrigin) Some(request.uri) else None
//
//  def handleNotAuthenticated(request: Request[AnyContent], redirectToOrigin: Boolean) = {
//    case UserCredentials(None, token@_) =>
//      Logger.info(s"No identity cookie found - redirecting to login. user: None token : $token")
//      Future.successful(Right(handleRedirect(request, redirectToOrigin)))
//    case UserCredentials(Some(userId), Some(token)) =>
//      Logger.info(s"Wrong user type - redirecting to login. user : $userId token : $token")
//      Future.successful(Right(handleRedirect(request, redirectToOrigin)))
//  }
//}


object IdaWithTokenCheckForBeta extends AuthenticationProvider {

  override val id = "IDA"

  lazy val samlConnector = new SamlConnector


  override def redirectToLogin(request: Request[AnyContent], redirectToOrigin: Boolean): Future[SimpleResult] = {
    implicit val hc = HeaderCarrier(request)
    request.getQueryString("token") match {
      case Some(token) => {
        samlConnector.validateToken(token).map { isValid =>
          if (isValid) toSamlLogin.withSession(buildSessionForRedirect(request.session, redirectUrl(request, true)))
          else toBadIdaToken
        }
      }
      case None => {
        if (samlConnector.idaTokenRequired) Future.successful(toBadIdaToken)
        else Future.successful(toSamlLogin.withSession(buildSessionForRedirect(request.session, redirectUrl(request, true))))
      }
    }
  }

  private def toBadIdaToken = {
    Logger.info("Redirecting to bad Ida token page")
    Redirect("/paye/company-car/ida-token-required-in-beta")
  }

  private def redirectUrl(request: Request[AnyContent], redirectToOrigin: Boolean) =
    if (redirectToOrigin) Some(request.uri) else None

  override def handleNotAuthenticated(redirectToOrigin: Boolean)(implicit request: Request[AnyContent]): PartialFunction[UserCredentials, Future[Either[User, SimpleResult]]] = {
    case UserCredentials(None, token@_) =>
      implicit val hc = HeaderCarrier(request)
      Logger.info(s"No identity cookie found - redirecting to login. user: None token : $token")
      redirectToLogin(request, redirectToOrigin).map(result => Right(result))
    case UserCredentials(Some(userId), Some(token)) =>
      implicit val hc = HeaderCarrier(request)
      Logger.info(s"Wrong user type - redirecting to login. user : $userId token : $token")
      redirectToLogin(request, redirectToOrigin).map(result => Right(result))
  }
}

object GovernmentGateway extends AuthenticationProvider {
  override val id = "GGW"

  val login = routes.LoginController.businessTaxLogin()

  override def redirectToLogin(request: Request[AnyContent], redirectToOrigin: Boolean) = Future.successful(Redirect(login))

  override def handleNotAuthenticated(redirectToOrigin: Boolean)(implicit request: Request[AnyContent]): PartialFunction[UserCredentials, Future[Either[User, FailureResult]]] = {
    case UserCredentials(None, token@_) =>
      Logger.info(s"No identity cookie found - redirecting to login. user: None token : $token")
      redirectToLogin(request, redirectToOrigin).map(Right(_))
    case UserCredentials(Some(userId), None) =>
      Logger.info(s"No gateway token - redirecting to login. user : $userId token : None")
      redirectToLogin(request, redirectToOrigin).map(Right(_))
  }
}

object AnyAuthenticationProvider extends AuthenticationProvider {

  private val login = routes.LoginController.businessTaxLogin()

  override def redirectToLogin(request: Request[AnyContent], redirectToOrigin: Boolean = false): Future[SimpleResult] = {
    Logger.info("In AnyAuthenticationProvider - redirecting to login page")
    request.session.get(SessionKeys.authProvider) match {
      case Some(IdaWithTokenCheckForBeta.id) => IdaWithTokenCheckForBeta.redirectToLogin(request, redirectToOrigin)
      case _ => Future.successful(Redirect(login))
    }
  }

  override def handleNotAuthenticated(redirectToOrigin: Boolean)(implicit request: Request[AnyContent]) = {
    request.session.get(SessionKeys.authProvider) match {
      case Some(GovernmentGateway.id) => GovernmentGateway.handleNotAuthenticated(redirectToOrigin)
      case Some(IdaWithTokenCheckForBeta.id) => IdaWithTokenCheckForBeta.handleNotAuthenticated(redirectToOrigin)
      case _ => {case _ => Future.successful(Right(Redirect(login).withNewSession))}
    }
  }

  override val id: String = "IDAorGGW"
}


